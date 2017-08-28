import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

/*
 * Artem Davtyan
 * 
 * The CPU class simulates the control unit of the SC4 finite state machine.
 */
public class CPU {
	
    //Constant int values for the Control Unit's macrostates
	private final int FETCH = 0;	//Represents the Fetch macrostate
	private final int DECODE = 1;	//Represents the Decode macrostate
	private final int EXECUTE = 2;  //Represents the Execute macrostate
	private final int HALT = 3;		//Represents the Halt macrostate
	
	//Constant int values for Fetch's microstates
	private final int ifetch1 = 0;	//Represents Fetch's first microstate
	private final int ifetch2 = 1;	//Represents Fetch's second microstate
	private final int ifetch3 = 2;  //Represents Fetch's third microstate
	private final int ifetch4 = 3;  //Represents Fetch's fourth microstate
	private final int MICROSTATE_EXIT = 4;	//Represents Fetch's microstates are over
	
	//Constant int values for OPCODES
	private final int LDI_OPCODE = 1;	//OPCODE for the LDI instruction
	private final int LD_OPCODE = 2;	//OPCODE for the LD instruction
	private final int ST_OPCODE = 4;	//OPCODE for the ST instruction
	private final int ADD_OPCODE = 8;	//OPCODE for the ADD instruction
	private final int SUB_OPCODE = 9;	//OPCODE for the SUB instruction
	private final int AND_OPCODE = 10;	//OPCODE for the AND instruction
	private final int OR_OPCODE = 11;	//OPCODE for the OR instruction
	private final int NOT_OPCODE = 12;	//OPCODE for the NOT instruction
	private final int BR_OPCODE = 16;   //OPCODE for the BR instruction
	private final int BRZ_OPCODE = 17;  //OPCODE for the BRZ instruction
	private final int HALT_OPCODE = 29; //OPCODE for the HALT instruction
	
	//Constant int values for instruction format
	private final int FORMAT_1 = 1;	//Represents Format 1 Type instruction
	private final int FORMAT_2 = 2;	//Represents Format 2 Type instruction
	private final int FORMAT_3 = 3;	//Represents Format 3 Type instruction
	private final int FORMAT_4 = 4;	//Represents Format 4 Type instruction
	
	//Constant values for two's complement min and max (for checking overflow)
	private final long TWOS_COMP_MIN = -2147483648; 
	private final long TWOS_COMP_MAX = 2147483647;
    
	//Memory and Register Files
	private final int MEMORY_SIZE = 100;  //The size of the program's memory
	private final int REGFILE_SIZE = 16;  //The size of the program's register file
	private long[] MEMORY;                //Contains the program's memory as an array of longs
	private long[] RegFile;               //Contains the register file
    private String TEXT_FILE;             //The text file that contains the program
    private boolean RUN;                  //Whether program is set to run continuously
	    
	//Control Unit States
	private int state;				//Represents the Control Unit's current macrostate
	private int microstate;			//Represents the Control Unit's current microstate
	
	//Datapath Elements
	private long PC;			    //Contains the Program Counter's contents
	private long IR;			    //Contains the Instruction Register's contents
	private String IRstring;		//Contains the Instruction Register's contents as a 
									//binary String for decoding purposes
	private long MAR;			    //Contains the Memory Address Register's contents
	private long MDR;               //Contains the Memory Data Register's contents
	private long ALU_A; 		    //Contains the ALU's A Register's contents
	private long ALU_B;		        //Contains the ALU's B Register's contents
	private long ALU_R;             //Contains the ALU's Result Register's contents
	
	//Condition Code Bits
	private long SW;                //Contains the Status Word Register's contents
    private boolean CC_Zero;        //True if result is zero
    private boolean CC_Neg;         //True if result is negative
    private boolean CC_Carryout;    //True if result has a carryout
    private boolean CC_Overflow;    //True if result has an overflow
    
	//Decoded Elements from Instruction
	private int opcode;				//Contains the opcode of the instruction
	private int format;				//Contains the format of the instruction
	private int dr;				    //Contains the destination register of the instruction
	private int sr1;				//Contains the source register 1 of the instruction
	private int sr2;				//Contains the source register 2 of the instruction
	private int immed;				//Contains the immediate value of the instruction	
	
