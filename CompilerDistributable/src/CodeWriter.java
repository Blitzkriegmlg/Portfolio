
import java.io.FileWriter;
import java.io.IOException;

/**
 * 
 * @author Matthew Benson
 * 
 * CodeWriter handles all the output of the compiler. This class contains
 * a StringBuilder which is used when building the output to efficiently
 * edit a potentially large String. Once the file is finished being written,
 * the close method should be called, as this method writes the content of
 * the StringBuilder to a new file which is titled [file].asm where [file]
 * is the name passed when creating a new CodeWriter object.
 *
 */
public class CodeWriter {
	
	private StringBuilder stringBuilder;
	private FileWriter writer;
	private int currentLine;
	private static final int INITIAL_CHAR_LENGTH = 1024;
	
	/**
	 * Creates a new CodeWriter object with initial size INITIAL_CHAR_LENGTH.
	 * This creates the StringBuilder which is used to construct the output file
	 * and initializes the FileWriter which takes the built String from the 
	 * StringBuilder and writes it to a file named file.asm where file is
	 * the String parameter of the constructor.
	 * 
	 * @param file	The suffix name of the output file. The file will be named
	 * 				[file].asm
	 * 
	 * @throws IOException
	 */
	public CodeWriter(String file) throws IOException
	{
		stringBuilder = new StringBuilder(INITIAL_CHAR_LENGTH);
		writer = new FileWriter(file + ".asm");
		currentLine = 1;
	}
	
	/**
	 * This writes the line number on the left, a tab, in order to keep
	 * the commands aligned in the output file, the command,
	 * and then a return to move on to the next line. 
	 * Example:
	 * 
	 * 	0  	SYS #1,,0
	 *	1  	STO #2,,1
	 *	2	HLT ,,
	 *
	 * @param text	The command to be written on the line.
	 * @throws IOException
	 */
	public void write(String text) 
	{
		//The space before the tab is necessary - Mini doesn't check for white spaces,
		//it ONLY looks for spaces, so we need the space
		stringBuilder.append(currentLine + " \t" + text + System.lineSeparator());
		currentLine++;
	}
	
	/**
	 * This method has the same functionality as write() but adds an @ symbol to
	 * the end of the line. This will be used to mark the place where the end of
	 * a jump statement should be written. The method replaceMark replaces the @
	 * symbol written with a string, which should be the line number to complete
	 * the jump statement.
	 * 
	 * @param text	The command to be written to the line.
	 */
	public void writeWithMark(String text)
	{
		stringBuilder.append(currentLine + " \t" + text + "@" +System.lineSeparator());
		currentLine++;
	}
	
	/**
	 * This method checks whether there are any instances of marks in the StringBuilder,
	 * and then if any are found, it replaces the last "@" in the string with the param
	 * String text.
	 * 
	 * @param text	The String that replaces the first mark in the string.
	 * @return
	 */
	public boolean replaceMark(String text)
	{
		//we need to first check if the mark location is valid, or we don't remove anything.
		int markLocation = stringBuilder.lastIndexOf("@");
		
		if(!(markLocation == -1))//if there is an instance of "@" (the mark)
		{
			stringBuilder.replace(markLocation, markLocation + 1, text);
			return true;
		}
		return false;
	}
	
	/**
	 * This method checks whether there are any instances of marks in the StringBuilder,
	 * and then if there are any found, it removes the last "@" in the String.
	 * 
	 * @return	Returns true if a mark is removed, false if no mark is found.
	 */
	public boolean removeMark()
	{
		//we need to first check if the mark location is valid, or we don't remove anything.
		int markLocation = stringBuilder.lastIndexOf("@");
		
		if(!(markLocation == -1))//if there is an instance of "@" (the mark)
		{
			stringBuilder.deleteCharAt(stringBuilder.indexOf("@"));
			return true;
		}
		return false;
		
	}
	
	/**
	 * This method calls replaceMark and replaces it with the currentLine - 1.
	 * This means that calling this method after reducing to if(expression) statement
	 * allows us to complete a jump statement with the proper line that will be jumped
	 * to.
	 */
	public void completeJumpStatement()
	{
		replaceMark("#" + String.valueOf(currentLine - 1));
	}
	
	
	/**
	 * This method takes the built string from the StringBuilder and writes
	 * it with the FileWriter writer. After that, it simply closes the
	 * open file so it can be written to a file by the OS. Using FileWriter
	 * flush then close is standard procedure for closing a FileWriter.
	 * @return
	 * @throws IOException
	 */
	public boolean close() throws IOException
	{
		writer.write(stringBuilder.toString());
		writer.flush();
		writer.close();
		return true;
	}
	
}
