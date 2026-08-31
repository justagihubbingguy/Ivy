package ivy.compiler.llvm;

import ivy.compiler.IvySourceStream;
import ivy.compiler.IvyTokenStream;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class IvyLLVMFunctionCallEmitter {

    private final IvyLLVMBackend llvm;

    private final MemorySegment[] llvmFunctions;
    private final MemorySegment[] llvmFunctionTypes;
    private final int[] nodeResolvedFunctions;
    private final IvyLLVMExpressionEmitter expressionEmitter;

    public IvyLLVMFunctionCallEmitter(
        IvyLLVMBackend llvm,
        MemorySegment[] llvmFunctions,
        MemorySegment[] llvmFunctionTypes,
        int[] nodeResolvedFunctions, IvyLLVMExpressionEmitter expressionEmitter
    ) {
        this.llvm = llvm;
        this.llvmFunctions = llvmFunctions;
        this.llvmFunctionTypes = llvmFunctionTypes;
        this.nodeResolvedFunctions = nodeResolvedFunctions;
        this.expressionEmitter = expressionEmitter;
    }

    public MemorySegment emitFunctionCall(
        int nodeId, int[] nodeTypes,
        int[] leftOrChild, int[] rightOrNext,
        int[] functionReturnType, int[] functionParameters,
        int[] functionBody, int[] initNode,
        int[] tokenReferences, Arena arena,
        IvyTokenStream tokenStream, IvySourceStream sourceStream
    ) {
        String name = sourceStream.getTokenText(tokenStream, tokenReferences[nodeId]);
        int argHead = rightOrNext[nodeId];

        int functionIndex = nodeResolvedFunctions[nodeId];

        MemorySegment callee = llvmFunctions[functionIndex];

        MemorySegment functionType = llvmFunctionTypes[functionIndex];

        int args = 0;

        for (
            int current = argHead;
            current != -1;
            current = rightOrNext[current]
        ) {
            args++;
        }

        MemorySegment[] arguments = new MemorySegment[args];

        int index = 0;

        for (
            int current = argHead;
            current != -1;
            current = rightOrNext[current]
        ) {
            arguments[index++] =
                expressionEmitter.emitExpression(
                    current,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    tokenReferences,
                    arena,
                    tokenStream,
                    sourceStream
                );
        }

        return llvm.buildCall(arena, functionType, callee, arguments, name);
    }
}