	//Scanner for console input
	private Scanner inputReader = new Scanner(System.in);
		
	/*
	 * Constructs an FSM_Control_Unit object which simulates the LC-2200 control unit.  
	 * It instantiates all the registers and memory.  It also sets Control unit's current state 
	 * to FETCH, microstate to 0, and instruction elements to 0.  It then runs runCycle(). 
	 */
	public CPU() {
		PC = 0;
		IR = 0;
		MAR = 0;
		MDR = 0;
		ALU_A = 0;
		ALU_B = 0;
		ALU_R = 0;
		SW = 0;
		opcode = 0;
		format = 0;
		dr = 0;
		sr1 = 0;
		sr2 = 0;
		immed = 0;
		RUN = false;
		RegFile = getRandLongArray(REGFILE_SIZE);
		MEMORY = getRandLongArray(MEMORY_SIZE);
		printState();
        pause();
		state = FETCH;
        microstate = 0;
		runCycle();
	}

    /*
     * Creates a new long array of specified size, filled with random long's up to 2^31-1.
     */
    private long[] getRandLongArray(int size) {
	    long[] result = new long[size]; //Initialize array of input size
	    for (int i = 0; i < result.length; i++) { //Go through each index
            result[i] = (long) (TWOS_COMP_MAX * Math.random()); //Initialize each index to random  
	    }
        return result;
    }
    
    /*
     * Loads the Memory as an array of "long"s.  The long's are pulled from individual lines of
     * the specified txt file from the same folder as the program.  
     */
    private long[] getMemory(String inputFile) {
	    long[] result = MEMORY;
	    int iterator = 0;
	    //Bring in text file to be read
	    InputStream input = getClass().getResourceAsStream(inputFile); 
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line;
        try { //Read through each line of text file
            while ((line = reader.readLine()) != null) { //While there is another line to read
                result[iterator] = getInstruction(line); //Put line's instruction into array
                iterator++; //Iterate array, so next line is put at next index
            }
        } catch (IOException e) { //Catch exceptions caused by reading the file
            e.printStackTrace();
        }
        try {
            reader.close(); //Close file when done reading it
        } catch (IOException e2) { //Catch exceptions caused by closing the file
            e2.printStackTrace();
        }
        return result;
    }
	
    /*
     * Returns the instruction from a give String or "line" of the input text.  It returns the
     * long value of the hex portion of the line.
     */
    private long getInstruction(String line) {
        int colonIndex = line.indexOf(':'); //finds the first colon (end of address)
        line = line.substring(colonIndex + 1); //cuts off address and colon to get just the instruction
        //Checks input line to see if they entered non-hex digits
        if (!line.matches("^[0-9A-Fa-f]+$")) { 
            System.out.println("\"" + line + "\" has invalid hex digits!"); 
        //If user inputs more than 8 digits, warn that it is too long.
        } else if (line.length() > 8) { 
            System.out.println(line + " is too long!"); 
        //If user inputs an invalid opcode, warn them
        } else { //Valid user input
            return Long.parseLong(line, 16); //Parse Hex String input into a BigInteger       
        }
        return 0; //If invalid command, return "NO OP"
    }
    
    /*
	 * Simulates the Control Unit's Fetch-Decode-Execute cycle.  It runs the fetch macrostate, 
	 * then decode macrostate, then execute macrostate, then back to fetch, etc until the 
	 * state is changed to HALT.
	 */
	private void runCycle() {
	    printState();
        pause();
	    while (state != HALT) {//Run the FSM's cycle until state is changed to HALT
		    switch (state) {//Switch to macrostate that is the current state of the Control Unit
		        case FETCH: //Runs the fetch() method which simulates Fetch's microstates
		                fetch();
		                break;
		                
		        case DECODE: //Runs the decode() method which simulates Decode's microstates
		                decode();
		                break;
		        
		        case EXECUTE: //Runs the execute() method which simulates Execute's microstates
		                execute();
		                break;
		                 
		        default: 
		                break;
			}
        }
	}

