import java.util.LinkedList;

/**
 * This SymbolTable will be used for the compiler implementation.
 * It is able to add Symbols to the table, look up Symbols and their
 * respective values, and set values to existing Symbols in the table.
 * It also includes a toString method that allows for easy, clean 
 * data representation.
 * 
 * @author Matthew Benson
 *
 */

public class SymbolTable {

	private LinkedList<Symbol>[] table;
	
	/**
	 * Construct a new SymbolTable which is
	 * an array of size 'size' that is filled
	 * with LinkedList buckets that hold 
	 * Symbols. This open hashing method is
	 * the best in our case. the size of the 
	 * table is more difficult to determine
	 * the most efficient size.
	 * 
	 * @param size		Length of the array held by SymbolTable
	 */
	@SuppressWarnings("unchecked")
	public SymbolTable(int size)
	{
		table = new LinkedList[size];//not sure what to change here
		
		for(int i = 0; i < table.length; i++)
		{
			table[i] = new LinkedList<Symbol>();
		}
	}
	
	/**
	 * We are adding a variable to the Symbol Table but
	 * we do not yet know the value that will be stored
	 * in it. 
	 * 
	 * @param name		The name of the Symbol
	 * @param type		The type of the Symbol
	 * @return			Returns true if added, false if not added.
	 */
	public boolean insert(String name, DataType type)
	{
		if(findSymbol(name) != null)//if the name isn't in the table
			{
				ErrorMessage.nameTaken(name);
				return false;//if the name is already in the table
			}
		int hashIndex = getHashIndex(name);
		
		//add to the bucket
		return table[hashIndex].add(new Symbol(name, type));
	}
	
	/**
	 * When we know the value of the variable we are inserting
	 * we can call this method over the other insert so we
	 * have more information in our table. 
	 * 
	 * @param name		name of the Symbol
	 * @param type		DataType of the Symbol
	 * @param value		value of the Symbol
	 * @return			Returns true if added, false if not added.
	 */
	public boolean insert(String name, DataType type, int value)
	{
		if(findSymbol(name) != null)//if the name isn't in the table
			{
				ErrorMessage.nameTaken(name);
				return false;//if the name is already in the table
			}
		int hashIndex = getHashIndex(name);
				
		return table[hashIndex].add(new Symbol(name, type, value));//add to the bucket
	}
	
	/**
	 * Find whether a named Symbol is in the table.
	 * @param name	name of the Symbol to search for.	
	 * @return		Returns true if found, false if not.
	 */
	public boolean lookup(String name)
	{	
		if(findSymbol(name) != null)
		{
			return true;//The name is in the table
		}
		return false;
	}
	
	/**
	 * Find the type of a Symbol, searching by name.
	 * 
	 * @param name	name of the Symbol to search for.
	 * @return		The DataType of this Symbol, null if not found.
	 */
	public DataType lookupType(String name)
	{	
		if(findSymbol(name) != null)
		{
			//get the type stored with this symbol and return it
			return findSymbol(name).getType();
		}		
		else {
			ErrorMessage.nameDoesNotExist(name);
			return null;
		}
	}
	
	/**
	 * Find the value of a Symbol, searching by name.
	 * 
	 * @param name	name of the Symbol to search for.
	 * @return		The Object value of this Symbol, null if not found.
	 * 				It is possible that the Symbol is found but still
	 * 				null if the variable is not initialized.
	 */
	public int lookupValue(String name)
	{
		if(findSymbol(name) != null)
		{
			//get the value stored with this symbol and return it
			return findSymbol(name).getValue();
		}		
		else {
			ErrorMessage.nameDoesNotExist(name);
			return -1;
		}
	}
	
	/**
	 * Find a Symbol by name and update the value held by it.
	 * 
	 * @param name			Name of Symbol to update.
	 * @param newValue		New Object value to store.
	 * @return				Returns true if successful, and false
	 * 						if the Symbol cannot be found.
	 */
	public boolean changeValue(String name, int newValue)
	{
		if(findSymbol(name) != null)
		{
			findSymbol(name).setValue(newValue);
			return true;
		}
		else {
			ErrorMessage.nameDoesNotExist(name);
			return false;
		}
	}
	
	/**
	 * Find a Symbol by name and update the DataType held by it.
	 * 
	 * @param name			Name of the Symbol to update.
	 * @param newType		New DataType type to store.
	 * @return				Returns true if successful, and false
	 * 						if the symbol cannot be found.
	 */
	public boolean changeType(String name, DataType newType)
	{
		if(findSymbol(name) != null)
		{
			findSymbol(name).setType(newType);
			return true;
		}
		
		else {
			ErrorMessage.nameDoesNotExist(name);
			return false;
		}
	}
	
	/**
	 * This prints out the SymbolTable with each index of the array as a row
	 * and the columns are the data.
	 * 
	 * For example, one potential output for a table set up with the following code:
	 * 
	 * SymbolTable table = new SymbolTable(20);
	 *	
	 *	table.insert("variable", DataType.INT, 5);
	 *	table.insert("datData", DataType.CHAR);
	 *	table.insert("datDota", DataType.INT, 17);
	 *	table.insert("datDota", DataType.INT, 234);
	 *
	 * Looks like this when printing:  
	 * []
	 * []
	 * []
	 * []
	 * []
	 * [datData: CHAR; ]
	 * []
	 * []
	 * []
	 * []
	 * []
	 * []
	 * []
	 * []
	 * []
	 * []
	 * [variable: INT, 5; ]
	 * []
	 * []
	 * [datDota: INT, 17; ]
	 *
	 */
	public String toString()
	{
		StringBuilder output = new StringBuilder();//using StringBuilder for efficiency
		
		for(int i = 0; i < table.length; i++)
		{
			output.append(table[i].toString());
			
			output = output.append("\r\n");
		}//end loop
		
		return output.toString();
	}
	
	private int getHashIndex(String name)
	{
		int hashIndex = name.hashCode();//find index to hash to
		
			while(hashIndex < 0)//if our index is too small, increase it while keeping it in our bounds
			{
				hashIndex += table.length;
			}
		
			while(hashIndex >= table.length)//prevent hashIndex out of bounds error
			{
				hashIndex = hashIndex - table.length;
			}	
			
			return hashIndex;//this is an appropriate unique value within bounds of our array
	}
	
	private Symbol findSymbol(String name)
	{
		int hashIndex = getHashIndex(name);// the index where this particular Symbol is to be stored
		
		LinkedList<Symbol> list = table[hashIndex];//create a LinkedList so accessing it will be easier later
		
		int listSize = list.size();//this line means we are reducing a reference inside our loop 
		//we could be using list.size() in our list but this is more efficient.
		
		for(int i = 0; i < listSize; i++)
		{
			if(list.get(i).getName().equals(name))//we have found a match
			{
				return list.get(i);//return the Symbol
			}
		}
		
		return null;//if we didn't find a match, return null
	}
	
}//end of class

