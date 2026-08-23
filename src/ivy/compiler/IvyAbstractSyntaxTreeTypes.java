package ivy.compiler;

public final class IvyAbstractSyntaxTreeTypes {
    // Primary / Expressions
    public static final int LITERAL       = 1;
    public static final int ADD           = 2;
    public static final int SUBT          = 3;
    public static final int MULT          = 4;
    public static final int DIV           = 5;
    public static final int ADDRESS_OF    = 6;  // &x
    public static final int DEREF         = 7;  // *y
    public static final int CALL          = 8;  // sys.println()
    public static final int FIELD_ACCESS  = 9;  // class.function();

    public static final int SYNC_STMT     = 10; // sync example.com.lib;
    public static final int FUNC_DECL     = 11; // public void main() { ... }
    public static final int PARAM         = 12; // Object object
    public static final int ANNOTATION    = 13; // @Gc.GcManaged
    public static final int POINTER_TYPE  = 14; // string*

    public static final int VAR_DEC       = 15;
    public static final int IF_STMT       = 16;
    public static final int RETURN_STMT   = 17;
    public static final int BLOCK         = 18;
    public static final int STRUCT_DECL   = 19; // struct Point { ... }
    public static final int CLASS_DECL    = 20;// class Node { ... }
    public static final int TYPE_REF      = 21;
    public static final int PATH          = 22;
    public static final int ASSIGN        = 23;
    public static final int IDENTIFIER    = 24;
    public static final int MEMBER_ACCESS = 25;
    public static final int WHILE_STMT    = 26;
    public static final int FOR_STMT      = 27;

    public static final int INDEX_ACCESS  = 28; // arr[i]
    public static final int ARRAY_TYPE    = 29; // int[]

    public static final int UNARY_MINUS   = 30; // -x
    public static final int LOGICAL_NOT   = 31; // !x

    public static final int EQUAL_TO      = 32; // ==
    public static final int NOT_EQUAL     = 33; // !=
    public static final int LESS_THAN     = 34; // <
    public static final int GREATER_THAN  = 35; // >
    public static final int LESS_EQUAL    = 36; // <=
    public static final int GREATER_EQUAL = 37; // >=
    public static final int LOGICAL_AND   = 38; // &&
    public static final int LOGICAL_OR    = 39;

    public static final int UNKNOWN       = 99;
}