	/*
	 * Simulates the Fetch macrostate by running its microstates.
	 */
	private void fetch() {
		microstate = ifetch1;
		if (PC >= MEMORY.length) {//in case we run into end of code, halts machine
            state = HALT;
            microstate = MICROSTATE_EXIT;
        }
		while (microstate != MICROSTATE_EXIT) {
			switch (microstate) {
				case ifetch1: //Runs the ifetch1 microstate
    				    MAR = PC;
    			        ALU_A = PC;
    			        microstate = ifetch2;
    			        
    			        break;
	                
				case ifetch2: //Runs the ifetch2 microstate
	                	IR = MEMORY[(int) PC]; //Load the IR with contents of memory at PC's address
	                	microstate = ifetch3;
		                break;
		        
				case ifetch3: //Runs the ifetch3 microstate
    				    PC = ALU_A + 1; //Adds one to ALU_A and stores it in the PC
    			        microstate = ifetch4;
						break;
	                
				case ifetch4: //Runs the ifetch4 microstate
    				    state = DECODE;
    			        microstate = MICROSTATE_EXIT;
		                break;
		                 
		        default: 
		            	break;
			}
		}		     
	}
	
	/*
	 * Simulates the Decode macrostate by running its microstates.  The state is changed 
	 * to EXECUTE when complete.
	 */
	private void decode() {
		//Run the getIRstring method to get the string version of the instruction
		IRstring = getIRstring(IR);
		opcode = Integer.parseInt(IRstring.substring(0, 5), 2); //Convert opcode from binary to int
		//Run the getFormat method to get the format of the instruction
		format = getFormat(IRstring);	
		switch (format) {
			case FORMAT_1: //When a Format 1 instruction is given, set the destination register and immediate value
			        dr = Integer.parseInt(IRstring.substring(5, 9), 2); //Convert dr from binary to int
			        immed = (int)twosCompToLong(IRstring.substring(9, 32)); //Convert immed from binary to int
			        break;
		        
			case FORMAT_2: //When a Format 2 instruction is given, set the destination register, source register 1, 
			               //and immediate value
			        dr = Integer.parseInt(IRstring.substring(5, 9), 2); //Convert dr from binary to int
			        sr1 = Integer.parseInt(IRstring.substring(9, 13), 2); //Convert sr1 from binary to int
			        immed = (int)twosCompToLong(IRstring.substring(13, 32)); //Convert immed from binary to int  
		            break;
		    
			case FORMAT_3: //When a Format 3 instruction is given, set the destination register, source register 1, 
			               //and source register 2
			        dr = Integer.parseInt(IRstring.substring(5, 9), 2); //Convert dr from binary to int
			        sr1 = Integer.parseInt(IRstring.substring(9, 13), 2); //Convert sr1 from binary to int
			        sr2 = Integer.parseInt(IRstring.substring(13, 17), 2); //Convert sr2 from binary to int
					break;
		        
			case FORMAT_4: //When an O-TYPE instruction is given, no registers need to be set
			        immed = (int)twosCompToLong(IRstring.substring(5, 32)); //Convert immed from binary to int  
		            break;

		    default: 
		        	break;
		}
		state = EXECUTE;
	}
	
    /*
     * Converts the IR as an int to a binary string.  It extends the 0's of the left if there are 
     * not enough (should be of length 32).  
     * Returns the resulting string.
     */
    private String getIRstring(long input) {
        String result = Long.toString(input, 2); //Converts Instruction Register to binary
        result.length();
        while (result.length() < 32) { //Extends 0's, if there are not enough 0 bits
            result = "0" + result;
        }
        return result;
    }

    /*
	 * Converts a binary two's complement number to a Java long.
	 * The first number of a two's complement shows if the long is positive or negative.
	 * If negative, you subtract 2^(#of bits minus one).
	 */
	private long twosCompToLong(String twosCompString) {
	    int firstNumber = Integer.parseInt(twosCompString.substring(0, 1), 2);
	    long result = firstNumber;
	    if (twosCompString.length() > 1) {
    	    int length = twosCompString.length();
    	    long restOfNumber = Long.parseLong(twosCompString.substring(1, length), 2);
    	    result = restOfNumber; //if first number is zero, this is the resulting int
    	    if (firstNumber == 1) { //if first number is one, it's negative, so subtract 2^(#of bits minus one)
    	        result = (long) (restOfNumber - Math.pow(2, length-1));
    	    }
	    }
        return result;
    }

