package ivy.compiler;

public final class IvyTokens {

    /*

        just to clarify, you can use the types alone like (uint, uint32,int,int32, .etc.).
        you can also use the "unsigned" modifier to turn existing signed variables into unsigned, like "unsigned char".

        The - 𐌉 ᕓ 𐌙 - Programming Language.

        Project (- 𐌉-ᕓ-𐌙 -)

     */


    public static final int EOF             =  0;

    // Integers
    public static final int KEYWORD_INT8    =  1;
    public static final int KEYWORD_INT16   =  2; // can be "short" alone instead
    public static final int KEYWORD_INT32   =  3; // can be "int" alone instead
    public static final int KEYWORD_INT64   =  4; // can be "long" alone instead

    // Unsigned Integers
    public static final int KEYWORD_UINT16  =  5;
    public static final int KEYWORD_UINT32  =  6; // can be "uint" alone instead
    public static final int KEYWORD_UINT64  =  7; // can be "ulong" alone instead

    // Floating-Point Decimals
    public static final int KEYWORD_FLOAT8  =  8;
    public static final int KEYWORD_FLOAT16 =  9; // can be "half" alone instead
    public static final int KEYWORD_FLOAT32 = 10; // can be "float" alone instead
    public static final int KEYWORD_FLOAT64 = 11; // can be "double" alone instead

    // Characters
    public static final int KEYWORD_CHAR8   = 12; // can be "char" alone instead
    public static final int KEYWORD_CHAR16  = 13;
    public static final int KEYWORD_CHAR32  = 14;
    public static final int KEYWORD_CHAR64  = 15;

    // Unsigned Characters
    public static final int KEYWORD_UCHAR8  = 16; // can be "uchar" alone instead
    public static final int KEYWORD_UCHAR16 = 17;
    public static final int KEYWORD_UCHAR32 = 18;
    public static final int KEYWORD_UCHAR64 = 19;

    // User-Defined
    public static final int IDENTIFIER      = 20; // the integer / function name that the user defines
    public static final int NUMBER          = 21; // 1.3 , 2.6, 5, .etc
    public static final int ASSIGN          = 22;

    // Operators / Semi-Operators
    public static final int ASTERISK        = 23; // pointer "*", also used as a multiplication operator
    public static final int AMPERSAND       = 24; // reference "&", also used as "&&" which corresponds to the "AND" operator
    public static final int SEMICOLON       = 25; // typed at the end of a line, also typed between conditions in "for" and "while" loops
    public static final int SUBTRACTION     = 27;
    public static final int DIVISION        = 28;

    // Nullability Operators
    public static final int NULL            = 29; // nullability identifier
    public static final int AWAKE           = 30; // instance is alive and awake, opposite of null

    // Modifiers
    public static final int MODFR_UNSIGNED  = 31; // the "unsigned" operator, placed before the variable.
    public static final int MODFR_PRIVATE   = 32;
    public static final int MODFR_PUBLIC    = 33;
    public static final int MODFR_PROTECTED = 34;
    public static final int MODFR_STATIC    = 35;
    public static final int MODFR_FINAL     = 36;

    // Logical Operators
    public static final int BAR             = 37; // "|"
    public static final int LOGICAL_AND     = 38; // "&&"
    public static final int LOGICAL_OR      = 39; // "||"

    // Structural Symbols
    public static final int COMMA           = 40; // ",", used between parameters
    public static final int LPAREN          = 41;
    public static final int RPAREN          = 42;
    public static final int LBRACE          = 43;
    public static final int RBRACE          = 44;
    public static final int SINGLE_QOUTE    = 45;
    public static final int L_BRACKET       = 46;
    public static final int EQUAL           = 47;
    public static final int PERCENT         = 48;
    public static final int DOUBLE_QOUTE    = 49;
    public static final int RBRACKET        = 50;
    public static final int DOT             = 51; // "." for method/field access
    public static final int COLON           = 52; // ":" for switch/ternary
    public static final int EXCLAMATION     = 53; // "!" for not equal or not

    // Flow Keywords
    public static final int KEYWORD_IF      = 54;
    public static final int KEYWORD_ELSE    = 55;
    public static final int KEYWORD_FOR     = 56;
    public static final int KEYWORD_WHILE   = 57;
    public static final int KEYWORD_RETURN  = 58;

    // OOP Keywords
    public static final int KEYWORD_CLASS   = 59;
    public static final int KEYWORD_NEW     = 60;
    public static final int KEYWORD_THIS    = 61;
    public static final int KEYWORD_VOID    = 62; // For no return functions.

    // Boolean States
    public static final int LITERAL_TRUE    = 63;
    public static final int LITERAL_FALSE   = 64;

    public static final int KEYWORD_STRUCT  = 65; // "struct" keyword

    public static final int KEYWORD_BYTE    = 66; // "byte" alone which is signed 8-bit
    public static final int KEYWORD_UBYTE   = 67; // "ubyte" alone which is unsigned 8-bit

    public static final int KEYWORD_MEMALLOC= 68; // "memalloc" function used to allocate memory
    public static final int KEYWORD_MEMFREE = 69; // "memfree" function used to de-allocate memory
    public static final int KEYWORD_STRING  = 70; // just string...
    public static final int KEYWORD_SYNC    = 71;
    public static final int PLUS            = 72;
    public static final int ANNOTATION      = 73;
    public static final int AT              = 74;
    public static final int KEYWORD_UINT8   = 75;
    // the rest of the operators
    public static final int NOT_EQUAL       = 76; // "!="
    public static final int LESS_THAN       = 77; // "<"
    public static final int GREATER_THAN    = 78; // ">"
    public static final int LESS_EQUAL      = 79; // "<="
    public static final int GREATER_EQUAL   = 80; // ">="

    public static final int INCREMENT       = 81; // "++"
    public static final int DECREMENT       = 82; // "--"

    public static final int PLUS_ASSIGN     = 83; // "+="
    public static final int MINUS_ASSIGN    = 84; // "-="
    public static final int ASTERISK_ASSIGN = 85; // "*="
    public static final int SLASH_ASSIGN    = 86; // "/="
    public static final int PERCENT_ASSIGN  = 87; // "%="

    public static final int BITWISE_XOR     = 88; // "^"
}
