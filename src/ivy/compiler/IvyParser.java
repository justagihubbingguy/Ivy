package ivy.compiler;

import ivy.compiler.exception.IvySyntaxException;

public class IvyParser {

    private final IvyTokenStream tokenStream;
    private final IvyAbstractSyntaxTree ast;
    private int cursor = 0;

    public IvyParser(IvyTokenStream tokenStream, IvyAbstractSyntaxTree ast) {
        //important, this is for the java conventions, i need to make it TK - AST or warnings
        this.tokenStream = tokenStream;
        this.ast = ast;
    }

    private int peekTokenId() {
        return tokenStream.getTokenId(cursor);
    }

    private boolean match(int expected) {
        if (peekTokenId() == expected) {
            cursor++;
            return true;
        }
        return false;
    }

    private void expect(int expected, String errorMsg) {
        if (!match(expected)) {
            throw new IvySyntaxException(
                String.format(
                    "%s at line %d, col %d",
                    errorMsg,
                    tokenStream.getLine(cursor),
                    tokenStream.getColumn(cursor)
                )
            );
        }
    }

    private void panicRecover() {
        int safetyCounter = 0;
        while (safetyCounter++ < 1000) {
            int tok = peekTokenId();

            if (tok == IvyTokens.SEMICOLON || tok == IvyTokens.RBRACE || tok == 0 || tok == IvyTokens.EOF) {
                if (tok == IvyTokens.SEMICOLON) cursor++;
                break;
            }

            if (tok == IvyTokens.KEYWORD_IF ||
                tok == IvyTokens.KEYWORD_RETURN ||
                tok == IvyTokens.KEYWORD_STRUCT ||
                tok == IvyTokens.KEYWORD_CLASS ||
                tok == IvyTokens.KEYWORD_SYNC) {
                break;
            }

            cursor++;
        }

        while (peekTokenId() == IvyTokens.SEMICOLON) {
            cursor++;
        }
    }

    public int parseFile() {
        return parseBlockBody(IvyTokens.EOF);
    }

    private int parseBlockBody(int endTokenId) {
        int headNode = -1;
        int endNode = -1;
        int safetyCounter = 0;

        while (peekTokenId() != endTokenId && peekTokenId() != IvyTokens.EOF && safetyCounter++ < 10000) {
            int stmtNode = parseTopLevelOrStatement();

            if (stmtNode == -1) {
                panicRecover();
                if (peekTokenId() == endTokenId || peekTokenId() == IvyTokens.EOF) break;
                continue;
            }

            if (headNode == -1) {
                headNode = stmtNode;
            } else {
                ast.rightOrNext[endNode] = stmtNode;
            }

            endNode = stmtNode;
        }
        return headNode;
    }

    private int parseTopLevelOrStatement() {
//        System.out.println(
//            "Parsing token ID: " + peekTokenId() + " at cursor: " + cursor
//        );

        int annotationHead = -1;

        if (peekTokenId() == IvyTokens.BAR ||
            isTheAnnotationStartPratt()) {
            annotationHead = parseAnnotationsPratt();
        }
        if (peekTokenId() == IvyTokens.KEYWORD_SYNC) {
            return parseSyncStatement();
        }
        // make sure it`s either a struct, class, or a statement
        if (peekTokenId() == IvyTokens.KEYWORD_STRUCT) {
            return parseAggregateDecl(IvyAbstractSyntaxTreeTypes.STRUCT_DECL);
        }
        if (peekTokenId() == IvyTokens.KEYWORD_CLASS) {
            return parseAggregateDecl(IvyAbstractSyntaxTreeTypes.CLASS_DECL);
        }

        int modifierMask = parseModifiers();

        if ((modifierMask & 0x01) != 0) {
            if (!isAFunctionDeclaration()) {
                System.err.printf(
                    "Syntax Exception: 'public:' is only valid for functions at line %d, col %d%n",
                    tokenStream.getLine(cursor),
                    tokenStream.getColumn(cursor)
                );
                panicRecover();
                return -1;
            }

            return parseFunctionDecl(annotationHead, modifierMask);
        }

        if (isAFunctionDeclaration()) {
            return parseFunctionDecl(annotationHead, modifierMask);
        }

        if (isExplicitType(peekTokenId()) ||
            isAnIdentifierForVarDeclaration()) {
            return parseVarDecl(modifierMask);
        }

        return parseStatement();
    }

