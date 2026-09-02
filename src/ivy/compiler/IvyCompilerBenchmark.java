package ivy.compiler;

import ivy.compiler.exception.IvySemanticException;
import ivy.compiler.exception.IvySyntaxException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class IvyCompilerBenchmark {

    private static final int WARMUP_ITERATIONS = 20;
    private static final int BENCHMARK_ITERATIONS = 100;

    private static final Path SOURCE_FILE =
        Path.of("C:\\Users\\Hp\\Project IVY\\src\\tests\\test.ivy");

    private IvyCompilerBenchmark() {
    }

    private static void compile(String source) {

        IvySourceStream sourceStream =
            new IvySourceStream(source);

        IvyTokenStream tokenStream =
            new IvyTokenStream(16384);

        IvyLexer lexer =
            new IvyLexer(sourceStream, tokenStream);

        lexer.lexAll();

        IvyAbstractSyntaxTree ast =
            new IvyAbstractSyntaxTree(16384);

        IvyParser parser =
            new IvyParser(tokenStream, ast);

        int rootNode =
            parser.parseFile();

        IvySemanticAnalyzer analyzer =
            new IvySemanticAnalyzer(
                SOURCE_FILE.getFileName().toString(),
                16384 // reject os, return to metal
            );

        boolean success =
            analyzer.analyze(
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
            throw new IllegalStateException(
                "Semantic analysis failed"
            );
        }
    }

    public static void main(String[] args) {

        final String source;

        try {
            source = Files.readString(SOURCE_FILE);
        } catch (IOException e) {
            System.err.println(
                "Failed to read benchmark source: "
                    + SOURCE_FILE
            );
            e.printStackTrace();
            return;
        }

        System.out.println(
            "Ivy compiler benchmark"
        );

        System.out.println(
            "Source: "
                + SOURCE_FILE
        );

        System.out.println(
            "Source size: "
                + source.length()
                + " characters"
        );

        /*
         * JVM warmup.
         *
         * HotSpot gets a chance to JIT-compile the
         * lexer, parser and semantic analyzer before
         * measurements begin.
         */
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            compile(source);
        }

        System.out.println(
            "Warmup complete."
        );

        long total = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {

            long start =
                System.nanoTime();

            compile(source);

            long elapsed =
                System.nanoTime() - start;

            total += elapsed;

            if (elapsed < min) {
                min = elapsed;
            }

            if (elapsed > max) {
                max = elapsed;
            }
        }

        double averageMs =
            total
                / (double) BENCHMARK_ITERATIONS
                / 1_000_000.0;

        double minMs =
            min / 1_000_000.0;

        double maxMs =
            max / 1_000_000.0;

        double sourceCharsPerSecond =
            source.length()
                / (averageMs / 1000.0);

        System.out.println();
        System.out.println(
            "===== IVY BENCHMARK ====="
        );

        System.out.printf(
            "Iterations : %,d%n",
            BENCHMARK_ITERATIONS
        );

        System.out.printf(
            "Source     : %,d chars%n",
            source.length()
        );

        System.out.printf(
            "Average    : %.3f ms%n",
            averageMs
        );

        System.out.printf(
            "Minimum    : %.3f ms%n",
            minMs
        );

        System.out.printf(
            "Maximum    : %.3f ms%n",
            maxMs
        );

        System.out.printf(
            "Throughput : %,.0f chars/sec%n",
            sourceCharsPerSecond
        );

        System.out.println(
            "###############################"
        );
    }
}
