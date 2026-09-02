package ivy.compiler;


import ivy.compiler.exception.IvySemanticException;

/**
 *
    Ivy Semantic Analyzer
 *
 **/

public class IvySemanticAnalyzer {

    /// oh my god this took long
    /// 1000+ lines.... ivy --psn-drop-in
    /// linx
    /// toxc --ivcc

    /**
     *  The underlying source code for the semantic analyzer of the Ivy programming language
     */

    private static final int MAX_SCOPES = 256;

    private static final int MAX_SYMBOLS = 4096;

    private final int[] scopeStack = new int[MAX_SCOPES];
    private int scopeDepth = 0;

    private final String fileName;

    private final int[] symbolTokenRef = new int[MAX_SYMBOLS];
    private final int[] symbolType = new int[MAX_SYMBOLS];

    private final int[] symbolScope = new int[MAX_SYMBOLS];

    private int symbolCount = 0;

    private final int[] functionTokenRef;
    private final int[] functionReturnTypes;

    private final int[] functionParameterHeads;

    private int functionCount = 0;

    private boolean insideFunction = false;

    private final int[] nodeResolvedTypes;
    private final int[] nodeResolvedFunctions;
    private final int[] functionNodeIds;

    private int currentFunctionReturnType =
        IvyAbstractSyntaxTreeTypes.UNKNOWN;

    public IvySemanticAnalyzer(String fileName, int astCapacity) {
        this.fileName = fileName;
        this.nodeResolvedTypes = new int[astCapacity];
        this.functionTokenRef = new int[astCapacity];

        this.functionReturnTypes = new int[astCapacity];
        this.functionParameterHeads = new int[astCapacity];
        this.nodeResolvedFunctions = new int[astCapacity];
        this.functionNodeIds = new int[astCapacity];
    }

    private IvySemanticException error(
        String message,
        int tokenRef,
        IvyTokenStream tokenStream
    ) {
        return new IvySemanticException(
            fileName,
            tokenStream.getLine(tokenRef),
            tokenStream.getColumn(tokenRef),
            message
        );
    }

    public boolean analyze(
        int rootAstIndex,
        int[] nodeTypes,

        int[] leftOrChild,
        int[] rightOrNext,
        int[] initNode,

        int[] tokenReferences,

        IvyTokenStream tokenStream,
        IvySourceStream sourceStream,

        int[] functionReturnType,

        int[] functionParameters,
        int[] functionBody
    ) {
        insideFunction = false;
        functionCount = 0;
        scopeDepth = 0;
        symbolCount = 0;

        scopeStack[0] = 0;
        currentFunctionReturnType =
            IvyAbstractSyntaxTreeTypes.UNKNOWN;

        return analyzeNode(
            rootAstIndex,

            nodeTypes,
            leftOrChild,

            rightOrNext,
            initNode,
            tokenReferences,
            tokenStream,

            sourceStream,
            functionReturnType,
            functionParameters,
            functionBody
        );
    }

