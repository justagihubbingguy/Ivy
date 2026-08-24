package ivy.compiler.llvm;

import ivy.compiler.IvySourceStream;
import ivy.compiler.IvyTokenStream;
import ivy.compiler.IvyTokens;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

import static org.llvm.c.LLVMCore.*;

public final class IvyLLVMBackend {

    private MemorySegment llvmContext;
    private MemorySegment llvmModule;
    private MemorySegment llvmBuilder;

    private MemorySegment i32Type;

    private MemorySegment toCString(Arena arena, String str) {
        return arena.allocateFrom(str + "\0", StandardCharsets.UTF_8);
    }

    public void init(Arena arena, String moduleName) {
        llvmContext = LLVMContextCreate();
        llvmModule = LLVMModuleCreateWithNameInContext(toCString(arena, moduleName), llvmContext);
        llvmBuilder = LLVMCreateBuilderInContext(llvmContext);
        i32Type = LLVMInt32TypeInContext(llvmContext);
    }

    public MemorySegment createMainFunction(Arena arena) {
        MemorySegment functionType = LLVMFunctionType(i32Type, MemorySegment.NULL, 0, 0);

        return LLVMAddFunction(llvmModule, toCString(arena, "main"), functionType);
    }
    public MemorySegment buildAlloca(
        Arena arena,
        String name
    ) {
        return LLVMBuildAlloca(
            llvmBuilder,
            i32Type,
            toCString(arena,name)
        );
    }

    public MemorySegment buildCall(
        Arena arena,
        MemorySegment functionType,
        MemorySegment callee,
        MemorySegment[] args,
        String name
    ) {
        MemorySegment argsArray = arena.allocate(
            ValueLayout.ADDRESS,
            args.length
        );
        for (int i = 0; i < args.length; i++) {
            argsArray.setAtIndex(ValueLayout.ADDRESS, i, args[i]);
        }
        return LLVMBuildCall2(
            llvmBuilder, functionType,
            callee, argsArray,
            args.length, toCString(arena, name)
        );
    }

    public MemorySegment buildFunctionType(
        Arena arena,
        MemorySegment returnType,
        MemorySegment[] parameterTypes
    ) {
        return LLVMFunctionType(
            returnType,
            toNativeTypeArray(arena, parameterTypes),
            parameterTypes.length,
            0
        );
    }

    private MemorySegment toNativeTypeArray(
        Arena arena,
        MemorySegment[] types
    ) {

        if (types.length == 0) {
            return MemorySegment.NULL;
        }

        MemorySegment array =
            arena.allocate(ValueLayout.ADDRESS, types.length);

        for (int i = 0; i < types.length; i++) {
            array.setAtIndex(
                ValueLayout.ADDRESS,
                i,
                types[i]
            );
        }

        return array;
    }

    public void nameFunctionParameters(
        MemorySegment function,
        int parameterNode,
        int[] rightOrNext,
        int[] tokenReferences,
        Arena arena,
        IvyTokenStream tokenStream,
        IvySourceStream sourceStream
    ) {
        int llvmIndex = 0;
        int current = parameterNode;

        while (current != -1) {

            int tokenRef = tokenReferences[current];

            String name = sourceStream.getTokenText(
                tokenStream,
                tokenRef
            );

            MemorySegment parameter =
                LLVMGetParam(function, llvmIndex);

            MemorySegment nameString =
                arena.allocateFrom(name + "\0");

            LLVMSetValueName2(
                parameter,
                nameString,
                name.length()
            );

            llvmIndex++;
            current = rightOrNext[current];
        }
    }

    public MemorySegment buildFunction(
        Arena arena, String name, MemorySegment functionType
    ){
        return LLVMAddFunction(llvmModule, toCString(arena, name), functionType);
    }

    public MemorySegment buildLoad(
        Arena arena,
        MemorySegment pointer,
        String name
    ) {
        return LLVMBuildLoad2(llvmBuilder, i32Type, pointer, arena.allocateFrom(name + "\0", StandardCharsets.UTF_8));
    }

    public void buildStore(
        MemorySegment value,
        MemorySegment pointer
    ) {
        LLVMBuildStore(
            llvmBuilder,
            value,
            pointer
        );
    }
    public void buildRetVoid() {
        LLVMBuildRetVoid(llvmBuilder);
    }

    public MemorySegment createEntryBlock(Arena arena, MemorySegment function) {
        MemorySegment entryBlock = LLVMAppendBasicBlockInContext(llvmContext, function, toCString(arena, "entry"));

        LLVMPositionBuilderAtEnd(llvmBuilder, entryBlock);

        return entryBlock;
    }

    public MemorySegment constI32(int value) {return LLVMConstInt(i32Type, value, 0);
    }

    public MemorySegment buildAdd(Arena arena, MemorySegment left, MemorySegment right) {
        return LLVMBuildAdd(llvmBuilder, left, right, toCString(arena, "addtmp"));
    }

    public MemorySegment buildSub(Arena arena, MemorySegment left, MemorySegment right) {
        return LLVMBuildSub(llvmBuilder, left, right, toCString(arena, "subtmp"));
    }

    public MemorySegment buildMul(Arena arena, MemorySegment left, MemorySegment right) {
        return LLVMBuildMul(llvmBuilder, left, right, toCString(arena, "multmp"));
    }

    public MemorySegment buildDiv(Arena arena, MemorySegment left, MemorySegment right) {
        return LLVMBuildSDiv(llvmBuilder, left, right, toCString(arena, "divtmp"));
    }

    public void buildRet(MemorySegment value) {
        LLVMBuildRet(llvmBuilder, value);
    }

    public void dumpModule() {
        MemorySegment message = LLVMPrintModuleToString(llvmModule);
        System.out.println(message.getString(0));
        LLVMDisposeMessage(message);
    }

    public void dispose() {
        if (llvmBuilder != null) {
            LLVMDisposeBuilder(llvmBuilder);
            llvmBuilder = null;
        }

        if (llvmModule != null) {
            LLVMDisposeModule(llvmModule);
            llvmModule = null;
        }

        if (llvmContext != null) {
            LLVMContextDispose(llvmContext);
            llvmContext = null;
        }
    }
    public MemorySegment getLLVMType(int ivyType) {
        return switch (ivyType) {
            case IvyTokens.KEYWORD_INT32 ->
                i32Type;

            default ->
                throw new IllegalArgumentException(
                    "Unsupported Ivy type: " + ivyType
                );
        };
    }
    public MemorySegment getContext() {
        return llvmContext;
    }

    public MemorySegment getModule() {
        return llvmModule;
    }

    public MemorySegment getBuilder() {
        return llvmBuilder;
    }

    public MemorySegment getI32Type() {
        return i32Type;
    }
}
