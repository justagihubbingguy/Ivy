package ivy.compiler.llvm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;

public final class IvyLLVMVariableManager {

    private final IvyLLVMBackend backend;

    private final Map<String, MemorySegment> variables =
        new HashMap<>();

    public IvyLLVMVariableManager(IvyLLVMBackend backend) {
        this.backend = backend;
    }

    public MemorySegment declare(
        Arena arena,
        String name
    ) {
        if (variables.containsKey(name)) {
            throw new IllegalStateException(
                "Variable already declared: " + name
            );
        }
        MemorySegment pointer =
            backend.buildAlloca(
                arena,
                name
            );

        variables.put(name, pointer);

        return pointer;
    }

    public MemorySegment get(String name) {
        return variables.get(name);
    }

    public boolean contains(String name) {
        return variables.containsKey(name);
    }

    public MemorySegment load(
        Arena arena,
        String name
    ) {
        MemorySegment pointer = variables.get(name);

        if (pointer == null) {
            throw new IllegalStateException(
                "Unknown variable: " + name
            );
        }

        return backend.buildLoad(
            arena,
            pointer,
            name
        );
    }

    public void store(
        Arena arena,
        String name,
        MemorySegment value
    ) {
        MemorySegment pointer = variables.get(name);

        if (pointer == null) {
            throw new IllegalStateException(
                "Unknown variable: " + name
            );
        }

        backend.buildStore(
            value,
            pointer
        );
    }

    public void clear() {
        variables.clear();
    }
}
