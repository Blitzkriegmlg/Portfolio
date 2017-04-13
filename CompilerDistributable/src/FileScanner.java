import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Hashtable;

/**
 * The FileScanner class is responsible for getting all the tokens from the input
 * file and returning them to the parser with the getNextToken() method.
 * 
 * @author Matthew Benson
 *
 */
public class FileScanner {

	private BufferedReader fileReader;	//Using BufferedReader to access the file we are reading.
	private String fileName;			//The file we are scanning.
	private String line;				//The current line we are reading from.
	private String currentLexeme;		//A substring of line which we use to build a 
										//lexeme until we find a separator character.
	private String currentSeparator;	//Temporarily holds separator characters so they can be
										//grouped more easily.
	private char currentChar;			//keep track of the current character being processed
	private String checkDoubleSeparator;//Temporary variable for testing whether the separator is part of 
										//a larger separator such as >= or \\
	private StringBuilder scannedInput;	//Build the string to be eventually given to the parser
										//in a StringReader using this StringBuilder
	private BufferedReader scannerOutput;//This reads the output created by scanFile
	private Hashtable<Integer, String> separatorLookupTable;//The lookup table used to hold 
										//separators so we can search for them quickly.
	private int singleQuoteFlag = 0;	//If we see quotes around a character, keep track to make better tokens
	private String newLine = System.getProperty("line.separator");//This saves us from repeatedly making system calls
	
	/*
	 * This list of separatorsis used to tokenize any strings that are separated by any of these
	 * characters. The inclusion of symbols like <= and // means that we need to handle double 
	 * characters, which is why these are stored as Strings and not characters.
	 */
	private String[] separators = {"(", ")", "\r", "\n", "\t", "+", "-", "*", "/", "%", 
			                       "|", "&", " ", "!", "?", "@", "$", "^", "[", "]", "=",
			                       "{" , "}", ",", ".", "/", "\\", "\"", ";", ":", "'",
			                       "<", "<=", "==", ">", ">=", "<>", "//"};
	
	/**
	 * Make a new FileScanner with the file passed in as a string.
	 * For example, using "input.txt" would use a file in the Compiler folder called "input.txt"
	 * 
	 * @param fileNameAndLocation		String for use as the file name.
	 */
	public FileScanner(String fileNameAndLocation)
	{
		scannedInput = new StringBuilder();
		if(setFile(fileNameAndLocation) == false) return;
		separatorLookupTable = new Hashtable<Integer, String>(separators.length);
		
		for(int i = 0; i < separators.length; i++)
		{
			separatorLookupTable.put(i, separators[i]);
		}
		
		scannerOutput = new BufferedReader(scanFile());
	}
	