    /*
	 * Converts the input long into a 2's complement binary string.  It sign extends the 0's or 1's depending on 
	 * if the number is negative or positive.
	 * Returns the resulting binary string.
	 */
	private String longToTwosComp(long input) {
	    String result = Long.toBinaryString(input); //convert input to binary string
	    while (result.length() < 64) { //add 0's to get 64 bits, if less than 64
	        result = "0" + result;
	    }
	    result = result.substring(32, 64); //keep only last 32 bits
	    return result;
	}
	
	/*
	 * Computes the opcode from the IR.
	 * Returns the format of the instruction based on the opcode as an int (eg FORMAT_1, FORMAT_2, etc).
	 */
	private int getFormat(String IRstring) {
		
		if (opcode == LDI_OPCODE) {
			return FORMAT_1;
		} else if (opcode == LD_OPCODE || opcode == ST_OPCODE || opcode == NOT_OPCODE) {
			return FORMAT_2;
		} else if (opcode == ADD_OPCODE || opcode == SUB_OPCODE || opcode == AND_OPCODE || opcode == OR_OPCODE) {
			return FORMAT_3;
		} else {
			return FORMAT_4;
		}
	}
 
	
	/*
	 * Simulates the Execute macrostate by outputting the specified instruction and what it would 
	 * be doing to the console. If the HALT instruction is called, the state is set to HALT, 
	 * otherwise it is set to FETCH.
	 */
	private void execute() {
	    String lastOP = " RTL: ";
	    switch (opcode) {
		    case LDI_OPCODE: //Load indirect
		        lastOP += "(LDI) R" + dr + " <- " + immed;
				RegFile[dr] = immed;
		        break;
		        
			case LD_OPCODE: //Load base-relative
			    ALU_A = RegFile[sr1];
			    ALU_B = immed;
			    ALU_R = ALU_A + ALU_B;
			    ALU_R = twosCompToLong(longToTwosComp(ALU_R)); //ensures only 32 bits
			    if (ALU_R > MEMORY.length || ALU_R < 0) {
				    lastOP += "(LD) Error, memory out of bounds at: " + ALU_R;
				} else {
				    lastOP += "(LD) R" + dr + " <- MEM[R" + sr1 + " + "+ immed + "]";
				    RegFile[dr] = MEMORY[(int) ALU_R];
				}
				break;
	        	
			case ST_OPCODE: //Store base-relative
			    ALU_A = RegFile[sr1];
                ALU_B = immed;
                ALU_R = ALU_A + ALU_B;
                ALU_R = twosCompToLong(longToTwosComp(ALU_R)); //ensures only 32 bits
			    if (ALU_R > MEMORY.length || ALU_R < 0) {
                    lastOP += "(ST) Error, memory out of bounds at: " + ALU_R;
			    } else {
                    lastOP += "(ST) MEM[R" + sr1 + " + "+ immed + "] <- R" + dr;
                    MEMORY[(int) ALU_R] = RegFile[dr];                    
                }
	        	break;
	        	
			case ADD_OPCODE: //Addition
			    lastOP += "(ADD) R" + dr + " <- R" + sr1 + " + R" + sr2;
				ALU_A = RegFile[sr1];
				ALU_B = RegFile[sr2];
				ALU_R = ALU_A + ALU_B;
				setCC(ALU_R);
				ALU_R = twosCompToLong(longToTwosComp(ALU_R)); //ensures only 32 bits
				RegFile[dr] = ALU_R;
				break;
	        	
			case SUB_OPCODE: //Subtraction
			    SW = 0;
			    lastOP += "(SUB) R" + dr + " <- R" + sr1 + " - R" + sr2;
			    ALU_A = RegFile[sr1];
                ALU_B = RegFile[sr2];
                ALU_R = ALU_A - ALU_B;
                setCC(ALU_R);
                ALU_R = twosCompToLong(longToTwosComp(ALU_R)); //ensures only 32 bits
                RegFile[dr] = ALU_R;
                break;
	        	
			case AND_OPCODE: //Bit-wise AND
			    lastOP += "(AND) R" + dr + " <- R" + sr1 + " AND R" + sr2;
			    ALU_A = RegFile[sr1];
                ALU_B = RegFile[sr2];
                String binA = longToTwosComp(ALU_A);
                String binB = longToTwosComp(ALU_B);
                String binR = "";
                for (int i = 0; i < binA.length(); i++) {
                    if (binA.charAt(i) == binB.charAt(i) && binA.charAt(i) != '0') {
                        binR = binR + "1";
                    } else {
                        binR = binR + "0";
                    }
                }
                ALU_R = twosCompToLong(binR);
                setCC(ALU_R);
                RegFile[dr] = ALU_R;
                break;
	        	
			case OR_OPCODE: //Bit-wise OR
			    lastOP += "(OR) R" + dr + " <- R" + sr1 + " OR R" + sr2;
			    ALU_A = RegFile[sr1];
                ALU_B = RegFile[sr2];
                binA = longToTwosComp(ALU_A);
                binB = longToTwosComp(ALU_B);
                binR = "";
                for (int i = 0; i < binA.length(); i++) {
                    if (binA.charAt(i) == 1 || binB.charAt(i) == 1) {
                        binR = binR + "1";
                    } else {
                        binR = binR + "0";
                    }
                }
                ALU_R = twosCompToLong(binR);
                setCC(ALU_R);
                RegFile[dr] = ALU_R;
                break;
	        	
            case NOT_OPCODE: //Bit-wise NOT
                lastOP += "(NOT) R" + dr + " <- NOT R" + sr1;
                ALU_A = RegFile[sr1];
                binA = longToTwosComp(ALU_A);
                binR = "";
                for (int i = 0; i < binA.length(); i++) {
                    if (binA.charAt(i) == 0) {
                        binR = binR + "1";
                    } else {
                        binR = binR + "0";
                    }
                }
                ALU_R = twosCompToLong(binR);
                setCC(ALU_R);
                RegFile[dr] = ALU_R;
                break;
                
            case BR_OPCODE: //Unconditional branch
                lastOP += "(BR) PC <- iterated PC + " + immed;
                ALU_A = PC;
                ALU_B = immed;
                ALU_R = ALU_A + ALU_B;
                ALU_R = twosCompToLong(longToTwosComp(ALU_R)); //ensures only 32 bits
                PC = ALU_R;
                break;
                
            case BRZ_OPCODE: //Branch on zero
                lastOP += "(BRZ) ";
                if (CC_Zero) {
                    ALU_A = PC;
                    ALU_B = immed;
                    ALU_R = ALU_A + ALU_B;
                    ALU_R = twosCompToLong(longToTwosComp(ALU_R)); //ensures only 32 bits
                    PC = ALU_R;
                    lastOP += "PC <- iterated PC + " + immed;
                }
                else {
                    lastOP += "Did not branch";
                }
                break;
	        	
			case HALT_OPCODE: //Halt
			    lastOP += "(HALT) Halting Program...";
				state = HALT;
	        	break;
           
		    default: 
		        lastOP += "(NOP) No operation";
		        	break;
		}
	    printState();
	    System.out.print(lastOP);
	    System.out.print("     Binary: " + IRstring);
        if (state != HALT) { //Only change state to FETCH if HALT is not chosen
			state = FETCH; //Set state to FETCH after Execute is complete
			if (!RUN) {
			    pause();
			}
		}
		
	}