    private boolean analyzeNode(

        int nodeIdx,

        int[] nodeTypes,
        int[] leftOrChild,

        int[] rightOrNext,
        int[] initNode,

        int[] tokenReferences,

        IvyTokenStream tokenStream,

        IvySourceStream sourceStream,

        int[] functionReturnType,
        int[] functionParameters,

        int[] functionBody

    ) {
        if (nodeIdx == -1) {
            return true;
        }

        int type = nodeTypes[nodeIdx];

        int tokenRef = tokenReferences[nodeIdx];


        int child = leftOrChild[nodeIdx];

        int next = rightOrNext[nodeIdx];

//        System.out.println(
//            "[SEMANTIC] visiting node " + nodeIdx +
//                " | type=" + type +
//                " | child=" + child +
//                " | next=" + next +
//                " | insideFunction=" + insideFunction
//        );

        switch (type) {


            case IvyAbstractSyntaxTreeTypes.BLOCK: {

                if (!pushScope()) {
                    throw error("Duplicate scope or max scope depth reached", tokenRef, tokenStream);
                }

                boolean valid = analyzeNode(
                    child,

                    nodeTypes,
                    leftOrChild,

                    rightOrNext,
                    initNode,
                    tokenReferences,

                    tokenStream,
                    sourceStream,
                    functionReturnType,

                    functionParameters,

                    functionBody
                );

                popScope();

                if (!valid) {
                    error("Analysis failed within block statement", tokenRef, tokenStream);
                }

                return analyzeNode(
                    next,

                    nodeTypes,
                    leftOrChild,

                    rightOrNext,
                    initNode,
                    tokenReferences,

                    tokenStream,
                    sourceStream,
                    functionReturnType,

                    functionParameters,

                    functionBody
                );
            }

            case IvyAbstractSyntaxTreeTypes.VAR_DEC: {

                int declaredType =
                    resolveTypeFromNode(
                        child,
                        nodeTypes,
                        tokenReferences,
                        tokenStream
                    );

                if (declaredType ==
                    IvyAbstractSyntaxTreeTypes.UNKNOWN) {
                    throw error(
                        "unknown variable type",
                        tokenRef,
                        tokenStream
                    );
                }

                int initializer = initNode[nodeIdx];

                if (initializer != -1) {

                    if (!analyzeNode(
                        initializer,
                        nodeTypes,
                        leftOrChild,
                        rightOrNext,
                        initNode,
                        tokenReferences,
                        tokenStream,
                        sourceStream,
                        functionReturnType,
                        functionParameters,
                        functionBody
                    )) {
                        throw error(
                            "invalid initialization",
                            tokenRef,
                            tokenStream
                        );
                    }

                    int initializerType =
                        nodeResolvedTypes[initializer];

                    if (initializerType != declaredType) {
                        throw error(
                            "type mismatch in initialization",
                            tokenRef,
                            tokenStream
                        );
                    }
                }

                if (!registerSymbol(
                    tokenRef,
                    declaredType,
                    tokenStream,
                    sourceStream
                )) {
                    throw error(
                        "duplicate identifier '"
                            + sourceStream.getTokenText(
                            tokenStream,
                            tokenRef
                        )
                            + "'",
                        tokenRef,
                        tokenStream
                    );
                }

                nodeResolvedTypes[nodeIdx] = declaredType;

                return analyzeNode(
                    next,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    initNode,
                    tokenReferences,
                    tokenStream,
                    sourceStream,
                    functionReturnType,
                    functionParameters,
                    functionBody
                );
            }

            case IvyAbstractSyntaxTreeTypes.PARAM: {

                int declaredType =
                    resolveTypeFromNode(
                        child,
                        nodeTypes,
                        tokenReferences,
                        tokenStream
                    );

                if (declaredType ==
                    IvyAbstractSyntaxTreeTypes.UNKNOWN) {

                    throw error("unknown variable type", tokenRef, tokenStream);
                }

                if (!registerSymbol(
                    tokenRef,
                    declaredType,
                    tokenStream,
                    sourceStream
                )) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                nodeResolvedTypes[nodeIdx] = declaredType;

                return analyzeNode(
                    next,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    initNode,
                    tokenReferences,
                    tokenStream,
                    sourceStream,
                    functionReturnType,
                    functionParameters,
                    functionBody
                );
            }

            case IvyAbstractSyntaxTreeTypes.IDENTIFIER: {

                int resolvedType = lookupSymbol(
                    tokenRef,
                    tokenStream,
                    sourceStream
                );

                if (resolvedType == -1) {
                    throw error("undefined identifier " + "'" + sourceStream.getTokenText(tokenStream, tokenRef) + "'", tokenRef, tokenStream);
                }

                nodeResolvedTypes[nodeIdx] = resolvedType;

                return true;
            }

            case IvyAbstractSyntaxTreeTypes.LITERAL: {

                nodeResolvedTypes[nodeIdx] =
                    IvyTokens.KEYWORD_INT32;

                return analyzeNode(
                    next,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    initNode,
                    tokenReferences,
                    tokenStream,
                    sourceStream,
                    functionReturnType,
                    functionParameters,
                    functionBody
                );
            }



            case IvyAbstractSyntaxTreeTypes.ADD:
            case IvyAbstractSyntaxTreeTypes.SUBT:
            case IvyAbstractSyntaxTreeTypes.MULT:
            case IvyAbstractSyntaxTreeTypes.DIV: {

                int leftOperand = child;
                int rightOperand = next;

                if (leftOperand == -1 ||
                    rightOperand == -1) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                boolean leftValid =
                    analyzeNode(
                        leftOperand,
                        nodeTypes,
                        leftOrChild,
                        rightOrNext,
                        initNode,
                        tokenReferences,
                        tokenStream,
                        sourceStream,
                        functionReturnType,
                        functionParameters,
                        functionBody
                    );

                if (!leftValid) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                boolean rightValid =
                    analyzeNode(
                        rightOperand,
                        nodeTypes,
                        leftOrChild,
                        rightOrNext,
                        initNode,
                        tokenReferences,
                        tokenStream,
                        sourceStream,
                        functionReturnType,
                        functionParameters,
                        functionBody
                    );

                if (!rightValid) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                int leftType =
                    nodeResolvedTypes[leftOperand];

                int rightType =
                    nodeResolvedTypes[rightOperand];

                if (leftType != rightType) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                nodeResolvedTypes[nodeIdx] = leftType;

                return true;
            }



            case IvyAbstractSyntaxTreeTypes.ASSIGN: {

                int target = child;
                int value = next;

                if (target == -1 || value == -1) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                if (!analyzeNode(
                    target,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    initNode,
                    tokenReferences,
                    tokenStream,
                    sourceStream,
                    functionReturnType,
                    functionParameters,
                    functionBody
                )) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                if (!analyzeNode(
                    value,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    initNode,
                    tokenReferences,
                    tokenStream,
                    sourceStream,
                    functionReturnType,
                    functionParameters,
                    functionBody
                )) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                int targetType = nodeResolvedTypes[target];
                int valueType = nodeResolvedTypes[value];

                if (targetType != valueType) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                nodeResolvedTypes[nodeIdx] = targetType;

                return true;
            }



            case IvyAbstractSyntaxTreeTypes.RETURN_STMT: {

                if(!insideFunction) {
                    throw error("return statement must be inside of a function", tokenRef, tokenStream);
                }

//                System.out.println(
//                    "[SEMANTIC] RETURN reached | currentFunctionReturnType = "
//                        + currentFunctionReturnType
//                );

                int expressionNode = child;

                if (expressionNode == -1) {
                    throw error("return statement must have a following expression", tokenRef, tokenStream);
                }

                if (!analyzeNode(
                    expressionNode,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    initNode,
                    tokenReferences,
                    tokenStream,
                    sourceStream,
                    functionReturnType,
                    functionParameters,
                    functionBody
                )) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                int expressionType =
                    nodeResolvedTypes[expressionNode];

//                System.out.println(
//                    "[SEMANTIC] return expression type = "
//                        + expressionType
//                        + " | expected = "
//                        + currentFunctionReturnType
//                );

                if (currentFunctionReturnType ==
                    IvyAbstractSyntaxTreeTypes.UNKNOWN) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                if (expressionType != currentFunctionReturnType) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                return analyzeNode(
                    next,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    initNode,
                    tokenReferences,
                    tokenStream,
                    sourceStream,
                    functionReturnType,
                    functionParameters,
                    functionBody
                );
            }



            case IvyAbstractSyntaxTreeTypes.FUNC_DECL: {

                int returnTypeNode = functionReturnType[nodeIdx];
                int parameterNode = functionParameters[nodeIdx];
                int bodyNode = functionBody[nodeIdx];

                boolean previousInsideFunction = insideFunction;
                insideFunction = true;

                try {
                    int declaredReturnType =
                        resolveTypeFromNode(
                            returnTypeNode,
                            nodeTypes,
                            tokenReferences,
                            tokenStream
                        );

//                    System.out.println(
//                        "[SEMANTIC] function return type = "
//                            + declaredReturnType
//                            + " | return node = "
//                            + returnTypeNode
//                    );

                    if (functionCount >= functionTokenRef.length) {

                        // IVY_TODO: replace with location-aware semantic error
                    }

                    if (lookupFunction(tokenRef, tokenStream, sourceStream) != -1) {

                        // IVY_TODO: replace with location-aware semantic error
                    }

                    functionTokenRef[functionCount] = tokenRef;
                    functionReturnTypes[functionCount] = declaredReturnType;
                    functionParameterHeads[functionCount] = parameterNode;
                    functionNodeIds[functionCount] = nodeIdx;

                    functionCount++;

                    if (declaredReturnType ==
                        IvyAbstractSyntaxTreeTypes.UNKNOWN) {

                        // IVY_TODO: replace with location-aware semantic error
                    }


                    int previousReturnType =
                        currentFunctionReturnType;

                    currentFunctionReturnType =
                        declaredReturnType;

                    if (!pushScope()) {
                        currentFunctionReturnType =
                            previousReturnType;

                        // IVY_TODO: replace with location-aware semantic error
                    }


                    boolean valid =
                        analyzeNode(
                            parameterNode,
                            nodeTypes,
                            leftOrChild,
                            rightOrNext,
                            initNode,
                            tokenReferences,
                            tokenStream,
                            sourceStream,
                            functionReturnType,
                            functionParameters,
                            functionBody
                        );


                    if (valid) {
                        valid =
                            analyzeNode(
                                bodyNode,
                                nodeTypes,
                                leftOrChild,
                                rightOrNext,
                                initNode,
                                tokenReferences,
                                tokenStream,
                                sourceStream,
                                functionReturnType,
                                functionParameters,
                                functionBody
                            );
                    }

                    popScope();

                    currentFunctionReturnType =
                        previousReturnType;

                    if (!valid) {

                        // IVY_IVY_TODO: replace with location-aware semantic error
                    }

                    return analyzeNode(
                        next,
                        nodeTypes,
                        leftOrChild,

                        rightOrNext,
                        initNode,
                        tokenReferences,
                        tokenStream,

                        sourceStream,
                        functionReturnType,
                        functionParameters,
                        functionBody
                    );
                } finally {
                    insideFunction = previousInsideFunction;
                }
            }



            case IvyAbstractSyntaxTreeTypes.CALL: {

                int calleeNode = child;

                if (calleeNode == -1) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                int calleeToken = tokenReferences[calleeNode];

                int functionIndex =
                    lookupFunction(
                        calleeToken,
                        tokenStream,
                        sourceStream
                    );

                if (functionIndex == -1) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                nodeResolvedFunctions[nodeIdx] = functionNodeIds[functionIndex];

                int returnType =
                    functionReturnTypes[functionIndex];

                nodeResolvedTypes[nodeIdx] = returnType;

                return analyzeNode(
                    next,
                    nodeTypes,
                    leftOrChild,

                    rightOrNext,
                    initNode,
                    tokenReferences,
                    tokenStream,

                    sourceStream,
                    functionReturnType,
                    functionParameters,
                    functionBody
                );
            }

            // if defaulted:

            default: {

                boolean leftValid =
                    analyzeNode(
                        child,
                        nodeTypes,
                        leftOrChild,

                        rightOrNext,
                        initNode,
                        tokenReferences,
                        tokenStream,
                        sourceStream,

                        functionReturnType,
                        functionParameters,
                        functionBody
                    );

                if (!leftValid) {

                    // IVY_TODO: replace with location-aware semantic error
                }

                return analyzeNode(
                    next,
                    nodeTypes,
                    leftOrChild,
                    rightOrNext,
                    initNode,
                    tokenReferences,
                    tokenStream,
                    sourceStream,

                    functionReturnType,
                    functionParameters,
                    functionBody
                );
            }
        }
    }