    private int parseAggregateDecl(int nodeType) {
        cursor++; // consume struct/class keyword
        int nameTokenId = cursor;
        expect(IvyTokens.IDENTIFIER, "Expected name after aggregate definition");
        expect(IvyTokens.LBRACE, "Expected '{' to start body");
        int bodyHead = parseAggregateBody();
        expect(IvyTokens.RBRACE, "Expected '}' at the end of body");
        return ast.allocateASTNode(nodeType, nameTokenId, bodyHead, -1);
    }

    private int parseAggregateBody() {
        int headMember = -1;
        int lastMember = -1;

        while (peekTokenId() != IvyTokens.RBRACE && peekTokenId() != IvyTokens.EOF) {
            int modifierMask = parseModifiers();
            int memberNode = -1;

            if (isAFunctionDeclaration()) {
                memberNode = parseFunctionDecl(-1, modifierMask);
            } else if (isExplicitType(peekTokenId()) || isAnIdentifierForVarDeclaration()) {
                memberNode = parseVarDecl(modifierMask);
            } else {
                cursor++;
                continue;
            }

            if (headMember == -1) {
                headMember = memberNode;
            } else {
                ast.rightOrNext[lastMember] = memberNode;
            }
            lastMember = memberNode;
        }
        return headMember;
    }

