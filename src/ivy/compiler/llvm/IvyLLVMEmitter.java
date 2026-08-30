package ivy.compiler.llvm;

import ivy.compiler.IvyAbstractSyntaxTreeTypes;
import ivy.compiler.IvySourceStream;
import ivy.compiler.IvyTokenStream;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class IvyLLVMEmitter {

    private final IvyLLVMBackend backend = new IvyLLVMBackend();

    private final IvyLLVMVariableManager variableManager = new IvyLLVMVariableManager(backend);

    private final IvyLLVMExpressionEmitter expressionEmitter = new IvyLLVMExpressionEmitter(backend, variableManager);

    private final IvyLLVMStatementEmitter statementEmitter = new IvyLLVMStatementEmitter(backend, expressionEmitter,variableManager);

    private final IvyLLVMFunctionEmitter functionEmitter = new IvyLLVMFunctionEmitter(backend, statementEmitter);

    {
        expressionEmitter.setFunctionEmitter(functionEmitter);
    }

    public void init(Arena arena, String modName) {
        backend.init(arena, modName);
    }

    public void compileAndFinalize(
        int rootNodeId,
        int[] nodeTypes,
        int[] leftOrChild,
        int[] rightOrNext,
        int[] functionReturnType,
        int[] functionParameters,
        int[] functionBody,
        int[] initNode,
        int[] tokenReferences,
        Arena arena,
        IvyTokenStream tokenStream,
        IvySourceStream sourceStream
    ) {
        this.nodeResolvedFunctions = nodeResolvedFunctions;

        llvmFunctions = new MemorySegment[nodeTypes.length];
        llvmFunctionTypes = new MemorySegment[nodeTypes.length];

        functionEmitter = new IvyLLVMFunctionEmitter(
            backend,
            statementEmitter,
            llvmFunctions,
            llvmFunctionTypes
        );

        functionCallEmitter =
            new IvyLLVMFunctionCallEmitter(
                backend,
                llvmFunctions,
                llvmFunctionTypes,
                nodeResolvedFunctions,
                expressionEmitter
            );
        
        expressionEmitter.setFunctionEmitter(functionEmitter);

        System.out.println(
            "st dump (total nodes: "
                + nodeTypes.length
                + ")"
        );

        for (int i = 0; i < nodeTypes.length; i++) {
            System.out.println(
                "node [" + i + "] -> Type: "
                    + nodeTypes[i]
                    + ", Child/Left: "
                    + leftOrChild[i]
                    + ", Next/Right: "
                    + rightOrNext[i]
                    + " INIT"
            );
        }

        System.out.println(
            "passed rootNodeId: " + rootNodeId
        );

        MemorySegment finalVal = emitNode(
            rootNodeId, nodeTypes, leftOrChild, rightOrNext,
            functionReturnType, functionParameters, functionBody, initNode,
            tokenReferences, arena, tokenStream, sourceStream
        );

        if (finalVal == null) {
            for (int i = 0; i < nodeTypes.length; i++) {

                if (nodeTypes[i] ==
                    IvyAbstractSyntaxTreeTypes.MULT
                    || nodeTypes[i] ==
                    IvyAbstractSyntaxTreeTypes.BLOCK
                    || nodeTypes[i] ==
                    IvyAbstractSyntaxTreeTypes.FUNC_DECL) {

                    System.out.println(
                        "[fallback] attempting emission "
                            + "from active node index: " + i
                    );

                    finalVal = emitNode(
                        i,
                        nodeTypes,
                        leftOrChild,
                        rightOrNext,
                        functionReturnType,
                        functionParameters,
                        functionBody,
                        initNode,
                        tokenReferences,
                        arena,
                        tokenStream,
                        sourceStream
                    );

                    if (finalVal != null) {
                        break;
                    }
                }
            }
        }

//        if (finalVal == null) {
//            backend.buildRet(finalVal);
//        } else {
//            backend.buildRet(
//                backend.constI32(0)
//            );
//        }
    }

    public MemorySegment emitNode(

        int nodeId,
        int[] nodeTypes,
        int[] leftOrChild,
        int[] rightOrNext,

        int[] functionReturnType,
        int[] functionParameters,
        int[] functionBody,
        int[] initNode,
        int[] tokenReferences,
        Arena arena,
        IvyTokenStream tokenStream,
        IvySourceStream sourceStream

    ) {
        if (nodeId == -1) {
            return null;
        }

        System.out.println(
            "[debug] emit visiting node id: "
                + nodeId
                + " | gype: "
                + nodeTypes[nodeId]
                + " | child: "
                + leftOrChild[nodeId]
                + " | next: "
                + rightOrNext[nodeId]
        );

        int type = nodeTypes[nodeId];

        switch (type) {

            // Expressions
            case IvyAbstractSyntaxTreeTypes.LITERAL:
            case IvyAbstractSyntaxTreeTypes.ADD:
            case IvyAbstractSyntaxTreeTypes.SUBT:
            case IvyAbstractSyntaxTreeTypes.MULT:
            case IvyAbstractSyntaxTreeTypes.DIV:

                return expressionEmitter.emitExpression(
                    nodeId,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    tokenReferences,
                    arena,
                    tokenStream,
                    sourceStream
                );

            // Statements
            case IvyAbstractSyntaxTreeTypes.BLOCK:
            case IvyAbstractSyntaxTreeTypes.VAR_DEC:
            case IvyAbstractSyntaxTreeTypes.ASSIGN:
            case IvyAbstractSyntaxTreeTypes.RETURN_STMT:

                return statementEmitter.emitStatement(
                    nodeId,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    initNode,
                    tokenReferences,
                    arena,
                    tokenStream,
                    sourceStream
                );

            case IvyAbstractSyntaxTreeTypes.FUNC_DECL: {

                MemorySegment function =  functionEmitter.emitFunctionDecl(
                    nodeId,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    functionReturnType,
                    functionParameters,
                    functionBody,
                    initNode,
                    tokenReferences,
                    arena,
                    tokenStream,
                    sourceStream
                );
                int next = rightOrNext[nodeId];

                while (next != -1) {
                    emitNode(
                        next,
                        nodeTypes,
                        leftOrChild,
                        rightOrNext,
                        functionReturnType,
                        functionParameters,
                        functionBody,
                        initNode,
                        tokenReferences,
                        arena,
                        tokenStream,
                        sourceStream
                    );

                    next = rightOrNext[next];

                }
                return function;
            }

            case IvyAbstractSyntaxTreeTypes.UNKNOWN: {

                int child = leftOrChild[nodeId];

                if (child != -1) {
                    return emitNode(
                        child,
                        nodeTypes,
                        leftOrChild,
                        rightOrNext,
                        functionReturnType,
                        functionParameters,
                        functionBody,
                        initNode,
                        tokenReferences,
                        arena,
                        tokenStream,
                        sourceStream
                    );
                }

                int next = rightOrNext[nodeId];

                if (next != -1) {
                    return emitNode(
                        next,
                        nodeTypes,
                        leftOrChild,
                        rightOrNext,
                        functionReturnType,
                        functionParameters,
                        functionBody,
                        initNode,
                        tokenReferences,
                        arena,
                        tokenStream,
                        sourceStream
                    );
                }

                return null;
            }

            default:
                return null;
        }
    }

    public void dumpIntermediate() {
        backend.dumpModule();
    }

    public void dispose() {
        backend.dispose();
    }
}