    private int resolveTypeFromNode(
        int nodeIdx,
        int[] nodeTypes,

        int[] tokenReferences,
        IvyTokenStream tokenStream
    ) {
        if (nodeIdx == -1) {
            return IvyAbstractSyntaxTreeTypes.UNKNOWN;
        }

        int nodeType = nodeTypes[nodeIdx];

        if (nodeType ==
            IvyAbstractSyntaxTreeTypes.TYPE_REF) {

            int tokenRef =
                tokenReferences[nodeIdx];

            return tokenStream.getTokenId(tokenRef);
        }

        if (nodeType ==
            IvyAbstractSyntaxTreeTypes.POINTER_TYPE) {

            return IvyAbstractSyntaxTreeTypes.POINTER_TYPE;
        }

        return nodeType;
    }




    private boolean pushScope() {

        if (scopeDepth >= MAX_SCOPES - 1) {

            // IVY_TODO: replace with location-aware semantic error
        }

        scopeDepth++;

        scopeStack[scopeDepth] =
            symbolCount;

        return true;
    }

    private void popScope() {

        if (scopeDepth > 0) {

            symbolCount =
                scopeStack[scopeDepth];

            scopeDepth--;
        }
    }




    private boolean registerSymbol(
        int tokenRef,
        int typeId,

        IvyTokenStream tokenStream,
        IvySourceStream sourceStream
    ) {

        int scopeStart =
            scopeStack[scopeDepth];

        for (int i = scopeStart;
             i < symbolCount;
             i++) {

            if (areTokensEqual(
                symbolTokenRef[i],
                tokenRef,
                tokenStream,
                sourceStream
            )) {

                // IVY_TODO: replace with location-aware semantic error
            }
        }

        if (symbolCount >= MAX_SYMBOLS) {

            // IVY_TODO: replace with location-aware semantic error
        }

        symbolTokenRef[symbolCount] =
            tokenRef;

        symbolType[symbolCount] = typeId;

        symbolScope[symbolCount] = scopeDepth;

        symbolCount++;

        return true;
    }

