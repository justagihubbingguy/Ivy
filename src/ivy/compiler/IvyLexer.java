package ivy.compiler;

public final class IvyLexer {

    public final char[] buf;

    private int ptr = 0;

    private final IvyTokenStream tokenStream;

    private int currentLine = 1;

    private int lineStartOffset = 0;

    public IvyLexer(IvySourceStream sourceStream, IvyTokenStream tokenStream) {
        this.buf = sourceStream.getRawBuffer();

        this.tokenStream = tokenStream;
    }

    public void lexAll() {
        final char[] buffer = this.buf;
        int localPtr = this.ptr;
        int line = this.currentLine;
        int lineStart = this.lineStartOffset;

        while (true) {
            int startPos = localPtr;
            char c = buffer[localPtr++];

            if (c == '\0') {

                localPtr--;

                break;
            }

            if (c == ' ' || c == '\t' || c == '\r') continue;

            if (c == '\n') {
                line++;
                lineStart = localPtr;
                continue;
            }

            int column = (startPos - lineStart) + 1;

            switch (c) {
                case ';': tokenStream.writeToken(IvyTokens.SEMICOLON, line, column); continue;

                case ',': tokenStream.writeToken(IvyTokens.COMMA, line, column); continue;

                case '(': tokenStream.writeToken(IvyTokens.LPAREN, line, column); continue;

                case ')': tokenStream.writeToken(IvyTokens.RPAREN, line, column); continue;

                case '{': tokenStream.writeToken(IvyTokens.LBRACE, line, column); continue;

                case '}': tokenStream.writeToken(IvyTokens.RBRACE, line, column); continue;

                case '[': tokenStream.writeToken(IvyTokens.L_BRACKET, line, column); continue;

                case ']': tokenStream.writeToken(IvyTokens.RBRACKET, line, column); continue;

                case '%': tokenStream.writeToken(IvyTokens.PERCENT, line, column); continue;

                case '*':
                    if (buffer[localPtr] == '=') {
                        localPtr++;
                        tokenStream.writeToken(IvyTokens.ASTERISK_ASSIGN, line, column);
                    } else {
                        tokenStream.writeToken(IvyTokens.ASTERISK, line, column);
                    }
                    continue;

                case '/':
                    if (buffer[localPtr] == '/') {
                        localPtr++;

                        while(buffer[localPtr] != '\n' && buffer[localPtr] != '\0') {
                            localPtr++;
                        }
                        continue;
                    }
                    // multi-line comment like /*
                    if (buffer[localPtr] == '*') {
                        localPtr++;

                        boolean terminatedComment = false;

                        while (true) {
                            char currentCommentChar = buffer[localPtr++];
                            if (currentCommentChar == '\0') {
                                break;
                            }
                            if (currentCommentChar == '\n') {
                                line++;
                                lineStart = localPtr;
                                continue;
                            }
                            if (currentCommentChar == '*' && buffer[localPtr] == '/') {
                                localPtr++;
                                terminatedComment = true;
                                break;
                            }
                        }
                        if (!terminatedComment) {
                            System.err.printf(
                                "Unterminated block comment at line %d, col %d%n",
                                line,
                                column
                            );
                        }
                        continue;
                    }
                    // slash assign
                    if (buffer[localPtr] == '=') {
                        localPtr++;
                        tokenStream.writeToken(IvyTokens.SLASH_ASSIGN, line, column);
                    } else {
                        tokenStream.writeToken(IvyTokens.DIVISION, line, column);
                    }
                    continue;

                case '-':
                    if (buffer[localPtr] == '-') {
                        localPtr++;
                        tokenStream.writeToken(IvyTokens.DECREMENT, line, column);
                    } else if (buffer[localPtr] == '=') {
                        localPtr++;
                        tokenStream.writeToken(IvyTokens.MINUS_ASSIGN, line, column);
                    } else {
                        tokenStream.writeToken(IvyTokens.SUBTRACTION, line, column);
                    }
                    continue;
                case '+':
                    if (buffer[localPtr] == '+') {
                        localPtr++;
                        tokenStream.writeToken(IvyTokens.INCREMENT, line, column);
                    } else if (buffer[localPtr] == '=') {
                        localPtr++;
                        tokenStream.writeToken(IvyTokens.PLUS_ASSIGN, line, column);
                    } else {
                        tokenStream.writeToken(IvyTokens.PLUS, line, column);
                    }
                    continue;

                case '.': tokenStream.writeToken(IvyTokens.DOT, line, column); continue;

                case ':': tokenStream.writeToken(IvyTokens.COLON, line, column); continue;

                case '@': tokenStream.writeToken(IvyTokens.AT, line, column); continue;

                case '!':
                    if (buffer[localPtr] == '=') {
                        localPtr++;
                        tokenStream.writeToken(IvyTokens.NOT_EQUAL, line, column);
                    } else {
                        tokenStream.writeToken(IvyTokens.EXCLAMATION, line, column);
                    }
                    continue;

                case '=':
                    if (buffer[localPtr] == '=') { localPtr++; tokenStream.writeToken(IvyTokens.EQUAL, line, column); }
                    else { tokenStream.writeToken(IvyTokens.ASSIGN, line, column); }
                    continue;

                case '&':
                    if (buffer[localPtr] == '&') { localPtr++; tokenStream.writeToken(IvyTokens.LOGICAL_AND, line, column); }
                    else { tokenStream.writeToken(IvyTokens.AMPERSAND, line, column); }
                    continue;

                case '|':
                    if (buffer[localPtr] == '|') { localPtr++; tokenStream.writeToken(IvyTokens.LOGICAL_OR, line, column); }
                    else { tokenStream.writeToken(IvyTokens.BAR, line, column); }
                    continue;

                case '"':
                    boolean terminatedString = false;

                    while (true) {
                        char sc = buffer[localPtr++];

                        if (sc == '\\') {
                            if (buffer[localPtr] != '\0') {
                                localPtr++;
                            }
                        } else if (sc == '"') {
                            terminatedString = true;
                            break;
                        } else if (sc == '\0') {
                            break;
                        } else if (sc == '\n') {
                            line++;
                            lineStart = localPtr;
                        }
                    }

                    if (!terminatedString) {
                        System.err.printf(
                            "Unterminated string at line %d, col %d%n",
                            line,
                            column
                        );
                        continue;
                    }

                    tokenStream.writeToken(IvyTokens.DOUBLE_QOUTE, line, column);
                    continue;

                case '\'':
                    boolean terminatedChar = false;

                    while (true) {
                        char sc = buffer[localPtr++];

                        if (sc == '\\') {
                            if (buffer[localPtr] != '\0') {
                                localPtr++;
                            }
                        } else if (sc == '\'') {
                            terminatedChar = true;
                            break;
                        } else if (sc == '\0') {
                            break;
                        } else if (sc == '\n') {
                            line++;
                            lineStart = localPtr;
                        }
                    }

                    if (!terminatedChar) {
                        System.err.printf(
                            "Unterminated character literal at line %d, col %d%n",
                            line,
                            column
                        );
                        continue;
                    }

                    tokenStream.writeToken(IvyTokens.SINGLE_QOUTE, line, column);
                    continue;
                case '<':
                    if (buffer[localPtr] == '=') {
                        localPtr++;
                        tokenStream.writeToken(IvyTokens.LESS_EQUAL, line, column);
                    } else {
                        tokenStream.writeToken(IvyTokens.LESS_THAN, line, column);
                    }
                    continue;
                case '>':
                    if (buffer[localPtr] == '=') {
                        localPtr++;
                        tokenStream.writeToken(IvyTokens.GREATER_EQUAL, line, column);
                    } else {
                        tokenStream.writeToken(IvyTokens.GREATER_THAN, line, column);
                    }
                    continue;
                case '^':
                    tokenStream.writeToken(IvyTokens.BITWISE_XOR, line, column);
                    continue;
            }


            if (c >= '0' && c <= '9') {
                boolean hasDot = false;

                while (true) {
                    char nc = buffer[localPtr];

                    if (nc >= '0' && nc <= '9') {
                        localPtr++;
                    } else if (nc == '.' && !hasDot) {
                        char nextChar = buffer[localPtr + 1];

                        if (nextChar >= '0' && nextChar <= '9') {
                            hasDot = true;
                            localPtr++;
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                }

                tokenStream.writeToken(IvyTokens.NUMBER, line, column);
                continue;
            }

            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') {
                int wordStart = localPtr - 1;
                while (true) {
                    char nc = buffer[localPtr];
                    if ((nc >= 'a' && nc <= 'z') || (nc >= 'A' && nc <= 'Z') || (nc >= '0' && nc <= '9') || nc == '_') {
                        localPtr++;
                    } else { break; }
                }
                int wordLen = localPtr - wordStart;
                int tokenType = resolveKeyword(buffer, wordStart, wordLen);
                tokenStream.writeTokenRefined(tokenType, line, column, wordLen, wordStart);
                continue;
            }

            System.err.printf("Unrecognized token/byte at line %d, col %d: '%c' (0x%02X)%n", line, column, c, (int) c);
        }

        tokenStream.writeToken(IvyTokens.EOF, line, (localPtr - lineStart) + 1);
        this.ptr = localPtr;
        this.currentLine = line;
        this.lineStartOffset = lineStart;
    }
    private int resolveKeyword(final char[] b, final int start, final int wordLen) {
        switch (wordLen) {
            case 2:
                if (b[start] == 'i' && b[start + 1] == 'f') return IvyTokens.KEYWORD_IF;
                if (b[start] == 'a' && b[start + 1] == 't') return IvyTokens.AT;
                break;

            case 3:
                switch (b[start]) {
                    case 'i': if (b[start + 1] == 'n' && b[start + 2] == 't') return IvyTokens.KEYWORD_INT32; break;
                    case 'f': if (b[start + 1] == 'o' && b[start + 2] == 'r') return IvyTokens.KEYWORD_FOR; break;
                    case 'n': if (b[start + 1] == 'e' && b[start + 2] == 'w') return IvyTokens.KEYWORD_NEW; break;
                }
                break;

            case 4:
                switch (b[start]) {
                    case 'b': if (b[start + 1] == 'y' && b[start + 2] == 't' && b[start + 3] == 'e') return IvyTokens.KEYWORD_BYTE; break;
                    case 'i': if (b[start + 1] == 'n' && b[start + 2] == 't' && b[start + 3] == '8') return IvyTokens.KEYWORD_INT8; break;
                    case 'e': if (b[start + 1] == 'l' && b[start + 2] == 's' && b[start + 3] == 'e') return IvyTokens.KEYWORD_ELSE; break;
                    case 't':
                        if (b[start + 1] == 'h' && b[start + 2] == 'i' && b[start + 3] == 's') return IvyTokens.KEYWORD_THIS;
                        if (b[start + 1] == 'r' && b[start + 2] == 'u' && b[start + 3] == 'e') return IvyTokens.LITERAL_TRUE;
                        break;
                    case 'v': if (b[start + 1] == 'o' && b[start + 2] == 'i' && b[start + 3] == 'd') return IvyTokens.KEYWORD_VOID; break;
                    case 'n': if (b[start + 1] == 'u' && b[start + 2] == 'l' && b[start + 3] == 'l') return IvyTokens.NULL; break;
                    case 's': if (b[start + 1] == 'y' && b[start + 2] == 'n' && b[start + 3] == 'c') return IvyTokens.KEYWORD_SYNC; break;
                    case 'u': if (b[start + 1] == 'i' && b[start + 2] == 'n' && b[start + 3] == 't') return IvyTokens.KEYWORD_UINT32; break;
                    case 'l': if (b[start + 1] == 'o' && b[start + 2] == 'n' && b[start + 3] == 'g') return IvyTokens.KEYWORD_INT64; break;
                    case 'h': if (b[start + 1] == 'a' && b[start + 2] == 'l' && b[start + 3] == 'f') return IvyTokens.KEYWORD_FLOAT16; break;
                    case 'c': if (b[start + 1] == 'h' && b[start + 2] == 'a' && b[start + 3] == 'r') return IvyTokens.KEYWORD_CHAR8; break;
                }
                break;

            case 5:
                switch (b[start]) {
                    case 'i':
                        if (b[start + 1] == 'n' && b[start + 2] == 't') {
                            if (b[start + 3] == '1' && b[start + 4] == '6') return IvyTokens.KEYWORD_INT16;
                            if (b[start + 3] == '3' && b[start + 4] == '2') return IvyTokens.KEYWORD_INT32;
                            if (b[start + 3] == '6' && b[start + 4] == '4') return IvyTokens.KEYWORD_INT64;
                        }
                        break;
                    case 'u':
                        if (b[start + 1] == 'i' && b[start + 2] == 'n' && b[start + 3] == 't' && b[start + 4] == '8') return IvyTokens.KEYWORD_UINT8;
                        if (b[start + 1] == 'b' && b[start + 2] == 'y' && b[start + 3] == 't' && b[start + 4] == 'e') return IvyTokens.KEYWORD_UBYTE;
                        if (b[start + 1] == 'l' && b[start + 2] == 'o' && b[start + 3] == 'n' && b[start + 4] == 'g') return IvyTokens.KEYWORD_UINT64;
                        if (b[start + 1] == 'c' && b[start + 2] == 'h' && b[start + 3] == 'a' && b[start + 4] == 'r') return IvyTokens.KEYWORD_UCHAR8;
                        break;
                    case 's': if (b[start + 1] == 'h' && b[start + 2] == 'o' && b[start + 3] == 'r' && b[start + 4] == 't') return IvyTokens.KEYWORD_INT16; break;
                    case 'f':
                        if (b[start + 1] == 'l' && b[start + 2] == 'o' && b[start + 3] == 'a' && b[start + 4] == 't') return IvyTokens.KEYWORD_FLOAT32;
                        if (b[start + 1] == 'a' && b[start + 2] == 'l' && b[start + 3] == 's' && b[start + 4] == 'e') return IvyTokens.LITERAL_FALSE;
                        if (b[start + 1] == 'i' && b[start + 2] == 'n' && b[start + 3] == 'a' && b[start + 4] == 'l') return IvyTokens.MODFR_FINAL;
                        break;
                    case 'w': if (b[start + 1] == 'h' && b[start + 2] == 'i' && b[start + 3] == 'l' && b[start + 4] == 'e') return IvyTokens.KEYWORD_WHILE; break;
                    case 'c': if (b[start + 1] == 'l' && b[start + 2] == 'a' && b[start + 3] == 's' && b[start + 4] == 's') return IvyTokens.KEYWORD_CLASS; break;
                    case 'a': if (b[start + 1] == 'w' && b[start + 2] == 'a' && b[start + 3] == 'k' && b[start + 4] == 'e') return IvyTokens.AWAKE; break;
                }
                break;

            case 6:
                switch (b[start]) {
                    case 'u':
                        if (b[start + 1] == 'i' && b[start + 2] == 'n' && b[start + 3] == 't') {
                            if (b[start + 4] == '1' && b[start + 5] == '6') return IvyTokens.KEYWORD_UINT16;
                            if (b[start + 4] == '3' && b[start + 5] == '2') return IvyTokens.KEYWORD_UINT32;
                            if (b[start + 4] == '6' && b[start + 5] == '4') return IvyTokens.KEYWORD_UINT64;
                        }
                        break;
                    case 'f':
                        if (b[start + 1] == 'l' && b[start + 2] == 'o' && b[start + 3] == 'a' && b[start + 4] == 't' && b[start + 5] == '8') return IvyTokens.KEYWORD_FLOAT8;
                        break;
                    case 'c':
                        if (b[start + 1] == 'h' && b[start + 2] == 'a' && b[start + 3] == 'r') {
                            if (b[start + 4] == '1' && b[start + 5] == '6') return IvyTokens.KEYWORD_CHAR16;
                            if (b[start + 4] == '3' && b[start + 5] == '2') return IvyTokens.KEYWORD_CHAR32;
                            if (b[start + 4] == '6' && b[start + 5] == '4') return IvyTokens.KEYWORD_CHAR64;
                            if (b[start + 4] == '0' && b[start + 5] == '8') return IvyTokens.KEYWORD_CHAR8;
                        }
                        break;
                    case 'd': if (b[start + 1] == 'o' && b[start + 2] == 'u' && b[start + 3] == 'b' && b[start + 4] == 'l' && b[start + 5] == 'e') return IvyTokens.KEYWORD_FLOAT64; break;
                    case 's':
                        if (b[start + 1] == 't') {
                            if (b[start + 2] == 'r' && b[start + 3] == 'u' && b[start + 4] == 'c' && b[start + 5] == 't') return IvyTokens.KEYWORD_STRUCT;
                            if (b[start + 2] == 'r' && b[start + 3] == 'i' && b[start + 4] == 'n' && b[start + 5] == 'g') return IvyTokens.KEYWORD_STRING;
                            if (b[start + 2] == 'a' && b[start + 3] == 't' && b[start + 4] == 'i' && b[start + 5] == 'c') return IvyTokens.MODFR_STATIC;
                        }
                        break;
                    case 'r': if (b[start + 1] == 'e' && b[start + 2] == 't' && b[start + 3] == 'u' && b[start + 4] == 'r' && b[start + 5] == 'n') return IvyTokens.KEYWORD_RETURN; break;
                    case 'p': if (b[start + 1] == 'u' && b[start + 2] == 'b' && b[start + 3] == 'l' && b[start + 4] == 'i' && b[start + 5] == 'c') return IvyTokens.MODFR_PUBLIC; break;
                }
                break;

            case 7:
                switch (b[start]) {
                    case 'f':
                        if (b[start + 1] == 'l' && b[start + 2] == 'o' && b[start + 3] == 'a' && b[start + 4] == 't') {
                            if (b[start + 5] == '1' && b[start + 6] == '6') return IvyTokens.KEYWORD_FLOAT16;
                            if (b[start + 5] == '3' && b[start + 6] == '2') return IvyTokens.KEYWORD_FLOAT32;
                            if (b[start + 5] == '6' && b[start + 6] == '4') return IvyTokens.KEYWORD_FLOAT64;
                        }
                        break;
                    case 'u':
                        if (b[start + 1] == 'c' && b[start + 2] == 'h' && b[start + 3] == 'a' && b[start + 4] == 'r') {
                            if (b[start + 5] == '1' && b[start + 6] == '6') return IvyTokens.KEYWORD_UCHAR16;
                            if (b[start + 5] == '3' && b[start + 6] == '2') return IvyTokens.KEYWORD_UCHAR32;
                            if (b[start + 5] == '6' && b[start + 6] == '4') return IvyTokens.KEYWORD_UCHAR64;
                            if (b[start + 5] == '0' && b[start + 6] == '8') return IvyTokens.KEYWORD_UCHAR8;
                        }
                        break;
                    case 'p': if (b[start + 1] == 'r' && b[start + 2] == 'i' && b[start + 3] == 'v' && b[start + 4] == 'a' && b[start + 5] == 't' && b[start + 6] == 'e') return IvyTokens.MODFR_PRIVATE; break;
                    case 'm': if (b[start + 1] == 'e' && b[start + 2] == 'm' && b[start + 3] == 'f' && b[start + 4] == 'r' && b[start + 5] == 'e' && b[start + 6] == 'e') return IvyTokens.KEYWORD_MEMFREE; break;
                }
                break;

            case 8:
                if (b[start] == 'm' && b[start + 1] == 'e' && b[start + 2] == 'm' && b[start + 3] == 'a' && b[start + 4] == 'l' && b[start + 5] == 'l' && b[start + 6] == 'o' && b[start + 7] == 'c') return IvyTokens.KEYWORD_MEMALLOC;
                if (b[start] == 'u' && b[start + 1] == 'n' && b[start + 2] == 's' && b[start + 3] == 'i' && b[start + 4] == 'g' && b[start + 5] == 'n' && b[start + 6] == 'e' && b[start + 7] == 'd') return IvyTokens.MODFR_UNSIGNED;
                break;

            case 9:
                if (b[start] == 'p' && b[start + 1] == 'r' && b[start + 2] == 'o' && b[start + 3] == 't' && b[start + 4] == 'e' && b[start + 5] == 'c' && b[start + 6] == 't' && b[start + 7] == 'e' && b[start + 8] == 'd') return IvyTokens.MODFR_PROTECTED;
                break;

            case 10:
                if (b[start] == 'a' && b[start + 1] == 'n' && b[start + 2] == 'n' && b[start + 3] == 'o' && b[start + 4] == 't' && b[start + 5] == 'a' && b[start + 6] == 't' && b[start + 7] == 'i' && b[start + 8] == 'o' && b[start + 9] == 'n') return IvyTokens.ANNOTATION;
                break;
        }
        return IvyTokens.IDENTIFIER;
    }

}
