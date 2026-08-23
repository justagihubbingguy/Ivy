package ivy.compiler.llvm;

import ivy.compiler.IvyAbstractSyntaxTreeTypes;
import ivy.compiler.IvySourceStream;
import ivy.compiler.IvyTokenStream;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static org.llvm.c.LLVMCore.LLVMGetParam;

public class IvyLLVMFunctionEmitter {

    private final IvyLLVMBackend llvm;
    private final IvyLLVMStatementEmitter statementEmitter;
    private MemorySegment[] parameterValues;
    private String[] parameterNames;

    public IvyLLVMFunctionEmitter(IvyLLVMBackend llvm, IvyLLVMStatementEmitter statementEmitter) {
        this.llvm = llvm;
        this.statementEmitter = statementEmitter;
    }

    public MemorySegment emitFunctionDecl(
        int nodeId, int[] nodeTypes,
        int[] leftOrChild, int[] rightOrNext,
        int[] functionReturnType, int[] functionParameters,
        int[] functionBody, int[] initNode,
        int[] tokenReferences, Arena arena,
        IvyTokenStream tokenStream, IvySourceStream sourceStream
    ) {

            System.out.println(
                "[FUNCTION] node=" + nodeId
                    + " tokenRef=" + tokenReferences[nodeId]
                    + " name=" + sourceStream.getTokenText(
                    tokenStream,
                    tokenReferences[nodeId]
                )
            );

            String name = sourceStream.getTokenText(tokenStream, tokenReferences[nodeId]);

            int returnNode =
                functionReturnType[nodeId];
            int parameterNode =
                functionParameters[nodeId];
            int bodyNode =
                functionBody[nodeId];

            if (parameterNode != -1) {
                System.out.println(
                    "[PARAM] node=" + parameterNode +
                        " type=" + nodeTypes[parameterNode] +
                        " next=" + rightOrNext[parameterNode]
                );
            }


            MemorySegment[] parameterTypes =
                resolveParameterTypes(
                    parameterNode,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    tokenReferences,
                    tokenStream
                );

            System.out.println(
                "[FUNCTION] " + name +
                    " resolved " + parameterTypes.length +
                    " parameter type(s)"
            );

            int returnType = resolveAndValidateReturnType(returnNode, nodeTypes, tokenReferences, tokenStream);

            if (returnType != IvyAbstractSyntaxTreeTypes.UNKNOWN) {

            System.out.println(
                "[FUNCTION] node=" + nodeId +
                    " tokenRef=" + tokenReferences[nodeId] +
                    " name=" + name +
                    " column=" + tokenStream.getColumn(tokenReferences[nodeId]) +
                    " length=" + tokenStream.getLength(tokenReferences[nodeId])
            );

            int parameterCount = countParameters(parameterNode, rightOrNext);

            MemorySegment function = llvm.buildFunction(
                arena,
                name, llvm.buildFunctionType(
                    arena,
                    llvm.getLLVMType(returnType),
                    parameterTypes
                )
            );

            parameterValues = new MemorySegment[parameterCount];

            parameterNames = new String[parameterCount];

            int index = 0;
            int current = parameterNode;

            while (current != -1) {

                int tokenRef = tokenReferences[current];

                parameterNames[index] = sourceStream.getTokenText(tokenStream, tokenRef);

                parameterValues[index] = LLVMGetParam(function, index);

                index++;
                current = rightOrNext[current];
            }

            llvm.nameFunctionParameters(
                function,
                parameterNode,
                rightOrNext,
                tokenReferences,
                arena,
                tokenStream,
                sourceStream
            );

            llvm.createEntryBlock(arena, function);

            statementEmitter.emitStatement(
                bodyNode, nodeTypes, leftOrChild,
                rightOrNext, initNode, tokenReferences,
                arena, tokenStream, sourceStream
            );

            return function;
        }
        return null;
    }

    MemorySegment resolveParameter(String name) {

        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(name)) {
                return parameterValues[i];
            }
        }

        return null;
    }

    private int resolveAndValidateReturnType(
        int nodeId, int[] nodeTypes,
        int[] tokenReferences, IvyTokenStream tokenStream
    ) {

        if (nodeId == -1){
            return IvyAbstractSyntaxTreeTypes.UNKNOWN;
        }

        if (nodeTypes[nodeId] == IvyAbstractSyntaxTreeTypes.TYPE_REF) {
            return tokenStream.getTokenId(tokenReferences[nodeId]);
        }

        return nodeTypes[nodeId];
    }

    private MemorySegment[] resolveParameterTypes(
        int parameterNode,
        int[] nodeTypes,
        int[] leftOrChild,
        int[] rightOrNext,
        int[] tokenReferences,
        IvyTokenStream tokenStream
    ) {
        int count = countParameters(parameterNode, rightOrNext);

        MemorySegment[] types = new MemorySegment[count];

        int index = 0;
        int current = parameterNode;

        while (current != -1) {

            int typeNode = leftOrChild[current];

            int type = resolveAndValidateReturnType(typeNode, nodeTypes, tokenReferences, tokenStream);

            types[index++] = llvm.getLLVMType(type);
            current = rightOrNext[current];
        }
        return types;
    }

    private int countParameters(
        int parameterNode,
        int[] rightOrNext
    ) {
        int count = 0;

        while (parameterNode != -1) {
            count++;
            parameterNode = rightOrNext[parameterNode];
        }

        return count;
    }
}