    /**
     *  This function checks if the symbol given ,
     *  is a registered symbol.
     *  <p>
     *      This function is used to check if the user has typed
     *      a valid symbol in the table,
     *      Example, the user types:
     *      <pre>
     *          {@code
     *          int32 x = 5;
     *          a = 3;
     *          }
     *      </pre>
     *      The semantic analyzer rejects this because 'a' is not registered.
     *  </p>
     * @param tokenRef
        * The token Reference
     * @param tokenStream
        * The token Stream
     * @param sourceStream
        * The Ivy source stream.
     * @return
     * {@code int isLookup}
     */

    private int lookupSymbol(
        int tokenRef,

        IvyTokenStream tokenStream,

        IvySourceStream sourceStream
    ) {

        for (int i = symbolCount - 1;
             i >= 0;
             i--) {

            if (areTokensEqual(
                symbolTokenRef[i],
                tokenRef,

                tokenStream,
                sourceStream
            )) {
                return symbolType[i];
            }
        }

        return -1;
    }

    /**
     *
     * <p>
     *     the areTokensEqual() function checks
     *     if tokenA and tokenB are equal
     * </p>
     *
     * @param tokA
         * tokenA is the first token
     * @param tokB
         * tokenB is the second token
     * @param stream
     * @param source
     * @return boolean areTokensEqual
     */
    private boolean areTokensEqual(

        int tokA,
        int tokB,

        IvyTokenStream stream,
        IvySourceStream source

    ) {
        if (tokA == tokB) {
            return true;
        }

        if (tokA < 0 || tokB < 0 || tokA >= stream.count() || tokB >= stream.count()) {

            // IVY_TODO: replace with location-aware semantic error
        }

        String a = source.getTokenText(stream, tokA);

        String b = source.getTokenText(stream, tokB);

        return a.equals(b);
    }

