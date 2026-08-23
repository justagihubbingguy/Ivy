package ivy.compiler;

import ivy.compiler.exception.IvySyntaxException;
import ivy.compiler.llvm.IvyLLVMEmitter;
import ivy.compiler.exception.IvySemanticException;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IvyCompilerContext {

    private final IvySourceStream sourceStream;
    private final String fileName;
    private final String sourceCode;

    public IvyCompilerContext(String fileName, String sourceCode) {
        this.fileName = fileName;
        this.sourceCode = sourceCode;
        this.sourceStream = new IvySourceStream(sourceCode);
    }

    public void compile() {

        long startTime = System.nanoTime();

        try {

            IvyTokenStream tokenStream = new IvyTokenStream(4096);

            IvyLexer lexer = new IvyLexer(sourceStream, tokenStream);
            lexer.lexAll();

            IvyAbstractSyntaxTree ast = new IvyAbstractSyntaxTree(4096);
            IvyParser parser = new IvyParser(tokenStream, ast);
            int rootNode = parser.parseFile();

            Path filePath = Paths.get(fileName);

            System.out.println(filePath.getFileName());

            IvySemanticAnalyzer analyzer = new IvySemanticAnalyzer(filePath.getFileName().toString(), 16384);
            boolean success = analyzer.analyze(
                rootNode,
                ast.nodeType,
                ast.leftOrChild,
                ast.rightOrNext,
                ast.initNode,
                ast.tokenReference,
                tokenStream,
                sourceStream,
                ast.functionReturnType,
                ast.functionParameters,
                ast.functionBody
            );

            if (!success) {
                System.err.println("Semantic analysis failed.");
                return;
            }

            try (Arena arena = Arena.ofConfined()) {
                IvyLLVMEmitter emitter = new IvyLLVMEmitter();
                emitter.init(arena, fileName);

                emitter.compileAndFinalize(
                    rootNode,
                    ast.nodeType,
                    ast.leftOrChild,
                    ast.rightOrNext,
                    ast.functionReturnType,
                    ast.functionParameters,
                    ast.functionBody,
                    ast.initNode,
                    ast.tokenReference,
                    arena,
                    tokenStream,
                    sourceStream
                );

                emitter.dumpIntermediate();
                emitter.dispose();
            }

            long endTime = System.nanoTime();
            System.out.printf("Compilation completed successfully in %.3f ms.%n",
                (endTime - startTime) / 1_000_000.0);
        } catch (IvySyntaxException | IvySemanticException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("The - 𐌉 ᕓ 𐌙 - Programming Language.");
            System.out.println("Usage: javac --enable-native-access=ALL-UNNAMED IvyCompilerContext <source_file.ivy>");
            return;
        }

        String targetFile = args[0];
        try {
            Path path = Paths.get(targetFile);
            String code = Files.readString(path);

            long startTIme = System.nanoTime();
            IvyCompilerContext context = new IvyCompilerContext(targetFile, code);
            long endTime = System.nanoTime();

            System.out.printf("Source array cached in %.3f ms. Engine online.%n",
                (endTime - startTIme) / 1_000_000.0);

            context.compile();

        } catch (IOException e) {
            System.err.println("File I/O Error: " + targetFile);
            System.err.println("\nCheck if that file exists, otherwise, check if the file is corrupted");
        }
    }
}
