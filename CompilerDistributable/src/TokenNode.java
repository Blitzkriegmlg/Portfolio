
/**
 * The TokenNode class holds tokens that go on the parse stack.
 * This class has setter methods for the variables, but to access
 * the fields of this class, it's best to use the . accessor since
 * they will be called on many times in the parser and we don't 
 * want a lot of method call overhead.
 * 
 * @author Matthew Benson
 *
 */
public class TokenNode extends ParseStackNode {

	private DataType type;
	
	/**
	 * Creates a new TokenNode and initializes the
	 * type of the TokenNode from the string passed
	 * in.
	 * 
	 * @param title	The string that be evaluated for
	 * 				a DataType
	 */
	public TokenNode(String title)
	{
		super(title);
		if(name.equals("int"))
		{
			type = DataType.INT;
		}
		if(name.equals("char"))
		{
			type = DataType.CHAR;
		}
	}
	
	/**
	 * Creates a new TokenNode and calls ParseStackNode
	 * with the value passed as a parameter. This sets
	 * the name as the String representation of the
	 * value and sets value to the parameter as well.
	 * 
	 * @param value	The Object used to create the Token's
	 * 				value and name.
	 */
	public TokenNode(Object value)
	{
		super(value);
	}
	
	/**
	 * Creates a new TokenNode with name equal to title, and
	 * type and value equal to their equivalent parameter.
	 * 
	 * @param title		Name of the token - what we read from the scanner
	 * @param type		Data type of this token - int/char
	 * @param value		Value held by this token
	 */
	public TokenNode(String title, DataType type, Object value)
	{
		super();//do nothing
		name = title;
		this.value = value;
		this.type = type;
	}
	
	/**
	 * Setter method for name.
	 * 
	 * @param title	The new name of this TokenNode.
	 */
	public void setName(String title)
	{
		name = title;
	}
	
	/**
	 * Accessor method for name. It's generally better
	 * to use the . accessor for name rather than this method.
	 * 
	 * @return	returns the name of the TokenNode as String.
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Setter method for the type of this TokenNode.
	 * 
	 * @param type	The new DataType for the TokenNode.
	 */
	public void setType(DataType type)
	{
		this.type = type;
	}
	
	/**
	 * Accessor method for type. It's generally better
	 * to use the . accessor to get the type.
	 * 
	 * @return	The DataType type of this TokenNode.
	 */
	public DataType getType()
	{
		if(getNumber() != 3 & type == null) {//if we have a non-terminal without a type
			type = DataType.TERMINAL;	
		}
		
		//If number is 3, then we have a symbol. This symbol should have a type,
		//and should be delt with in the syntax tree.
		
		return type;
	}

	/**
	 * Returns the name as String.
	 */
	public String toString()
	{
		return name;
	}
	
}//end class
