
public class AVLNode {

    AVLNode left, right;
    String keyword;
    int productionNumber;
    int height;

    /* Constructor */
    public AVLNode()
    {
        left = null;
        right = null;
        //data = 0;
        height = 0;
    }
    /* Constructor */
    public AVLNode(String n, int prod)
    {
        left = null;
        right = null;
        keyword = n;
        productionNumber = prod;
        height = 0;
    } 
	
}
