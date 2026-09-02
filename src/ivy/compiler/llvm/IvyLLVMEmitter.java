package ivy.compiler.llvm;

import ivy.compiler.IvyAbstractSyntaxTreeTypes;
import ivy.compiler.IvySourceStream;
import ivy.compiler.IvyTokenStream;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class IvyLLVMEmitter {

    private final IvyLLVMBackend backend = new IvyLLVMBackend();

    private MemorySegment[] llvmFunctions;
    private MemorySegment[] llvmFunctionTypes;
    private int[] nodeResolvedFunctions;

    private final IvyLLVMVariableManager variableManager = new IvyLLVMVariableManager(backend);

    private final IvyLLVMExpressionEmitter expressionEmitter = new IvyLLVMExpressionEmitter(backend, variableManager);

    private final IvyLLVMStatementEmitter statementEmitter = new IvyLLVMStatementEmitter(backend, expressionEmitter,variableManager);

    private IvyLLVMFunctionEmitter functionEmitter;
    private IvyLLVMFunctionCallEmitter functionCallEmitter;

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
        int[] nodeResolvedFunctions,
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

        functionCallEmitter = new IvyLLVMFunctionCallEmitter(
            backend,
            llvmFunctions,
            llvmFunctionTypes,
            nodeResolvedFunctions,
            expressionEmitter
        );

        expressionEmitter.setFunctionEmitter(functionEmitter);
        expressionEmitter.setFunctionCallEmitter(functionCallEmitter);

        int current = rootNodeId;

        while (current != -1) {
            emitNode(
                current,
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

            current = rightOrNext[current];
        }
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

            case IvyAbstractSyntaxTreeTypes.FUNC_DECL:

                return functionEmitter.emitFunctionDecl(
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
            case IvyAbstractSyntaxTreeTypes.CALL:
                return functionCallEmitter.emitFunctionCall(
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
