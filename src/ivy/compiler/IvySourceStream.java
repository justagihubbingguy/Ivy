package ivy.compiler;

public class IvySourceStream {

    private final char[] buffer;
    private final int length;
    private int cursor;

    public IvySourceStream(String code) {
        if (code == null) {
            this.buffer = new char[]{'\0'};
            this.length = 0;
        } else {
            int len = code.length();
            this.buffer = new char[len + 1];
            code.getChars(0, len, this.buffer, 0);
            this.buffer[len] = '\0';
            this.length = len;
        }
        this.cursor = 0;
    }

    public String getTokenText(IvyTokenStream tokenStream, int tokenRef) {
        if (tokenStream == null || tokenRef < 0 || tokenRef >= tokenStream.count()) {
            return "";
        }

        int start = tokenStream.getSourceOffset(tokenRef);
        int length = tokenStream.getLength(tokenRef);
        int end = start + length;

        int safeStart = Math.max(0, start);
        int safeEnd = Math.min(this.length, end);

        if (safeStart >= safeEnd) {
            return "";
        }

        return new String(this.buffer, safeStart, safeEnd - safeStart);
    }


    public IvySourceStream(java.nio.file.Path filePath) throws java.io.IOException {
        long size = java.nio.file.Files.size(filePath);

        this.buffer = new char[(int) size + 1];

        try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(filePath)) {
            reader.read(this.buffer, 0, (int) size);
        }

        this.buffer[(int) size] = '\0';
        this.length = (int) size;
        this.cursor = 0;
    }
    public char peek() {
        return buffer[cursor];
    }
    public char advance() {
        return buffer[cursor++];
    }
    public char peekNext() {
        return buffer[cursor + 1];
    }
    public void retreat(int steps) {
        this.cursor = Math.max(0, this.cursor - steps);
    }
    public boolean isEnd() {
        return buffer[cursor] == '\0';
    }

    public int getPosition() {
        return cursor;
    }

    public char[] getRawBuffer() {
        return buffer;
    }
}
