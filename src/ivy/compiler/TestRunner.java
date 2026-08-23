package ivy.compiler;

public class TestRunner {

    static {
        System.load("C:\\Tools\\LLVM-21.1.8-Compiler\\llvm-21.1.8-windows-amd64-msvc17-msvcrt\\bin\\LLVM-C.dll");
    }

    public static void main(String[] args) throws Exception {
        String filePath = "C:\\Users\\Hp\\Project IVY\\src\\tests\\test.ivy";
        IvyCompilerContext.main(new String[]{ filePath });
    }

}
