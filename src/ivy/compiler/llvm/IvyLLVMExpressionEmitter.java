package ivy.compiler.llvm;

import ivy.compiler.IvyAbstractSyntaxTreeTypes;
import ivy.compiler.IvySourceStream;
import ivy.compiler.IvyTokenStream;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class IvyLLVMExpressionEmitter {

    private final IvyLLVMBackend backend;
    private final IvyLLVMVariableManager varManager;
    private IvyLLVMFunctionCallEmitter functionCallEmitter;
    private IvyLLVMFunctionEmitter functionEmitter;

    public IvyLLVMExpressionEmitter(IvyLLVMBackend backend, IvyLLVMVariableManager varManager) {
        this.backend = backend;
        this.varManager = varManager;
    }

    public void setFunctionCallEmitter(IvyLLVMFunctionCallEmitter functionCallEmitter) {
        if (functionCallEmitter == null) {
            throw new IllegalArgumentException(
                "Function call emitter cannot be null!"
            );
        }

        this.functionCallEmitter = functionCallEmitter;
    }

    public void setFunctionEmitter(IvyLLVMFunctionEmitter functionEmitter) {
        if (functionEmitter == null) {
            throw new IllegalArgumentException("Function emitter cannot be null!");
        }
        this.functionEmitter = functionEmitter;
    }

    public MemorySegment emitExpression(int nodeId, int[] nodeTypes, int[] leftOrChild, int[] rightOrNext, int[] tokenReferences, Arena arena, IvyTokenStream tokenStream, IvySourceStream sourceStream) {

        if (nodeId == -1) {
            return null;
        }

        int type = nodeTypes[nodeId];

        switch (type) {

            case IvyAbstractSyntaxTreeTypes.LITERAL: {
                int val = parseIntegerConstant(
                    tokenReferences[nodeId],
                    tokenStream,
                    sourceStream
                );

                return backend.constI32(val);
            }
            case IvyAbstractSyntaxTreeTypes.IDENTIFIER:
                String name = sourceStream.getTokenText(
                    tokenStream,
                    tokenReferences[nodeId]
                );

                System.out.println("[IDENTIFIER] resolving: " + name);

                MemorySegment value =
                    functionEmitter.resolveParameter(name);

                if (value != null) {
                    System.out.println("[IDENTIFIER] " + name + " -> PARAMETER");
                    return value;
                }

                if (varManager.contains(name)) {
                    System.out.println("[IDENTIFIER] " + name + " -> LOCAL");
                    return varManager.load(arena, name);
                }

                System.out.println("[IDENTIFIER] " + name + " -> NOT FOUND");

                throw new IllegalStateException(
                    "Unknown identifier: " + name
                );
            case IvyAbstractSyntaxTreeTypes.ADD:
            case IvyAbstractSyntaxTreeTypes.SUBT:
            case IvyAbstractSyntaxTreeTypes.MULT:
            case IvyAbstractSyntaxTreeTypes.DIV: {

                int leftNode = leftOrChild[nodeId];
                int rightNode = rightOrNext[nodeId];

                MemorySegment left = emitExpression(
                    leftNode, nodeTypes, leftOrChild, rightOrNext,
                    tokenReferences, arena, tokenStream, sourceStream
                );

                MemorySegment right = emitExpression(
                    rightNode, nodeTypes, leftOrChild, rightOrNext,
                    tokenReferences, arena, tokenStream, sourceStream
                );

                if (left == null || right == null) {
                    return null;
                }

                return switch (type) {
                    case IvyAbstractSyntaxTreeTypes.ADD -> backend.buildAdd(
                        arena, left, right
                    );
                    case IvyAbstractSyntaxTreeTypes.SUBT -> backend.buildSub(
                        arena, left, right
                    );
                    case IvyAbstractSyntaxTreeTypes.MULT -> backend.buildMul(
                        arena, left, right
                    );
                    case IvyAbstractSyntaxTreeTypes.DIV -> backend.buildDiv(
                        arena, left, right
                    );
                    default -> null;
                };
            }
            case IvyAbstractSyntaxTreeTypes.CALL:

                return functionCallEmitter.emitFunctionCall(
                    nodeId,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    null,
                    null,
                    null,
                    null,
                    tokenReferences,
                    arena,
                    tokenStream,
                    sourceStream
                );

            default:
                return null;
        }
    }

    private int parseIntegerConstant(int tokenRef, IvyTokenStream stream, IvySourceStream source) {
        int line = stream.getLine(tokenRef);
        int col = stream.getColumn(tokenRef);

        char[] buf = source.getRawBuffer();

        int offset = 0;
        int currentLine = 1;

        while (
            offset < buf.length
                && currentLine < line
        ) {
            if (buf[offset] == '\n') {
                currentLine++;
            }

            offset++;
        }

        offset += col - 1;

        int val = 0;

        while (
            offset < buf.length
                && buf[offset] >= '0'
                && buf[offset] <= '9'
        ) {
            val = val * 10 + (buf[offset] - '0');
            offset++;
        }

        return val;
    }
}
