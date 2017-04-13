import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Parser has a scanner created from a file and reads from that scanner
 * then outputs the production numbers that correspond to the input.
 * The parser also has an AVL Tree with all the reserved words and 
 * their production numbers. When going through the reductions on
 * the parse stack, code is generated using the values of the TokenNodes
 * in the stack.
 * 
 * @author Matt Benson
 *
 */
public class Parser {
	
	private SymbolTable symbolTable;			//The symbol table used to keep track of vars
	private final int SYMBOLTABLESIZE = 1024;	//The default size for the symbol table
	private int symbolValueTracker = 1;			//0 is reserved for swap space in code generation
	
	private String fileName;					//The file name of the input file
	private FileScanner scanner;				//The scanner used to read the input file and tokenize
	private BufferedReader scannerOutput;		//BufferedReader used to help keep track of input file
	
	private TokenNode currentToken;				//The current token being worked on
	private StateNode currentState;				//The current state in the parse table
	private TokenNode nextToken;				//The next scanned token
	private StateNode nextState;				//The next state to be pushed on
	
	private HashMap<Integer, Integer> productionMapping;//Used to match Tokens in the stack to numbers in table
	private ArrayDeque<ParseStackNode> parseStack;//Holds Tokens and States in stack while parsing
	private ArrayDeque<String> declarations;	//Holds var declarations while vars are being reduced
	private DataType declarationType;			//The type declared along a set of variables
	private CodeWriter writer;					//writes output to a file for mICE
	
	
	//These temp nodes are used in the generateCodeFromProduction() method to help generate code
	private ParseStackNode temp1;
	private ParseStackNode temp2;
	private ParseStackNode temp3;
	private ParseStackNode temp4;
	private ParseStackNode temp5;
	
	/* 
	 * Left-Hand-Side number, number of things produced, production numbers produced on RHS
	 * Production number is the index - refer to grammar sheet
	 */
 	private int[][] productions = {
								   {0,0,0},//0
								   {100, 12, 101, 1, 2, 3, 11, 102, 12, 9, 103, 105, 10, 13},
			
								   {101, 1, 4}, 
								   {101, 1, 5},
								   
								   {102, 1, 3},
								   {102, 3, 102, 36, 3},//5 - updated from {102, 2, 102, 3}
								   {102, 0}, //this one is null, so do I need anything else?
								   
								   {103, 6, 103, 6, 102, 7, 104, 8},
								   {103, 0}, //null production
								   
								   {104, 1, 14},
								   {104, 1, 15},//10
								   
								   {105, 3, 9, 106, 10},
								   
								   {106, 1, 107},
								   {106, 3, 106, 8, 107},
								   
								   {107, 1, 108},
								   {107, 1, 105},//15
								   {107, 4, 16, 11, 3, 12},
								   {107, 4, 17, 11, 3, 12},
								   {107, 5, 18, 11, 110, 12, 107},
								   
								   {108, 3, 3, 19, 109},
								   
								   {109, 1, 110},//20
								   
								   {110, 1, 111},
								   {110, 3, 111, 114, 111},
								   
								   {111, 1, 112},
								   {111, 3, 111, 115, 112},
								   
								   {112, 1, 113},//25
								   {112, 3, 112, 116, 113},
								   
								   {113, 1, 3},
								   {113, 1, 20},
								   {113, 1, 21},
								   {113, 1, 22},//30
								   //{113, 1, 34}, //literal numbers - update this for ' 34 '
								   {113, 3, 37, 34, 37},//updated
								   
								   {114, 1, 23},
								   {114, 1, 24},
								   {114, 1, 25},
								   {114, 1, 26},//35
								   {114, 1, 27},
								   {114, 1, 28},
								   
								   {115, 1, 29},
								   {115, 1, 30},
								   
								   {116, 1, 31},//40
								   {116, 1, 32},
								   {116, 1, 33}
										};
	
