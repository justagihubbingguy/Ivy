package ivy.compiler;

public class IvyAbstractSyntaxTree {

    private int capacity;
    public int nodeCount = 0;

    public int[] nodeType;
    public int[] tokenReference;
    public int[] leftOrChild;
    public int[] rightOrNext;
    public int[] modifiers;
    public int[] functionReturnType;
    public int[] functionParameters;
    public int[] functionBody;
    public int[] ifElseBody;
    public int[] initNode;
    public int[] updateNode;
    public int[] conditionNode;
    public int[] forBody;

    public IvyAbstractSyntaxTree(int originalCapacity) {
        this.capacity       = originalCapacity;
        this.nodeType       = new int[originalCapacity];
        this.tokenReference = new int[originalCapacity];
        this.leftOrChild    = new int[originalCapacity];
        this.rightOrNext    = new int[originalCapacity];
        this.modifiers      = new int[originalCapacity];

        this.functionReturnType = new int[originalCapacity];
        this.functionParameters = new int[originalCapacity];
        this.functionBody       = new int[originalCapacity];
        this.initNode            = new int[originalCapacity];
        this.updateNode         = new int[originalCapacity];
        this.conditionNode            = new int[originalCapacity];
        this.forBody            = new int[originalCapacity];
        this.ifElseBody = new int[capacity];

        java.util.Arrays.fill(ifElseBody, -1);
        java.util.Arrays.fill(this.leftOrChild, -1);
        java.util.Arrays.fill(this.rightOrNext, -1);
        java.util.Arrays.fill(this.functionReturnType, -1);
        java.util.Arrays.fill(this.functionParameters, -1);
        java.util.Arrays.fill(this.functionBody, -1);
        java.util.Arrays.fill(this.initNode, -1);
        java.util.Arrays.fill(this.conditionNode, -1);
        java.util.Arrays.fill(this.updateNode, -1);
        java.util.Arrays.fill(this.forBody, -1);
    }

    public int allocateASTNode(int type, int tokenIndex, int left, int right) {
        if (nodeCount >= capacity) grow();
        int id = nodeCount++;

        nodeType[id] = type;
        tokenReference[id] = tokenIndex;

        leftOrChild[id] = left;

        rightOrNext[id] = right;
        modifiers[id] = 0;

        functionReturnType[id] = -1;
        functionParameters[id] = -1;
        functionBody[id] = -1;
        initNode[id] = -1;
        conditionNode[id] = -1;
        updateNode[id] = -1;
        forBody[id] = -1;

        return id; // we return the index as the id
    }

    private void grow() {
        capacity *= 2;

        // fast zero-overhead array copy

        nodeType       = java.util.Arrays.copyOf(nodeType, capacity);
        tokenReference = java.util.Arrays.copyOf(tokenReference, capacity);
        leftOrChild    = java.util.Arrays.copyOf(leftOrChild, capacity);
        rightOrNext    = java.util.Arrays.copyOf(rightOrNext, capacity);
        modifiers      = java.util.Arrays.copyOf(modifiers, capacity);

        functionReturnType = java.util.Arrays.copyOf(functionReturnType, capacity);
        functionParameters = java.util.Arrays.copyOf(functionParameters, capacity);
        functionBody       = java.util.Arrays.copyOf(functionBody, capacity);

        initNode           = java.util.Arrays.copyOf(initNode, capacity);
        conditionNode      = java.util.Arrays.copyOf(conditionNode, capacity);
        updateNode         = java.util.Arrays.copyOf(updateNode, capacity);
        forBody            = java.util.Arrays.copyOf(forBody, capacity);
        ifElseBody = java.util.Arrays.copyOf(ifElseBody, capacity);
    }
}