	/*
     * Sets condition codes based on long result from an operation.
     */
    private void setCC(long input) {
        CC_Zero = (input == 0);   
        CC_Neg = (input < 0);
        CC_Carryout = (input > TWOS_COMP_MAX || input < TWOS_COMP_MIN); //if too large or too small
        CC_Overflow = (input > TWOS_COMP_MAX || input < TWOS_COMP_MIN); //if too large or too small
        setSW();
    }
    
    /*
     * Sets the SW word based on current values of condition code (CC) flags for 
     * CC_Zero, CC_Neg, CC_Carryout, and CC_Overflow.  This is represented by the first
     * four bits of the SW word, respectively. (The rest is just 0's.)
     */
    private void setSW() {
        String SWstring = "";
        if (CC_Zero) { //If result is 0, turn on CC_Zero flag
            SWstring += "1";
        } else { //Else, turn off CC_Zero flag
            SWstring += "0";
        } 
        if (CC_Neg) { //If result is negative, turn on CC_Neg flag
            SWstring += "1";
        } else { //Else, turn off CC_Neg flag
            SWstring += "0";
        } 
        if (CC_Carryout) { //If result has a carryout, turn on CC_Carryout flag
            SWstring += "1";
        } else { //Else, turn off CC_Carryout flag
            SWstring += "0";
        } 
        if (CC_Overflow) { //If result has an overflow, turn on CC_Overflow flag
            SWstring += "1";
        } else { //Else, turn off CC_Overflow flag
            SWstring += "0";
        } 
        SWstring += "000000000000000000000000000"; //Fill in extra bits as 0
        SW = Long.parseLong(SWstring, 2); //Convert from binary to long for storage   
    }
    