	/**
	 * This parse table uses 0 as an accepting state. Note that the first index is the state
	 * number, then that row has indecies defined in the same order as the are listed below.
	 * I would like to modify [76][mulop] in order to accept statements like SUM = SUM + 1 / 30;
	 * It currently is not part of our language - there is no way to derive it.
	 * All symbols and their production values are as follows:
	 *  $end, 35
	 *  $, 13
	 *  %, 33
	 *  ', 37
	 *  (, 11
	 *  ), 12
	 *  *, 31
	 *  +, 29
	 *  ,, 36
	 *  -, 30
	 *  /, 32
	 *  :, 7
	 *  ;, 8
	 *  <, 27
	 *  =, 19
	 *  >, 23
	 *  {, 9
	 *  }, 10
	 *  error, 99 - this is our reject action
	 *  STATIC, 1
	 *  VOID, 2
	 *  ID, 3
	 *  PUBLIC, 4
	 *  PRIVATE, 5
	 *  VAR, 6
	 *  CHAR, 14
	 *  INT, 15
	 *  GET, 16
	 *  PUT, 17
	 *  IF, 18
	 *  NUM, 20
	 *  TRUE, 21
	 *  FALSE, 22
	 *  LITERAL, 34
	 *  >=, 24
	 *  ==, 25
	 *  <=, 26
	 *  <>, 28
	 *  $accept, 0 
	 *  start, 100
	 *  access, 101
	 *  indentifer_list, 102
	 *  declarations, 103
	 *  type, 104
	 *  compound_statement, 105
	 *  statement_list, 106
	 *  statement, 107
	 *  lefthandside, 108
	 *  righthandside, 109
	 *  expression, 110
	 *  simple_expression, 111
	 *  term, 112
	 *  factor, 113
	 *  relop, 114
	 *  addop, 115
	 *  mulop 116
	 */
	private int[][] parseTable = {
//state $end	$	%	\	(	)	*	+	,	-	/	:	;	<	=	>	{	}	error	STATIC	VOID	ID	PUBLIC	PRIVATE	VAR	CHAR	INT	GET	PUT	IF	NUM	TRUE	FALSE	Literal	>=	==	<=	<>	$accept	start	access	identifier_list	declarations	type	compound_statement	statement_list	statement	lefthandside	righthandside	expression	simple_expression	term	factor	relop	addop	mulop
/* 0 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	1,	2,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	3,	4,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 1 */ {-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	-2,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 2 */ {-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	-3,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 3 */ {5,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 4 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	6,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 5 */ {0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 6 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	7,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 7 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	8,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 8 */ {99,	99,	99,	99,	9,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 9 */ {-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	10,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	99,	99,	99,	11,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 10 */ {-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	-4,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 11 */ {99,	99,	99,	99,	99,	12,	99,	99,	13,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 12 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	14,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 13 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	15,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 14 */ {-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	-8,	99,	99,	99,	99,	16,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 15 */ {-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	-5,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 16 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	18,	99,	99,	99,	99,	99,	99,	99,	17,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	19,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 17 */ {-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	10,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	-6,	99,	99,	99,	20,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 18 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	18,	99,	99,	99,	99,	21,	99,	99,	99,	99,	99,	22,	23,	24,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	25,	26,	27,	28,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 19 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	29,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 20 */ {99,	99,	99,	99,	99,	99,	99,	99,	13,	99,	99,	30,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 21 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	31,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 22 */ {99,	99,	99,	99,	32,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 23 */ {99,	99,	99,	99,	33,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 24 */ {99,	99,	99,	99,	34,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 25 */ {-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	-15,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 26 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	36,	99,	99,	99,	99,	35,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 27 */ {-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	-12,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 28 */ {-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	-14,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 29 */ {99,	37,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 30 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	38,	39,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	40,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 31 */ {99,	99,	99,	45,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	41,	99,	99,	99,	99,	99,	99,	99,	99,	42,	43,	44,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	46,	47,	48,	49,	50,	99,	99,	99,	},
/* 32 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	51,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 33 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	52,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 34 */ {99,	99,	99,	45,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	41,	99,	99,	99,	99,	99,	99,	99,	99,	42,	43,	44,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	53,	48,	49,	50,	99,	99,	99,	},
/* 35 */ {-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	-11,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 36 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	18,	99,	99,	99,	99,	21,	99,	99,	99,	99,	99,	22,	23,	24,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	25,	99,	54,	28,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 37 */ {-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	-1,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 38 */ {-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	-9,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 39 */ {-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	-10,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 40 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	55,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 41 */ {-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	-27,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 42 */ {-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	-28,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 43 */ {-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	-29,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 44 */ {-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	-30,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 45 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	56,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 46 */ {-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	-19,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 47 */ {-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	-20,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 48 */ {-21,	-21,	-21,	-21,	-21,	-21,	-21,	63,	-21,	64,	-21,	-21,	-21,	61,	-21,	57,	-21,	-21,	-21,	-21,	-21,	-21,	-21,	-21,	-21,	-21,	-21,	-21,	-21,	-21,	-21,	-21,	-21,	-21,	58,	59,	60,	62,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	65,	66,	99,	},
/* 49 */ {-23,	-23,	69,	-23,	-23,	-23,	67,	-23,	-23,	-23,	68,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	-23,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	70,	},
/* 50 */ {-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	-25,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 51 */ {99,	99,	99,	99,	99,	71,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 52 */ {99,	99,	99,	99,	99,	72,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 53 */ {99,	99,	99,	99,	99,	73,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 54 */ {-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	-13,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 55 */ {-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	-7,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 56 */ {99,	99,	99,	74,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 57 */ {-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	-32,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 58 */ {-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	-33,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 59 */ {-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	-34,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 60 */ {-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	-35,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 61 */ {-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	-36,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 62 */ {-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	-37,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 63 */ {-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	-38,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 64 */ {-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	-39,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 65 */ {99,	99,	99,	45,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	41,	99,	99,	99,	99,	99,	99,	99,	99,	42,	43,	44,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	75,	49,	50,	99,	99,	99,	},
/* 66 */ {99,	99,	99,	45,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	41,	99,	99,	99,	99,	99,	99,	99,	99,	42,	43,	44,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	76,	50,	99,	99,	99,	},
/* 67 */ {-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	-40,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 68 */ {-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	-41,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 69 */ {-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	-42,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 70 */ {99,	99,	99,	45,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	41,	99,	99,	99,	99,	99,	99,	99,	99,	42,	43,	44,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	77,	99,	99,	99,	},
/* 71 */ {-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	-16,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 72 */ {-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	-17,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 73 */ {99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	18,	99,	99,	99,	99,	21,	99,	99,	99,	99,	99,	22,	23,	24,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	25,	99,	78,	28,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 74 */ {-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	-31,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	66,	99,	},
/* 75 */ {-22,	-22,	-22,	-22,	-22,	-22,	-22,	63,	-22,	64,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	-22,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	70,	},
/* 76 */ {-24,	-24,	69,	-24,	-24,	-24,	67,	-24,	-24,	-24,	68,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	-24,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},//should change the last goto to 70
/* 77 */ {-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	-26,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
/* 78 */ {-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	-18,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	99,	},
	};
	
	/**
	 * Initializes field variables and creates the production mappings.
	 * 
	 * @param file	The name of the input file to be compiled.
	 */
	public Parser(String file)
	{
		//initializing fields
		fileName = file;
		scanner = new FileScanner(fileName);
		productionMapping = new HashMap<Integer, Integer>();
		parseStack = new ArrayDeque<ParseStackNode>();
		declarations = new ArrayDeque<String>();
		symbolTable = new SymbolTable(SYMBOLTABLESIZE);
		
		populateMapping();//write all the production mappings
	}
	
	/**
	 * ParseInput goes through each token and derives whether the input is accepted
	 * or not by using shift/reduce with the parseTable. Start in state 0 (push 0 on the stack),
	 * then use the parse table to find the next state based on the next token.
	 * 
	 * @return		False if input is invalid (reaches a rejecting state in parseTable - 99)
	 * 				True if input is accepted (reaches an accepting state in parseTable - 0)
	 */
	public boolean parseInput(boolean verbose) {
	
	try {
		writer = new CodeWriter(fileName);
		
		currentState = new StateNode(0);//start in state 0
		parseStack.push(currentState);//push 0 initially
		
		nextToken = scanner.getNextToken();
		
		if(verbose) {
			System.out.println(parseStack.toString());
			System.out.println();
			System.out.println("Next token: " + nextToken.getName() + " : " + nextToken.getNumber());
		}
		
		while(true)
		{
			
			nextState = new StateNode(parseTable[currentState.getState()][productionMapping.get(nextToken.getNumber())]);
			

			if(nextState.getState() == 99 || nextToken.getNumber() == 99) {	
				
				ErrorMessage.failureAtToken(nextToken);
				ErrorMessage.syntaxError();
				return false;//reject
				
			} else if(nextState.getState() > 0) {
				shift(verbose);
				
			} else if(nextState.getState() < 0) {
				//reduce
				if(!reduce(verbose))
				{
					return false;//return false if reduce fails
				}
				
			} else {
			
				System.out.println("Input accepted and compiled");
				System.out.println("Output intermediate code file: " + fileName +".asm");
				writer.close(); //this is required when we stop processing so the file gets written
				return true;
			}
			parseStack.push(nextState);
			currentState = nextState;
			
		}//end while
	} catch (NullPointerException e) {
				e.printStackTrace();
				ErrorMessage.syntaxError();
	} catch (IOException e) {
				e.printStackTrace();
				ErrorMessage.cannotParse();
			}
	
	return false;//this won't be reached unless the try fails
	}//end method
	
	/**
	 * This method gets the next token number by getting the next token
	 * from the scanner then calling .getNumber() on that token.
	 * 
	 * @return	returns the number for the next token as int.
	 */
	public int getNextTokenNumber()
	{
		currentToken = null;
		try {
			if((currentToken = scanner.getNextToken()) != null)
			{
				return currentToken.getNumber();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new TokenNode("$end").getNumber();//error case: tempToken is null - this means we've reached the end
	}
	
	/**
	 * The getProductionNumbers() method prints out all the tokens from
	 * the scanned input and the token number next to it. This method
	 * is not currently used in the compiler, but can be helpful for 
	 * visualizing the process.
	 */
	public void getProductionNumbers()
	{
		getTokens();
		String line = "";
		TokenNode temp;
		try {
			while(!(line = scannerOutput.readLine()).equals("$"))
			{
				//find the production number of each line and output it
				temp = new TokenNode(line);
				System.out.println(line + ": " + temp.getNumber());
				
			}
			System.out.println("$: " + 13);//production for $ not sure if this is needed
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			ErrorMessage.cannotParse();
		}
	}
	
	/**
	 * The getTokens() method gets a readable form of all the tokens
	 * found by the scanner where each token is on a line. It is used
	 * in the getProductionNumbers() method which is not currently
	 * used.
	 */
	private void getTokens()
	{
		scannerOutput = new BufferedReader(scanner.scanFile(fileName));
	}
	
	/**
	 * The shift method pushes the 'nextToken' from the last turn
	 * onto the parseStack then gets the next token from the input.
	 * If the new token is an ID, it is stored in the symbol table.
	 *  
	 * @throws IOException
	 */
	private void shift(boolean verbose) throws IOException {
		
		parseStack.push(nextToken);	
		
		nextToken = scanner.getNextToken();
		
		if(nextToken == null)
		{
			nextToken = new TokenNode("$end");
		}
		
		if(nextToken.getNumber() == 3 && !symbolTable.lookup(nextToken.getName()))
		{
			//we have an ID that isn't in the table
			symbolTable.insert(nextToken.getName(), nextToken.getType(), symbolValueTracker);//our first seen ID will be 0, the
			//next will be 1, then 2, etc.
			symbolValueTracker++;
		}
		
		if(verbose) {
			System.out.println(parseStack.toString());
			System.out.println();
			System.out.println("Next token: " + nextToken.getName() + " : " 
							 + nextToken.getType() + " : " + nextToken.getNumber());
		}
		
	}
	
	/**
	 * The reduce() method uses the state from the parse table and the productions
	 * list to determine how many pairs of states and tokens to pop off, then what
	 * should replace them.
	 * 
	 * @return	returns the value of reduceHelper()
	 * @throws IOException
	 */
	private boolean reduce(boolean verbose) throws IOException {
		
		//invert the sign back to positive
		nextState = new StateNode(0 - nextState.getState());
		
		currentToken = new TokenNode(productions[nextState.getState()][0]);
		currentToken.setName(TokenNode.getNameFromNonTerminal(Integer.valueOf(currentToken.name)));
		
		generateCodeFromProduction(verbose);
		
		for(int i = 0; i < productions[nextState.getState()][1]; i++)//The number of pairs to pop off
		{	
			//This could be more efficient by essentially running the generateCodeFromProductions inside this loop
			//since the values are needed from the tokens being popped off.
			
			parseStack.pop();//pop off the state
			
			//we need to validate that each of these is the right thing to pop off using the productions
			if(productions[nextState.getState()][productions[nextState.getState()].length - i - 1] != parseStack.pop().getNumber())
			{
				ErrorMessage.reductionError();
				return false;
			}//end if
		}//end for	
		return reduceHelper(verbose);
		
	}//end reduce method

	/**
	 * The reduceHelper method pushes the currentToken onto the stack,
	 * then uses the previous state to get the next state from the parse
	 * table and production mapping.
	 * 
	 * @return	Returns false if there is an error, otherwise, true.
	 */
	private boolean reduceHelper(boolean verbose) {
		//get the state we need, but don't alter currentState
		StateNode previousState = (StateNode) parseStack.peek();
		
		//push the reduced production back on the stack
		parseStack.push(currentToken);
		
		if(verbose) {
			System.out.println("Reduces to: " + currentToken.getName());
		}

		//now we need to goto for the next production - we're in previousState and we see NextToken
		nextState = new StateNode(parseTable[previousState.getState()][productionMapping.get(parseStack.peek().getNumber())]);
		
		if(nextState.getState() == 99)//if the next state will be an error state
		{
			ErrorMessage.syntaxError();
			return false;
		}
		return true;
	}
	
	/**
	 * Generates code based on each production number. Each production has
	 * its own rules - for more infomation, look for each production number
	 * in the switch case.
	 * 
	 * @return		returns true if successful
	 * @throws IOException
	 */
	private boolean generateCodeFromProduction(boolean verbose) throws IOException {
	
		int productionRule = nextState.getState();//the current grammar rule being used
		
		if(verbose) {
			System.out.println("PRODUCTION RULE " + productionRule);
		}
		
		switch (productionRule)
		{
		
		case 0: //no code to generate
			break;
			
		case 1://start -> access static void ID ( identifier_list ) { declarations compound_statement } $
			//we're finished when we get to this production
			//write the halt command to the output file
			writer.write("HLT ,,");
			break;
			
		case 2://access -> public
			//no code to generate
			break;
			
		case 3://access -> private
			//no code to generate
			break;
			
		case 4://identifier_list -> ID
			//flows down to case 5	
			
		case 5://identifier_list -> identifier_list , ID
			//we don't want to consider the first identifier_list since it is for the params and unused
			for(Iterator<ParseStackNode> itr = parseStack.iterator();itr.hasNext();)  
			{
				   if(itr.next().name.equals("identifier_list"))//if we see an identifier_list in the stack
				   {	//we need to get the 2nd element on the stack, so we pop off the top temporarily  	
						temp1 = parseStack.pop();
						declarations.push(parseStack.peek().toString());//we need to store this name
						parseStack.push(temp1);//restore the top of the stack
						break;
				   }
			}
			break;
			
		case 6://identifier_list -> [null]
			//no code to generate
			break;
			
		case 7://declarations -> declarations var identifier_list : type ;
			//find all of the names in the declarations stack in symbol table
			while(!declarations.isEmpty())//while declarations aren't empty
			{
				//update the symbol in the table to their declared type
				symbolTable.changeType(declarations.pop(), declarationType);
			}
			//clear the declarations stack
			declarations.clear();
			//clear the declarationType
			declarationType = null;
			break;
			
		case 8://declarations -> [null]
			//no code to generate			
			break;
			
		case 9://type -> char
			declarationType = DataType.CHAR;//Mark the current declarationType as CHAR
			break;
			
		case 10://type -> int
			declarationType = DataType.INT;//Mark the current declarationType as INT
			break;
			
		case 11://compound_statement -> { statement_list }
			//no code to generate
			break;
			
		case 12://statement_list -> statement
			//no code to generate
			break;
			
		case 13://statement_list -> statement_list ; statement
			//no code to generate
			break;
			
		case 14://statement -> lefthandside
			//no code to generate
			break;
			
		case 15://statement -> compound_statement
			//no code to generate
			break;
			
		case 16://statement -> get ( ID )
			temp1 = parseStack.pop();
			temp2 = parseStack.pop();
			temp3 = parseStack.pop();
			//the top of the stack is the ID
			//This could be more DRY by making a method for get/put
			if(symbolTable.lookupType(parseStack.peek().name) == DataType.INT) {
				//use int output
				writer.write("SYS #1,," + symbolTable.lookupValue(parseStack.peek().name));
			} else {
				//use char output
				writer.write("SYS #2,," + symbolTable.lookupValue(parseStack.peek().name));
			}
			parseStack.push(temp3);
			parseStack.push(temp2);
			parseStack.push(temp1);
			break;
			
		case 17://statement -> put ( ID ) 
			
			temp1 = parseStack.pop();
			temp2 = parseStack.pop();
			temp3 = parseStack.pop();
			//the top of the stack is the ID
			if(symbolTable.lookupType(parseStack.peek().name) == DataType.INT) {
				//use int output
				writer.write("SYS #-1," + symbolTable.lookupValue(parseStack.peek().name) + ",");
			} else {
				//use char output
				writer.write("SYS #-2," + symbolTable.lookupValue(parseStack.peek().name) + ",");
			}
			parseStack.push(temp3);
			parseStack.push(temp2);
			parseStack.push(temp1);
			break;
			
		case 18://statement -> if ( expression ) statement
			writer.completeJumpStatement();
			break;
			
		case 19://lefthandside -> ID = righthandside
			temp1 = parseStack.pop();
			temp2 = parseStack.pop();
			temp3 = parseStack.pop();
			temp4 = parseStack.pop();
			temp5 = parseStack.pop();
			//we just get from the temp var and put into ID - ID is the stack top
			writer.write("STO " + 0 + ",," + symbolTable.lookupValue(parseStack.peek().name));
			
			parseStack.push(temp5);
			parseStack.push(temp4);
			parseStack.push(temp3);
			parseStack.push(temp2);
			parseStack.push(temp1);
			break;
			
		case 20://righthandside -> expression
			//no code to generate
			break;
			
		case 21://expression -> simple_expression
			temp1 = parseStack.pop();
			//bring the value up from simple_expression
			currentToken.setValue(parseStack.peek().value);
			
			String simple_expressionValue = String.valueOf(parseStack.peek().value);
			
			if(!simple_expressionValue.equals("0") && !simple_expressionValue.equals("true")
					&& !simple_expressionValue.equals("false"))//no need to write STO 0,,0 or store for true/false
			{
				writer.write("STO " + parseStack.peek().value + ",,0");//write the term into our temp space
			}
			parseStack.push(temp1);
			break;
			
		case 22://expression -> simple_expression relop simple_expression
			temp1 = parseStack.pop();
			temp2 = parseStack.pop();//simple_expression (second one)
			temp3 = parseStack.pop();
			temp4 = parseStack.pop();//relop
			temp5 = parseStack.pop();
			//stack top is simple_expression (first one)
			
			if(temp4.value.equals(">")) {
				writer.writeWithMark("JLE " + parseStack.peek().value + "," + temp2.value + ",");
			} else if(temp4.value.equals(">=")) {
				writer.writeWithMark("JLT " + parseStack.peek().value + "," + temp2.value + ",");
			} else if(temp4.value.equals("==")) {
				writer.writeWithMark("JNE " + parseStack.peek().value + "," + temp2.value + ",");
			} else if(temp4.value.equals("<=")) {
				writer.writeWithMark("JGT " + parseStack.peek().value + "," + temp2.value + ",");
			} else if(temp4.value.equals("<")) {
				writer.writeWithMark("JGE " + parseStack.peek().value + "," + temp2.value + ",");
			} else if(temp4.value.equals("<>")) {
				writer.writeWithMark("JEQ " + parseStack.peek().value + "," + temp2.value + ",");
			}
			parseStack.push(temp5);
			parseStack.push(temp4);
			parseStack.push(temp3);
			parseStack.push(temp2);
			parseStack.push(temp1);
			break;
			
		case 23://simple_expression -> term
			temp1 = parseStack.pop();
			currentToken.setValue(parseStack.peek().value);
			parseStack.push(temp1);
			break;
			
		case 24://simple_expression -> simple_expression addop term
			temp1 = parseStack.pop();
			temp2 = parseStack.pop();//this is factor
			temp3 = parseStack.pop();
			temp4 = parseStack.pop();//this is mulop
			temp5 = parseStack.pop();
			//stack top is simple_expression
			currentToken.setValue("0");//the reference should be for 0 (the work space)
			
			if(temp4.value.equals("+")) {
				writer.write("ADD " + parseStack.peek().value + "," + temp2.value + "," + 0);
			}
			if(temp4.value.equals("-")) {
				writer.write("SUB " + parseStack.peek().value + "," + temp2.value + "," + 0);
			}
			parseStack.push(temp5);
			parseStack.push(temp4);
			parseStack.push(temp3);
			parseStack.push(temp2);
			parseStack.push(temp1);
			break;
			
		case 25://term -> factor
			temp1 = parseStack.pop();
			currentToken.setValue(parseStack.peek().value);//pass the value up
			parseStack.push(temp1);
			break;
			
		case 26://term -> term mulop factor
			temp1 = parseStack.pop();
			temp2 = parseStack.pop();//temp2 is factor
			temp3 = parseStack.pop();
			temp4 = parseStack.pop();//temp4 is mulop
			temp5 = parseStack.pop();
			//stack top is term
			currentToken.setValue("0");//the reference should be for 0 (the work space)
			
			if(temp4.value.equals("*")) {
				writer.write("MUL " + parseStack.peek().value + "," + temp2.value + "," + 0);
			}
			if(temp4.value.equals("/")) {
				writer.write("DIV " + parseStack.peek().value + "," + temp2.value + "," + 0);
			}
			if(temp4.value.equals("%")) {
				writer.write("MOD " + parseStack.peek().value + "," + temp2.value + "," + 0);
			}
			parseStack.push(temp5);
			parseStack.push(temp4);
			parseStack.push(temp3);
			parseStack.push(temp2);
			parseStack.push(temp1);	
			break;
			
		case 27://factor -> ID
			temp1 = parseStack.pop();
			currentToken.setValue(Integer.toString(symbolTable.lookupValue(parseStack.peek().name)));//need the string
			parseStack.push(temp1);
			break;
			
		case 28://factor -> num
			temp1 = parseStack.pop();
			currentToken.setValue("#" + parseStack.peek().name);
			parseStack.push(temp1);
			break;
			
		case 29://factor -> true
			currentToken.setValue("true");				
			break;
			
		case 30://factor -> false
			currentToken.setValue("false");
			writer.writeWithMark("JMP ,,");//Always jump for "if(false)"
			break;
			
		case 31://factor -> ' literal '
			currentToken.setType(DataType.CHAR);
			temp1 = parseStack.pop();
			temp2 = parseStack.pop();
			temp3 = parseStack.pop();
			
			char character = parseStack.peek().name.charAt(1);//get the character value
			String ascii = Integer.toString( (int) character);//convert to ascii integer then String
			currentToken.setValue("#" + ascii);//add the # for direct access numbers
			
			parseStack.push(temp3);
			parseStack.push(temp2);
			parseStack.push(temp1);
			break;
			 
		case 32://relop -> >
			//jump if less than or equal
			currentToken.setValue(">");			
			break;
			
		case 33://relop -> >=
			//jump if less than
			currentToken.setValue(">=");
			break;
			
		case 34://relop -> ==
			//jump if not equal
			currentToken.setValue("==");
			break;
			
		case 35://relop -> <=
			//jump if greater than
			currentToken.setValue("<=");
			break;
			
		case 36://relop -> <
			//jump if less than or equal
			currentToken.setValue("<");
			break;
			
		case 37://relop -> <> 
			//jump if equal
			currentToken.setValue("<>");
			break;
			
		case 38://addop -> +
			currentToken.setType(DataType.INT);
			currentToken.setValue("+");
			break;
			
		case 39://addop -> -
			currentToken.setType(DataType.INT);
			currentToken.setValue("-");
			break;
			
		case 40://mulop -> *
			currentToken.setType(DataType.INT);
			currentToken.setValue("*");
			break;
			
		case 41://mulop -> /
			currentToken.setType(DataType.INT);
			currentToken.setValue("/");
			break;
			
		case 42://mulop -> %
			currentToken.setType(DataType.INT);
			currentToken.setValue("%");
			break;
			
		default:
		}
		return true;
	}
	
	/**
	 * These mappings for the production numbers matches each possible symbol to the proper
	 * column in the parse table.
	 */
	private void populateMapping()
	{
		productionMapping.put(35, 0); //  $end, 35
		productionMapping.put(13, 1); //  $, 13
		productionMapping.put(33, 2); //  %, 33
		productionMapping.put(37, 3); //  ', 37
		productionMapping.put(11, 4); //  (, 11
		productionMapping.put(12, 5); //  ), 12
		productionMapping.put(31, 6); //  *, 31
		productionMapping.put(29, 7); //  +, 29
		productionMapping.put(36, 8); //  ,, 36
		productionMapping.put(30, 9); //  -, 30
		productionMapping.put(32, 10); //  /, 32
		productionMapping.put(7, 11); //  :, 7
		productionMapping.put(8, 12); //  ;, 8
		productionMapping.put(27, 13); //  <, 27
		productionMapping.put(19, 14); //  =, 19
		productionMapping.put(23, 15); //  >, 23
		productionMapping.put(9, 16); //  {, 9
		productionMapping.put(10, 17); //  }, 10
		productionMapping.put(99, 18); //  error, 99 - this is our reject action
		productionMapping.put(1, 19); //  STATIC, 1
		productionMapping.put(2, 20); //  VOID, 2
		productionMapping.put(3, 21); //  ID, 3
		productionMapping.put(4, 22); //  PUBLIC, 4
		productionMapping.put(5, 23); //  PRIVATE, 5
		productionMapping.put(6, 24); //  VAR, 6
		productionMapping.put(14, 25); //  CHAR, 14
		productionMapping.put(15, 26); //  INT, 15
		productionMapping.put(16, 27); //  GET, 16
		productionMapping.put(17, 28); //  PUT, 17
		productionMapping.put(18, 29); //  IF, 18
		productionMapping.put(20, 30); //  NUM, 20
		productionMapping.put(21, 31); //  TRUE, 21
		productionMapping.put(22, 32); //  FALSE, 22
		productionMapping.put(34, 33); //  LITERAL, 34
		productionMapping.put(24, 34); //  >=, 24
		productionMapping.put(25, 35); //  ==, 25
		productionMapping.put(26, 36); //  <=, 26
		productionMapping.put(28, 37); //  <>, 28
		productionMapping.put(0, 38); //  $accept, 0 
		productionMapping.put(100, 39); //  start, 100
		productionMapping.put(101, 40); //  access, 101
		productionMapping.put(102, 41); //  indentifer_list, 102
		productionMapping.put(103, 42); //  declarations, 103
		productionMapping.put(104, 43); //  type, 104
		productionMapping.put(105, 44); //  compound_statement, 105
		productionMapping.put(106, 45); //  statement_list, 106
		productionMapping.put(107, 46); //  statement, 107
		productionMapping.put(108, 47); //  lefthandside, 108
		productionMapping.put(109, 48); //  righthandside, 109
		productionMapping.put(110, 49); //  expression, 110
		productionMapping.put(111, 50); //  simple_expression, 111
		productionMapping.put(112, 51); //  term, 112
		productionMapping.put(113, 52); //  factor, 113
		productionMapping.put(114, 53); //  relop, 114
		productionMapping.put(115, 54); //  addop, 115
		productionMapping.put(116, 55); //  mulop 116 
	}
}
