/**
 * The ErrorMessage class is responsible for outputting any relevant errors
 * when compiling the input file.
 * 
 * @author Matthew Benson
 *
 */
public final class ErrorMessage {
	
	private ErrorMessage()
	{
		//empty constructor - ErrorMessage cannot be instantiated.
	}
	
	public static void fileNotFound(String fileName)
	{
		System.out.println("Unable to open file: " +"\"" + fileName + "\"");
	}
	
	public static void cannotParse()
	{
		System.out.println("Unable to parse input file: make sure"
				   		 + " to include file end character $");
	}
	
	public static void syntaxError()
	{
		System.out.println("Input is rejected - unable to compile while parsing.");
		System.out.println("Check syntax of input and try again.");
	}
	
	public static void reductionError()
	{
		System.out.println("Encountered an error when reducing - could be a type"
						 + " mismatch or another gramatical or logical error");
	}
	
	public static void nameTaken(String name)
	{
		System.out.println("Error: Symbol already in table - " + name);
	}
	
	public static void nameDoesNotExist(String name)
	{
		System.out.println("Error: Name does not exist in symbol table: " + name);
	}
	
	public static void failureAtToken(TokenNode token)
	{
		System.out.println("Failure at token: " + token.getName() + " : " + token.getNumber());
	}
	
	public static void failureToCreateExecutable()
	{
		System.out.println("Failure in mini: unable to create executable file.");
	}
	
	public static void failureToExecute()
	{
		System.out.println("Failure in mice: unable to execute file.");
	}
}