    private int parseSyncStatement() {
        int syncTokenId = cursor;
        cursor++; // consume sync
        int pathStartTokenId = cursor;

        while (peekTokenId() == IvyTokens.IDENTIFIER || peekTokenId() == IvyTokens.DOT) {
            cursor++;
        }
        int pathEndTokenId = cursor - 1;

        if (pathEndTokenId < pathStartTokenId) {
            panicRecover();
            return -1;
        }

        expect(IvyTokens.SEMICOLON, "Expected ';' at the end of the line");
        int pathNode = ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.PATH, pathStartTokenId, pathEndTokenId, -1);
        return ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.SYNC_STMT, syncTokenId, pathNode, -1);
    }

    private int parseVarDecl(int modifierMask) {
        int typeNode = parseType();
        int nameTokenId = cursor;
        expect(IvyTokens.IDENTIFIER, "Expected variable name after type");

        int initNode = -1;
        if (match(IvyTokens.ASSIGN)) {
            initNode = parseExpression(0);
        }

        expect(IvyTokens.SEMICOLON, "Expected ';' after expression");
        int varNode = ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.VAR_DEC, nameTokenId, typeNode, -1);

        ast.initNode[varNode] = initNode;
        if (modifierMask != 0) {
            ast.modifiers[varNode] = modifierMask;
        }
        return varNode;
    }

    public int parseStatement() {
        int tokenType = peekTokenId();
        int tokenIdPos = cursor;

        switch (tokenType) {
            case IvyTokens.KEYWORD_IF:
                return parseIfStatementsPratt();
            case IvyTokens.KEYWORD_RETURN:
                cursor++;
                int expr = parseExpression(0);
                expect(IvyTokens.SEMICOLON, "Expected ';' at the end of return value");
                return ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.RETURN_STMT, tokenIdPos, expr, -1);
            case IvyTokens.KEYWORD_WHILE:
                return parseWhileStatementsPratt();
            case IvyTokens.KEYWORD_FOR:
                return parseForStatementsPratt();
            case IvyTokens.LBRACE:
                return parseIndentBlock();

            default:
                if (isExplicitType(tokenType) || isAnIdentifierForVarDeclaration()) {
                    return parseVarDecl(0);
                }

                int exprNode = parseExpression(0);
                expect(IvyTokens.SEMICOLON, "Expected ';' after expression");
                return exprNode;
        }
    }

    private int parseFunctionDecl(int annotationHead, int modifierMask) {
        int fnNameTokenId = cursor;
        expect(IvyTokens.IDENTIFIER, "Expected function name");
        expect(IvyTokens.LPAREN, "Expected '(' after function name");

        int returnTypeNode = parseType();
        int paramHead = -1;

        if (match(IvyTokens.COMMA)) {
            paramHead = parseCurrentParameterListPratt();
        }

        expect(IvyTokens.RPAREN, "Expected ')' after return type/parameters");
        int bodyNode = parseIndentBlock();

        int fnNode = ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.FUNC_DECL, fnNameTokenId, -1, -1);

        ast.functionReturnType[fnNode] = returnTypeNode;
        ast.functionParameters[fnNode] = paramHead;
        ast.functionBody[fnNode] = bodyNode;

        if (modifierMask != 0) {
            ast.modifiers[fnNode] = modifierMask;
        }
        if (annotationHead != -1) {
            int currAnnott = annotationHead;
            while (ast.rightOrNext[currAnnott] != -1) {
                currAnnott = ast.rightOrNext[currAnnott];
            }
            ast.leftOrChild[currAnnott] = fnNode;
        }
        return fnNode;
    }

    private int parseCurrentParameterListPratt() {
        int headParam = -1;
        int lastParam = -1;

        while (peekTokenId() != IvyTokens.RPAREN && peekTokenId() != IvyTokens.EOF) {
            int typeNode = parseType();
            int nameTokenId = -1;
            if (peekTokenId() == IvyTokens.IDENTIFIER) {
                nameTokenId = cursor++;
            }

            int paramNode = ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.PARAM, nameTokenId, typeNode, -1);

            // so no more annoying "Node = -1"
            if (headParam == -1) {
                headParam = paramNode;
            } else {
                ast.rightOrNext[lastParam] = paramNode;
            }
            lastParam = paramNode;

            if (!match(IvyTokens.COMMA)) {
                break;
            }
        }

        return headParam;
    }


    private int parseType() {
        int baseTypeTokenId = cursor;
        cursor++;

        while (peekTokenId() == IvyTokens.DOT) {
            cursor++;
            expect(IvyTokens.IDENTIFIER, "Expected identifier after '.' in type path");
        }

        int typeNode = ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.TYPE_REF, baseTypeTokenId, -1, -1);

        if (match(IvyTokens.ASTERISK)) {
            return ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.POINTER_TYPE, baseTypeTokenId, typeNode, -1);
        }

        return typeNode;
    }

    private int parseIfStatementsPratt() {
        int ifTokenId = cursor;
        cursor++;

        expect(IvyTokens.LPAREN, "Expected '(' after 'if'");
        int condNode = parseExpression(0);
        expect(IvyTokens.RPAREN, "Expected ')' after conditionals");

        int thenBodyNode = parseIndentBlock();

        int elseBodyNode = -1;

        if(peekTokenId() == IvyTokens.KEYWORD_ELSE) {
            cursor++;
            elseBodyNode = parseIndentBlock();
        }

        int ifNode = ast.allocateASTNode(
            IvyAbstractSyntaxTreeTypes.IF_STMT,
            ifTokenId,
            condNode,
            thenBodyNode
        );

        ast.ifElseBody[ifNode] = elseBodyNode;

        return ifNode;
    }
    private int parseForStatementsPratt() {
        int forTokenId = cursor;
        cursor++;


        expect(IvyTokens.LPAREN, "Expected '(' after 'for'");
        int initNode = parseVarDecl(0);

        int condition = parseExpression(0);

        expect(IvyTokens.SEMICOLON, "Expected ';' after for condition");
        int update = parseExpression(0);

        expect(IvyTokens.RPAREN, "Expected ')' after conditionals");
        int forBody = parseIndentBlock();

        int forNode = ast.allocateASTNode(
            IvyAbstractSyntaxTreeTypes.FOR_STMT,
            forTokenId,
            initNode,
            forBody
        );

        ast.conditionNode[forNode] = condition;
        ast.updateNode[forNode] = update;
        ast.forBody[forNode] = forBody;

        return forNode;
    }

    private int parseWhileStatementsPratt() {
        int whileTokenId = cursor;
        cursor++;

        expect(IvyTokens.LPAREN, "Expected '(' after 'while'");
        int condNode = parseExpression(0);
        expect(IvyTokens.RPAREN, "Expected ')' after conditionals");

        int thenBodyNode = parseIndentBlock();

        return ast.allocateASTNode(
            IvyAbstractSyntaxTreeTypes.WHILE_STMT,
            whileTokenId,
            condNode,
            thenBodyNode
        );
    }

    private int parseIndentBlock() {
        int braceTokenId = cursor;

        expect(IvyTokens.LBRACE, "Expected '{' to start block");
        int firstStatement = parseBlockBody(IvyTokens.RBRACE);

        expect(IvyTokens.RBRACE, "Expected '}' at the end of the block");
        return ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.BLOCK, braceTokenId, firstStatement, -1);
    }

    public int parseExpression(int precedent) {
        int tokenId = cursor;
        if (peekTokenId() == IvyTokens.EOF) return -1;

        int tokenType = peekTokenId();
        cursor++;

        int leftNode = parsePrimary(tokenType, tokenId);
        if (leftNode == -1) return -1;

        while (precedent < getBindingStrength(peekTokenId())) {
            int opTokenId = cursor;
            int opType = peekTokenId();
            cursor++;
            int opPrecedent = getBindingStrength(opType);

            if (opType == IvyTokens.DOT) {
                int fieldNameId = cursor;
                expect(IvyTokens.IDENTIFIER, "Expected field or method name after '.'");
                int memberIdentNode = ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.IDENTIFIER, fieldNameId, -1, -1);

                if (match(IvyTokens.LPAREN)) {
                    int argHead = parseArgumentList();
                    expect(IvyTokens.RPAREN, "Expected ')' after arguments");
                    int calleeNode = ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.MEMBER_ACCESS, fieldNameId, leftNode, memberIdentNode);
                    leftNode = ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.CALL, fieldNameId, calleeNode, argHead);
                } else {
                    leftNode = ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.FIELD_ACCESS, fieldNameId, leftNode, memberIdentNode);
                }
                continue;
            }

            int nextPrecedent = (opType == IvyTokens.ASSIGN) ? opPrecedent : opPrecedent + 1;
            int rightNode = parseExpression(nextPrecedent);
            leftNode = ast.allocateASTNode(mapOperationsToAnAbstractSyntaxTreeType(opType), opTokenId, leftNode, rightNode);
        }

        return leftNode;
    }

    private int parsePrimary(int tokenType, int tokenId) {
        if (tokenType == IvyTokens.SUBTRACTION || tokenType == IvyTokens.EXCLAMATION) {
            int targetNode = parseExpression(30);
            int astType = (tokenType == IvyTokens.SUBTRACTION)
                ? IvyAbstractSyntaxTreeTypes.UNARY_MINUS
                : IvyAbstractSyntaxTreeTypes.LOGICAL_NOT;

            return ast.allocateASTNode(astType, tokenId, targetNode, -1);
        }
        if (tokenType == IvyTokens.AMPERSAND) {
            int targetNode = parseExpression(30);
            return ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.ADDRESS_OF, tokenId, targetNode, -1);
        }

        if (tokenType == IvyTokens.ASTERISK) {
            int targetNode = parseExpression(30);
            return ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.DEREF, tokenId, targetNode, -1);
        }

        if (tokenType == IvyTokens.NUMBER || tokenType == IvyTokens.KEYWORD_STRING ||
            tokenType == IvyTokens.LITERAL_TRUE || tokenType == IvyTokens.LITERAL_FALSE) {
            return ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.LITERAL, tokenId, -1, -1);
        }

        if (tokenType == IvyTokens.IDENTIFIER) {
            if (peekTokenId() == IvyTokens.LPAREN) {
                cursor++; // consume LPAREN
                int argHead = parseArgumentList();
                expect(IvyTokens.RPAREN, "Expected ')' after arguments");

                int calleeNode = ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.IDENTIFIER, tokenId, -1, -1);
                return ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.CALL, tokenId, calleeNode, argHead);
            }

            return ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.IDENTIFIER, tokenId, -1, -1);
        }

        if (tokenType == IvyTokens.LPAREN) {
            int expression = parseExpression(0);
            expect(IvyTokens.RPAREN, "Expected ')'");
            return expression;
        }

        return -1;
    }

    private int parseArgumentList() {
        int headArg = -1;
        int lastArg = -1;

        if (peekTokenId() != IvyTokens.RPAREN) {
            do {
                int argNode = parseExpression(0);
                if (headArg == -1) {
                    headArg = argNode;
                } else {
                    ast.rightOrNext[lastArg] = argNode;
                }
                lastArg = argNode;
            } while (match(IvyTokens.COMMA));
        }
        return headArg;
    }

    private boolean isAFunctionDeclaration() {
        if (peekTokenId() != IvyTokens.IDENTIFIER) return false;

        int lookahead = cursor + 1;
        if (tokenStream.getTokenId(lookahead) != IvyTokens.LPAREN) return false;

        lookahead++;
        int parenDepth = 1;

        while (parenDepth > 0) {
            int tok = tokenStream.getTokenId(lookahead);
            if (tok == IvyTokens.EOF || tok == IvyTokens.SEMICOLON) return false;
            if (tok == IvyTokens.LPAREN) parenDepth++;
            else if (tok == IvyTokens.RPAREN) parenDepth--;
            lookahead++;
        }

        return tokenStream.getTokenId(lookahead) == IvyTokens.LBRACE;
    }

    private void skipTypeTokensWithNoAllocationsPratt() {
        if (isTypeToken(peekTokenId())) {
            cursor++;
            while (peekTokenId() == IvyTokens.DOT) {
                cursor++;
                if (peekTokenId() == IvyTokens.IDENTIFIER) {
                    cursor++;
                }
            }
            while (peekTokenId() == IvyTokens.ASTERISK) {
                cursor++;
            }
        }
    }

    private boolean isAnIdentifierForVarDeclaration() {
        if (peekTokenId() != IvyTokens.IDENTIFIER) return false;
        int lookahead = cursor + 1;

        while (tokenStream.getTokenId(lookahead) == IvyTokens.DOT) {
            lookahead++;
            int currentToken = tokenStream.getTokenId(lookahead);
            if (currentToken != IvyTokens.IDENTIFIER) {
                return false;
            }
            lookahead++;
        }

        while (tokenStream.getTokenId(lookahead) == IvyTokens.ASTERISK) {
            lookahead++;
        }

        return tokenStream.getTokenId(lookahead) == IvyTokens.IDENTIFIER;
    }

    private int parseModifiers() {
        int mask = 0;
        while (true) {
            int tok = peekTokenId();
            if (tok == IvyTokens.MODFR_PUBLIC) {
                mask |= 0x01;
                cursor++;
            } else if (tok == IvyTokens.MODFR_PRIVATE) {
                mask |= 0x02;
                cursor++;
            } else if (tok == IvyTokens.MODFR_PROTECTED) {
                mask |= 0x04;
                cursor++;
            } else if (tok == IvyTokens.MODFR_STATIC) {
                mask |= 0x08;
                cursor++;
            } else {
                break;
            }
        }
        if (peekTokenId() == IvyTokens.COLON) {
            cursor++;
        }

        return mask;
    }

    private boolean isTheAnnotationStartPratt() {
        return peekTokenId() == IvyTokens.ANNOTATION || peekTokenId() == IvyTokens.AT;
    }

    private int parseAnnotationsPratt() {
        int head = -1;
        int last = -1;

        while (peekTokenId() == IvyTokens.BAR || isTheAnnotationStartPratt()) {
            if (peekTokenId() == IvyTokens.BAR) {
                cursor++;
            }

            if (!isTheAnnotationStartPratt()) {
                break;
            }

            int annTokenId = cursor;
            cursor++;

            int node = ast.allocateASTNode(IvyAbstractSyntaxTreeTypes.ANNOTATION, annTokenId, -1, -1);
            if (head == -1) {
                head = node;
            } else {
                ast.rightOrNext[last] = node;
            }
            last = node;
        }
        return head;
    }

    private boolean isExplicitType(int type) {
        return (type >= IvyTokens.KEYWORD_INT8 && type <= IvyTokens.KEYWORD_INT64) ||
            type == IvyTokens.KEYWORD_STRING || type == IvyTokens.KEYWORD_BYTE ||
            type == IvyTokens.KEYWORD_UBYTE || type == IvyTokens.KEYWORD_VOID;
    }

    private boolean isTypeToken(int type) {
        return isExplicitType(type) || type == IvyTokens.IDENTIFIER;
    }

    private int getBindingStrength(int tokenType) {
        return switch (tokenType) {
            case IvyTokens.DOT -> 40;
            case IvyTokens.ASTERISK, IvyTokens.DIVISION -> 20;
            case IvyTokens.PLUS, IvyTokens.SUBTRACTION -> 10;
            case IvyTokens.EQUAL -> 6;
            case IvyTokens.ASSIGN -> 1;
            default -> 0;
        };
    }

    private int mapOperationsToAnAbstractSyntaxTreeType(int tokenType) {
        return switch (tokenType) {
            case IvyTokens.PLUS -> IvyAbstractSyntaxTreeTypes.ADD;
            case IvyTokens.SUBTRACTION -> IvyAbstractSyntaxTreeTypes.SUBT;
            case IvyTokens.ASTERISK -> IvyAbstractSyntaxTreeTypes.MULT;
            case IvyTokens.DIVISION -> IvyAbstractSyntaxTreeTypes.DIV;
            case IvyTokens.ASSIGN -> IvyAbstractSyntaxTreeTypes.ASSIGN;
            default -> IvyAbstractSyntaxTreeTypes.UNKNOWN;
        };
    }
}