    /**
     * holy shit this is getting long
     * <p>
     *     isIdentifierBoundary(char c) tells us if its an identifier
     * </p>
     * check if c is an identifier.
     * @param c
     * @return isIdentifier
     */

    private boolean isIdentifierBoundary(char c) {


        return c == '\0'

            || Character.isWhitespace(c)
            || c == ','
            || c == ';'
            || c == '('
            || c == ')'
            || c == '{'

            || c == '}'
            || c == '['
            || c == ']'
            || c == '+'
            || c == '-'
            || c == '*'
            || c == '/'

            || c == '&'
            || c == '|'
            || c == '='
            || c == '!'
            || c == '<'
            || c == '>'
            || c == '.'
            || c == ':';
    }

    /**
     *
     * Lookup Function using symbol lookups.
     *
     */

    private int lookupFunction(
        int tokenRef,
        IvyTokenStream tokenStream,
        IvySourceStream sourceStream
    ) {
        for (int i = functionCount - 1; i >= 0; i--) {
            if (areTokensEqual(
                functionTokenRef[i],
                tokenRef,
                tokenStream,
                sourceStream
            )) {
                return i;
            }
        }

        return -1;
    }

    /**
     *
     * Get The correct Type.
     *
     **/

    public int getResolvedType(int astNodeIdx) {
        if (astNodeIdx < 0 ||
            astNodeIdx >= nodeResolvedTypes.length) {

            return IvyAbstractSyntaxTreeTypes.UNKNOWN;
        }
        return nodeResolvedTypes[astNodeIdx];
    }

    public int[] getNodeResolvedFunctions() {
        return nodeResolvedFunctions;
    }
}
