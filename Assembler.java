package pippin;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.io.PrintWriter;
import java.util.TreeSet;

public class Assembler{
	/**
	* lists the mnemonics of the instructions that do not have arguments
	*/
	public static Set<String> noArgument = new TreeSet<String>();
	/**
	* lists the mnemonics of the instructions that allow immediate addressing
	*/
	public static Set<String> allowsImmediate = new TreeSet<String>();
	/**
	* lists the mnemonics of the instructions that allow indirect addressing
	*/
	public static Set<String> allowsIndirect = new TreeSet<String>();

	static {
		noArgument.add("HALT");
		noArgument.add("NOP");
		noArgument.add("NOT");

		allowsImmediate.add("LOD");
		allowsImmediate.add("ADD");
		allowsImmediate.add("SUB");
		allowsImmediate.add("MUL");
		allowsImmediate.add("DIV");
		allowsImmediate.add("AND");
		allowsImmediate.add("JUMP");
		allowsImmediate.add("JMPZ");

		allowsIndirect.add("LOD");
		allowsIndirect.add("STO");
		allowsIndirect.add("ADD");
		allowsIndirect.add("SUB");
		allowsIndirect.add("MUL");
		allowsIndirect.add("DIV");
	} 

	/**
	* Method to assemble a file to its binary representation. If the input has errors
	* a list of errors will be written to the errors map. If there are errors,
	* they appear as a map with the line number as the key and the description of the error
	* as the value. If the input or output cannot be opened, the "line number" key is 0.
	* @param input the source assembly language file
	* @param output the binary version of the program if the souce program is
	* correctly formatted
	* @param errors the errors map
	* @return
	*/
	public static boolean assemble(File input, File output, Map<Integer, String> errors){
		ArrayList<String> inputText = new ArrayList<String>();
		ArrayList<String> inCode = new ArrayList<String>();
		ArrayList<String> inData = new ArrayList<String>();
		ArrayList<String> outCode = new ArrayList<String>();
		ArrayList<String> outData = new ArrayList<String>();
		int j = 0;
		boolean doData = false;
		int offset = 1;

		int firstBlank = 0;
		boolean checkedFirstBlank = false;

		if(errors == null){
			throw new IllegalArgumentException("Coding error: the error map in null");
		}else{
			try(Scanner inp = new Scanner(input)){
	            // while loop reading the lines from input in inputText
				while(inp.hasNextLine()){
					inputText.add(inp.nextLine());
				}
			}catch (FileNotFoundException e){
				errors.put(0, "Error: Unable to open the input file");
			}
			

			//checks for errors
			for(int i = 0; i < inputText.size(); i++){
				if(inputText.get(i).trim().length() > 0){
					if(Character.isWhitespace(inputText.get(i).charAt(0)) || inputText.get(i).charAt(0) == '\t'){
						errors.put(i + 1, "Error on line " + (i + 1) + ": starts with white space"); 
					}
				}if(inputText.get(i).trim().length() == 0){
						if(!checkedFirstBlank){
							checkedFirstBlank = true;
							firstBlank = i;
						}
				}if(inputText.get(i).trim().length() > 0 && checkedFirstBlank){
						errors.put(firstBlank, "Error on line " + firstBlank + ": illegal blank line");
					}
				}
			

	        //splits inputText
			for(int i = 0; i < inputText.size(); i++){
				String line = inputText.get(i).trim();

				if(line.equalsIgnoreCase("DATA")){
					if(line.equals("DATA")){
						doData = true;
					}else{
						errors.put(i + 1, "Error on line " + (i + 1) + ": DATA must be capitalized error");
					}
				}if(!doData && (!checkedFirstBlank)){
					inCode.add(line);
				}if(doData && !line.equals("DATA") && (!checkedFirstBlank)){
					inData.add(line);
				}
			}

			j = 0;
			for(String s: inCode){
				String[] parts = s.split("\\s+");

				if(!(InstructionMap.opcode.containsKey(parts[0].toUpperCase()))){
					errors.put((j + 1), "Error on line " + (j + 1) + ": Not a mnemonic");
				}else if(InstructionMap.opcode.containsKey(parts[0].toUpperCase())){
					if(InstructionMap.opcode.containsKey(parts[0])){
						//mnemonic is okay... Go on to processing each instruction

						if(noArgument.contains(parts[0]) && parts.length > 1){
							errors.put((j + 1), "Error on line " + (j + 1) + ": mnemonic doesn't take arguments");
						}else if(noArgument.contains(parts[0]) && parts.length == 1){
							outCode.add(Integer.toString(InstructionMap.opcode.get(parts[0]), 16) + " 0 0");
						}
						//Done for all non arguments
						else{
							//for immediate and indirect addressing needs
							if(!(noArgument.contains(parts[0])) && parts.length > 2){
								errors.put((j + 1), "Error on line " + (j + 1) + ": too many arguments");
							}else if(parts.length == 2){
								//3 else cases
								//Case 1
								if(parts[1].length() >= 3 && parts[1].charAt(0) == '[' && parts[1].charAt(1) == '['){
									if(allowsIndirect.contains(parts[0])){
										try{
											int arg = Integer.parseInt(parts[1].substring(2),16);
											outCode.add(Integer.toString(InstructionMap.opcode.get(parts[0]), 16) + " " +
											Integer.toString(arg, 16).toUpperCase() + " 2");
										}catch(NumberFormatException e){
											errors.put((j + 1), "Error on line "+ (j + 1) + ": indirect argument is not a hex number 1");
										}
									}else{
										errors.put((j + 1), "Error on line " + (j + 1) + ": mnemonic does not allow indirect addressing");
									}
								//Case 2
								}else if(parts[1].length() >= 2 && parts[1].charAt(0) == '['){
									try{
										int arg = Integer.parseInt(parts[1].substring(1),16);
										outCode.add(Integer.toString(InstructionMap.opcode.get(parts[0]), 16) + " " +
										Integer.toString(arg, 16).toUpperCase() + " 1");
									}catch(NumberFormatException e){
										errors.put(j + 1, "Error on line "+ (j + 1)+ ": direct argument is not a hex number");
									}
								//Case 3
								}else if(parts[1].length() >= 1 && parts[1].charAt(0) != '['){
									if(allowsImmediate.contains(parts[0])){
										try{
											int arg = Integer.parseInt(parts[1].substring(0),16);
											outCode.add(Integer.toString(InstructionMap.opcode.get(parts[0]), 16) + " " +
											Integer.toString(arg, 16).toUpperCase() + " 0");
										}catch(NumberFormatException e){
											errors.put((j + 1), "Error on line "+ (j + 1)+ ": immediate argument is not a hex number 2");
										}
									}else{
										errors.put((j + 1), "Error on line " + (j + 1) + ": mnemonic does not allow immediate addressing");
									}
								}
							}
							else
							{
								errors.put((j + 1), "Error on line " + (j + 1) + ": not enough arguments");
							}
						}
					}else{
						errors.put((j + 1), "Error on line " + (j + 1) + ": mnemonic is not uppercase");
					}
					j++;
				}
			}

			offset = inCode.size() + 1;
			j = 1;
			for(String s: inData){
				if(s.isEmpty())
					continue;

				String[] parts = s.trim().split("\\s+");
				
				if(parts.length != 2 && parts.length != 0){
					errors.put(offset + j, "Error on line "+ (offset + j) + ": immediate argument is not a hex number ");
				}else if(parts.length == 0){

				}else{
					int addr = -1;
					int val = -1;

					try{
						addr = Integer.parseInt(parts[0],16);
						val = Integer.parseInt(parts[1],16);
						if((!(parts[0].matches("[0-9A-Fa-f]+")))){
							errors.put((offset + j), "Error on line " + (offset + j) + ": Not a hex number");
						}
					}catch(NumberFormatException e){
						errors.put((offset + j), "Error on line " + (offset + j) + ": Not a hex number");
					}

					outData.add(Integer.toString(addr, 16).toUpperCase() + " "
						+ Integer.toString(val, 16).toUpperCase());
				}
				j++;
			}

			if(errors.size() == 0){
				try (PrintWriter outp = new PrintWriter(output)){
					for(String str : outCode) outp.println(str);
						outp.println(-1); // the separator where the source has "DATA"
					for(String str : outData) outp.println(str);
				}catch (FileNotFoundException e) {
					errors.put(0, "Error: Unable to write the assembled program to the output file");
				}
			}
			return errors.size() == 0; // TRUE means there were no errors
		}
	}
}