	/**
	 * setFile(fileNameAndLocation) sets the file to be read and sets fileReader
	 * to that file. This method returns true if it is successful, and returns false
	 * if it should fail to created the BufferedReader for any reason.
	 * 
	 * @param fileNameAndLocation	The file to be read from.
	 * @return						True if successful, false if there is an IOException.
	 */
	public boolean setFile(String fileNameAndLocation)
	{
		line = null;//our current line to read from
		try {
			
			fileReader = new BufferedReader(new FileReader(fileNameAndLocation));
			fileName = fileNameAndLocation;//hold our file name so we can reference it again
			return true;
			
		} catch(IOException e) {
			ErrorMessage.fileNotFound(fileName);
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Scans the passed in file and writes out all of the
	 * separated lexemes to the StringBuilder.
	 * 
	 * @return	returns a StringReader object if successful, and 
	 * 		   	returns null if creating the StringReader failed.
	 */
	public StringReader scanFile(String fileNameAndLocation)
	{
		setFile(fileNameAndLocation);
		return scanFile();
	}
	
	/**
	 * Use this method after either setting the input file or after
	 * successfully instantiating the object. This reads the output
	 * of the separated lexemes and returns one token at a time.
	 * @return
	 * @throws IOException 
	 */
	public TokenNode getNextToken() throws IOException
	{
		if((line = scannerOutput.readLine()) != null)
		{
			if(TokenNode.isNumeric(line))
			{
				return new TokenNode(line, DataType.INT, Integer.valueOf(line));//return a token holding the value
			}
			
			if(singleQuoteFlag == 2)
			{
				singleQuoteFlag = 1;
				return new TokenNode("'" + line + "'", DataType.CHAR ,line.charAt(0));//wrap that token in the quotes
			}
			if(line.equals("'") && singleQuoteFlag == 0) singleQuoteFlag = 2;//opening quote
			if(line.equals("'") && singleQuoteFlag == 1) singleQuoteFlag = 0;//end quote
			return new TokenNode(line);
		}
		return null;
	}
	
	/**
	 * Scans the currently selected file and writes out all of the
	 * separated lexemes to the StringBuilder.
	 * 
	 * @return 	returns a StringReader object if successful, and 
	 * 		   	returns null if creating the StringReader failed.
	 */
	private StringReader scanFile() {
		
		try 
		{
			scannedInput = scannedInput.delete(0, scannedInput.length());//ensure it is empty
			
			while ((line = fileReader.readLine()) != null)
			{
				//in this block we can perform any operation on line
				//we need to scan in order to separate all lexemes
				scanHelper(line);
				
			}//end of loop for the file
			fileReader.close();
			return new StringReader(scannedInput.toString());
		
			} catch (IOException e) {
				e.printStackTrace();
		}//end try/catch
		return null;//we've failed to build anything
	}
	
	/**
	 * The scanHelper method accepts a line of text and separates out the lexemes
	 * by using the separator list as well as white space characters. When the lexemes
	 * are separated, they are written so they can be given to the parser later in the
	 * getNextToken() method.
	 * 
	 * @param line	The line of text to be separated out.
	 */
	private void scanHelper(String line) {
		
		currentLexeme = "";
		currentSeparator = "";
		
		for(int i = 0; i < line.length(); i++)//for all characters in the line read left to right
		{

			currentChar = line.charAt(i);
			
			//This if says 'if the currentChar is a separator'
			if(separatorLookupTable.contains(String.valueOf(currentChar)))
			{	
				/////////////////// Handle the separator characters \\\\\\\\\\\\\\\\\\\
				
				//we need to check for cases like >= where we have 2 separators together
				if(i + 1 < line.length())// if we aren't at the end of the line, check 
				{						 // for two separator characters
					
					checkDoubleSeparator = (String.valueOf(currentChar) + 
											String.valueOf(line.charAt(i+1)));
					
					if(separatorLookupTable.contains((checkDoubleSeparator)))
					{
						//we want to get both of those separators
						currentSeparator = checkDoubleSeparator;
						i++;//increment i so we don't process the 2nd char twice
						
						if(checkDoubleSeparator.equals("//"))
						{
							//ignore the rest of the line, this is a comment.
							return;
						}
					}
					else {// in this case we aren't at the end of the line but we still have 1 separator
						currentSeparator = String.valueOf(currentChar);
					}
				}
				else { //we just have a single separator 
					currentSeparator = String.valueOf(currentChar);
				}
				
				/////////////////// Handle the current lexeme \\\\\\\\\\\\\\\\\\\
				
				//System.out.println("We are inside the if block");
				if(currentLexeme.equals(""))//We don't need to write blank lines
				{
					//no need to write
				}
				else 
				{
					scannedInput.append(currentLexeme + newLine);//This is a symbol and will be printed
					currentLexeme = "";//clear it out so we can use it again
				}
				
				/////////////////// Output the separator character(s) \\\\\\\\\\\\\\\\\\\
				
				if(currentChar == ' ' | currentChar == '\t')//don't output spaces or tabs
				{
					//no need to write
				}
				else
				{
					scannedInput.append(currentSeparator + newLine);
					currentSeparator = "";
				}
			}//end of if the current char is a separator
			
			else
			{
				currentLexeme += line.charAt(i);
			}
		}//end of loop in the line
		
		
		if(currentLexeme.equals(""))//at the end of the line, if our currentLexeme
			//is empty, the line ended with a separator. Else, we still need to 
			//print it out and set currentLexeme = ""
		{
			//we don't need to do anything if our currentLexeme is blank.
		}
		else
		{
			scannedInput.append(currentLexeme + newLine);
			currentLexeme = "";
		}
	}
	
}