    /*
	 * Prints the state of the machine into the console.
	 */
	private void printState() {
	    for (int j = 0; j < 24; j++) { //Print 24 line spacer
	        System.out.println();
	    }
	    System.out.println("  Debug Monitor");
	    System.out.println(" Register File                  Memory Dump");
	    for (int i = 0; i < RegFile.length; i++) { //iterate through RegFile registers to append them
            System.out.println(" " + Integer.toHexString(i).toUpperCase() + ": " + longToHex(RegFile[i]) + 
                    "                 " + longToHex((long)i) + ": " + longToHex(MEMORY[i]));
        }
	    System.out.println();
	    System.out.println(" PC: " + longToHex(PC) + "  IR: " + longToHex(IR) + " SW: " + longToHex(SW));
	    System.out.println(" MAR: " + longToHex(MAR) + " MDR: " + longToHex(MDR) + " ALU.A: " + longToHex(ALU_A) 
	                        + " ALU.B: " + longToHex(ALU_B) + " ALU.R: " + longToHex(ALU_R));
        
       
    }

	/*
	 * The pause method pauses the CPU's run cycle and waits for the user to press enter to continue.
	 */
    private void pause() { 
        System.out.println();
        System.out.print(" Commands: 1=Load, 2=Step, 3=Run, 4=Memory, 5=Save, 9=Exit    Enter: ");
	    try {
	        String input = inputReader.nextLine().trim(); //Get user input
	        System.out.println();
	        switch (input){
	            case "1": //Load file name
	                System.out.print(" Enter name of file to load: ");
	                TEXT_FILE = inputReader.nextLine().trim();
	                //if file does not end in .txt, put .txt on the end
	                if (!TEXT_FILE.toLowerCase().endsWith(".txt")) { 
	                    TEXT_FILE += ".txt";
	                }
	                MEMORY = getMemory(TEXT_FILE);
	                PC = 0; //resets PC
	                System.out.println();
	                printState();
	                break;
	                
	            case "2": //Step
	                break;
	                
	            case "3": //Run
                    RUN = true;
	                break;
                    
	            case "4": //Memory
	                
                    break;
                    
	            case "5": //Save
	                
                    break;
	                
	            case "9": //Exit
	                state = HALT;
	                System.out.println(" (EXIT) Terminating Console...");
	                break;
	                
	            default: //Invalid input
	                System.out.println(" Please enter a valid command.");
	                pause();
	                break;
	        }
	        
	    }  
	    catch(Exception e){ //Exception invalid file name
	        System.out.println(" Error, invalid file!");
	        pause();
	    }	    
	}
    
    /*
     * Returns a String "hex string" from the input long value.
     */
    private String longToHex(long input) {
        //convert to uppercase hex
        String result = Long.toHexString(input).toUpperCase(); 
        //if more than 8 hex for some reason, cut off left hex digits
        while (result.length() > 8) {
            result = result.substring(1);
        }
        //if input is too small, pad 0's
        while (result.length() < 8) {
            result = "0" + result;
        }
        return result;
    }

	/*
	 * This is the main method to start the program.  It creates a new CPU object 
	 * to start the CPU's cycle.
	 */
	public static void main(String[] args) {
	    new CPU();
	}	
}