package ivy.compiler.exception;

public class IvySemanticException extends RuntimeException {
    public IvySemanticException(
        String fileName,
        int line,
        int column,
        String message
    ) {
        super(
            fileName + ":" +
                line + ":" +
                column + ": " +
                message
        );
    }
}
