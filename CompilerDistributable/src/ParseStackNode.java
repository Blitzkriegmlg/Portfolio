
/**
 * Parent class for nodes that will be placed on the parse stack.
 * This class holds three static methods to deal with setting up
 * parse stack nodes with proper names and numbers.
 * 
 * @author Matthew Benson
 *
 */
public class ParseStackNode {

	protected String name;
	protected Object value;
	protected int number;
	protected static AVLTree keywordTree;
	
	/**
	 * Creates a new ParseStackNode with an Object value
	 * passed in as a parameter. The name is also initially
	 * the string representation of the value passed in.
	 * 
	 * @param value	The Object to be held in value.
	 */
	public ParseStackNode(Object value)
	{
		name = value.toString();
		this.value = value;
	}
	
	public ParseStackNode() { }
	
	/**
	 * Setter method for value.
	 * 
	 * @param value	The object to be stored in value.
	 */
	public void setValue(Object value)
	{
		this.value = value;
	}
	
	/**
	 * Avoid using this method, favor .value over
	 * the method, as it will be called a lot and
	 * we do not want a lot of method call overhead.
	 * 
	 * @return	The Object held by value.
	 */
	public Object getValue()
	{
		return value;
	}
	
	/**
	 * Uses the name of the token, which is given by the scanner to find
	 * the number in the parse table that represents the token. The method
	 * goes as follows: 
	 * 
	 * 1)	check if the token is a reserved keyword by looking in the keyword tree.
	 * 2)	check if the token is numeric.
	 * 3)	check if the token is a variable. (ID token)
	 * 4)	check if the token is a character.
	 * 
	 * @return	The number that the parser recognizes the token by.
	 */
	public int getNumber()
	{
		//if the keywordTree hasn't been populated, populate the tree
		if(keywordTree == null) ParseStackNode.populateTree();
		
		if((number = keywordTree.search(name)) != -1) { //if it is found
			//if the token name is in our list return its number
			return number;
		} else if(isNumeric(name)) {
			return 20;//return 20 for num
			
		} else if(name.toUpperCase().equals(name) && name.substring(0,1).matches("\\w")) {
			//if the name is all uppercase and starts with a letter or _
			return 3;//return 3 for ID
			
		} else if(name.startsWith("'") && name.endsWith("'") && name.length() == 3) { 
			//we have a literal character
			return 34;//return 34 for literal
			
		} else {
			return 99;//our error case
		}

	}//end method
	
	/**
	 * The getNameFromNonTerminal uses the case number to give a proper name to the node.
	 * 
	 * @param name	The current name as int of the node.
	 * @return		The String representation of that case.
	 */
	public static String getNameFromNonTerminal(int name)
	{
		switch (name) {
			
			case 100: return "start";
		
			case 101: return "access";
		
			case 102: return "identifier_list";
		
			case 103: return "declarations";
		
			case 104: return "type";
		
			case 105: return "compound_statement";
		
			case 106: return "statement_list";
			
			case 107: return "statement";
			
			case 108: return "lefthandside";
			
			case 109: return "righthandside";
			
			case 110: return "expression";
			
			case 111: return "simple_expression";
			
			case 112: return "term";
			
			case 113: return "factor";
			
			case 114: return "relop";
			
			case 115: return "addop";
			
			case 116: return "mulop";
		}
		
		return String.valueOf(name);//hopefully we never reach this
	}//end method
	
	/**
	 * Uses a matching regular expression to find whether a string
	 * represents a number.
	 * 
	 * @param s	The string to be tested.
	 * @return	returns true if String s is numeric, otherwise returns false.
	 */
	public static boolean isNumeric(String s)
	{
		return s.matches("\\d*\\.?\\d*");
	}
	
	/**
	 * This method runs the first time a number from the keywordTree is searched.
	 * Putting these values into a tree allows us to search for them quickly, which
	 * is important as they will be searched for a lot when compiling.
	 */
	private static void populateTree()
	{
		keywordTree = new AVLTree();
		keywordTree.insert("static", 1);
		keywordTree.insert("void", 2);
		//space left for any IDs getting production 3
		keywordTree.insert("public", 4);
		keywordTree.insert("private", 5);
		keywordTree.insert("var", 6);
		keywordTree.insert(":", 7);
		keywordTree.insert(";", 8);
		keywordTree.insert("{", 9);
		keywordTree.insert("}", 10);
		keywordTree.insert("(", 11);
		keywordTree.insert(")", 12);
		keywordTree.insert("$", 13);
		keywordTree.insert("char", 14);
		keywordTree.insert("int", 15);
		keywordTree.insert("get", 16);
		keywordTree.insert("put", 17);
		keywordTree.insert("if", 18);
		keywordTree.insert("=", 19);
		//20 is reserved, as it covers actual numbers (ex. 2, 3, 584)
		keywordTree.insert("true", 21);
		keywordTree.insert("false", 22);
		keywordTree.insert(">", 23);
		keywordTree.insert(">=", 24);
		keywordTree.insert("==", 25);
		keywordTree.insert("<=", 26);
		keywordTree.insert("<", 27);
		keywordTree.insert("<>", 28);
		keywordTree.insert("+", 29);
		keywordTree.insert("-", 30);
		keywordTree.insert("*", 31);
		keywordTree.insert("/", 32);
		keywordTree.insert("%", 33);
		//any literals should get 34
		keywordTree.insert("$end", 35);
		keywordTree.insert(",", 36);
		keywordTree.insert("'", 37);
		
		//non-terminals
		keywordTree.insert("start", 100);
		keywordTree.insert("access", 101);
		keywordTree.insert("identifier_list", 102);
		keywordTree.insert("declarations", 103);
		keywordTree.insert("type", 104);
		keywordTree.insert("compound_statement", 105);
		keywordTree.insert("statement_list", 106);
		keywordTree.insert("statement", 107);
		keywordTree.insert("lefthandside", 108);
		keywordTree.insert("righthandside", 109);
		keywordTree.insert("expression", 110);
		keywordTree.insert("simple_expression", 111);
		keywordTree.insert("term", 112);
		keywordTree.insert("factor", 113);
		keywordTree.insert("relop", 114);
		keywordTree.insert("addop", 115);
		keywordTree.insert("mulop", 116);
	}
	
	/**
	 * Returns the value of the ParseStackNode as a string. The 
	 * formatting is left up to the type of the object held by
	 * value.
	 */
	public String toString()
	{
		return value.toString();
	}
}
