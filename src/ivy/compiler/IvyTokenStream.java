package ivy.compiler;

public class IvyTokenStream {

    // non-gc overhead,  assembly speed zero-copy
    private long[]  tokens;
    private int count  = 0;
    private int[] sourceOffsets;
    private int cursor = 0;

    // the initial capacity of the token stream

    public IvyTokenStream(int initialCapacity) {
        this.tokens = new long[initialCapacity];
        this.sourceOffsets = new int[initialCapacity];
    }

    // write tokens into the packed stream

    public void writeToken(int tokenID, int line, int column) {

        // if the count of the tokens is smaller than that of tokens then we grow

        writeToken(tokenID, line, column, 0,0);
    }
    public void writeTokenRefined(int tokenID, int line, int column, int wordLen, int wordStart) {
        writeToken(tokenID, line, column, wordLen, wordStart);
    }

    public int getSourceOffset(int index) {
        return sourceOffsets[index];
    }

    // zero-overhead fast math utility functions (no gc overhead)

    public int getTokenId(int index) { return (int) (tokens[index] >>> 52); }
    public int getLine(int index)     { return (int) ((tokens[index] >>> 20) & 0xFFFFFL); }
    public int getColumn(int index)   { return (int) (tokens[index] & 0xFFFFFL); }

    // extremely fast zero-copy, avoids java garbage collector
    public int count() {
        return count;
    }

    private void grow() {
        int newCapacity = tokens.length == 0
            ? 8
            : tokens.length * 2;

        long[] newTokens = new long[newCapacity];
        int[] newSourceOffsets = new int[newCapacity];

        System.arraycopy(
            tokens,
            0,
            newTokens,
            0,
            tokens.length
        );

        System.arraycopy(
            sourceOffsets,
            0,
            newSourceOffsets,
            0,
            sourceOffsets.length
        );

        tokens = newTokens;
        sourceOffsets = newSourceOffsets;
    }

    public void writeToken(int tokenID, int line, int column, int length, int sourceOffset) {
        if (count >= tokens.length) {
            grow();
        }

        long packed = (((long) tokenID & 0xFFFL) << 52)
            | (((long) length & 0xFFFL) << 40)
            | (((long) line & 0xFFFFFL) << 20)
            | ((long) column & 0xFFFFFL);

        sourceOffsets[count] = sourceOffset;
        tokens[count++] = packed;
    }

    public int getLength(int index) {
        return (int) ((tokens[index] >>> 40) & 0xFFFL);
    }
    // advance onto next line

    private int advance() {
        if (cursor >= count) return 0;
        return getTokenId(cursor++);
    }
}
