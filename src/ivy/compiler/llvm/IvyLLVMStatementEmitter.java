package ivy.compiler.llvm;

import ivy.compiler.IvyAbstractSyntaxTreeTypes;
import ivy.compiler.IvySourceStream;
import ivy.compiler.IvyTokenStream;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public final class IvyLLVMStatementEmitter {

    private final IvyLLVMBackend backend;
    private final IvyLLVMExpressionEmitter expressionEmitter;
    private final IvyLLVMVariableManager variableManager;

    public IvyLLVMStatementEmitter(
        IvyLLVMBackend backend,
        IvyLLVMExpressionEmitter expressionEmitter,
        IvyLLVMVariableManager variableManager
    ) {
        this.backend = backend;
        this.expressionEmitter = expressionEmitter;
        this.variableManager = variableManager;
    }

    public MemorySegment emitStatement(
        int nodeId,
        int[] nodeTypes,
        int[] leftOrChild,
        int[] rightOrNext,
        int[] initNode,
        int[] tokenReferences,
        Arena arena,
        IvyTokenStream tokenStream,
        IvySourceStream sourceStream
    ) {
        if (nodeId == -1) {
            return null;
        }

        int type = nodeTypes[nodeId];

        switch (type) {

            case IvyAbstractSyntaxTreeTypes.BLOCK:
                return emitBlock(
                    nodeId, nodeTypes,
                    leftOrChild, rightOrNext,
                    initNode,
                    tokenReferences, arena,
                    tokenStream, sourceStream
                );

            case IvyAbstractSyntaxTreeTypes.RETURN_STMT:
                return emitReturn(
                    nodeId, nodeTypes,
                    leftOrChild, rightOrNext,
                    tokenReferences, arena,
                    tokenStream, sourceStream
                );
            case IvyAbstractSyntaxTreeTypes.VAR_DEC:
                return emitVariableDeclaration(
                    nodeId, nodeTypes,
                    leftOrChild, rightOrNext,
                    initNode,
                    tokenReferences, arena,
                    tokenStream, sourceStream
                );
            case IvyAbstractSyntaxTreeTypes.ASSIGN:
                return emitAssignment(
                    nodeId, nodeTypes,
                    leftOrChild, rightOrNext,
                    tokenReferences, arena,
                    tokenStream, sourceStream
                );
            default:
                return null;
        }
    }

    private MemorySegment emitBlock(
        int nodeId,
        int[] nodeTypes,
        int[] leftOrChild,
        int[] rightOrNext,
        int[] initNode,
        int[] tokenReferences,
        Arena arena,
        IvyTokenStream tokenStream,
        IvySourceStream sourceStream
    ) {
        MemorySegment lastResult = null;

        int current = leftOrChild[nodeId];

        while (current != -1) {

            MemorySegment result = emitStatement(
                current,
                nodeTypes,
                leftOrChild,
                rightOrNext,
                initNode,

                tokenReferences,
                arena,
                tokenStream,
                sourceStream
            );

            if (result != null) {
                lastResult = result;
            }

            current = rightOrNext[current];
        }

        return lastResult;
    }



    private MemorySegment emitVariableDeclaration(
        int nodeId, int[] nodeTypes, int[] leftOrChild, int[] rightOrNext,
        int[] initNode,
        int[] tokenReferences, Arena arena, IvyTokenStream tokenStream, IvySourceStream sourceStream
    ) {
        String name = sourceStream.getTokenText(tokenStream, tokenReferences[nodeId]);

        int initializerNode = initNode[nodeId];

        MemorySegment varValue = null;

        if (initializerNode != -1) {
            varValue = expressionEmitter.emitExpression(
                initializerNode, nodeTypes, leftOrChild,rightOrNext,
                tokenReferences, arena, tokenStream ,sourceStream
            );
        }
        // TODO: Pointers
        MemorySegment pointer = variableManager.declare(arena, name);

        if (varValue != null) {
            variableManager.store(arena, name, varValue);
        }

        return varValue;
    }

    public void clearVariables() {
        variableManager.clear();
    }

    private MemorySegment emitAssignment(
        int nodeId, int[] nodeTypes, int[] leftOrChild, int[] rightOrNext,
        int[] tokenReferences, Arena arena, IvyTokenStream tokenStream, IvySourceStream sourceStream
    ) {

        int targetNode = leftOrChild[nodeId];
        int valueNode = rightOrNext[nodeId];

        if (targetNode == -1|| valueNode  == -1) {
            return null;
        }

        String name = sourceStream.getTokenText(tokenStream, tokenReferences[targetNode]);

        MemorySegment varValue = expressionEmitter.emitExpression(
            valueNode, nodeTypes, leftOrChild, rightOrNext,
            tokenReferences, arena, tokenStream, sourceStream);

        if (varValue == null) {
            return null;
        }

        variableManager.store(
            arena, name, varValue
        );

        return varValue;
    }

    private MemorySegment emitReturn(
        int nodeId, int[] nodeTypes,
        int[] leftOrChild, int[] rightOrNext, int[] tokenReferences,
        Arena arena, IvyTokenStream tokenStream, IvySourceStream sourceStream
    ) {
        int expressionNode = leftOrChild[nodeId];

        if (expressionNode == -1) {
            backend.buildRetVoid();
            return null;
        }

        MemorySegment value =
            expressionEmitter.emitExpression(
                expressionNode, nodeTypes, leftOrChild, rightOrNext,
                tokenReferences, arena, tokenStream, sourceStream
            );

        if (value == null) {
            return null;
        }

        backend.buildRet(value);

        return value;
    }
}
