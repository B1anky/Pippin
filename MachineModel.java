package pippin;
import java.util.*;
import java.util.Observable;

public class MachineModel extends Observable{

	public final Map<Integer, Instruction> INSTRUCTION_MAP = new TreeMap<Integer, Instruction>();
	private Registers cpu = new Registers();
	private Memory memory = new Memory();
	private boolean withGUI = false;
	private boolean running = false;

	private Code code;

	public MachineModel() {
		this(false);
	}

	public MachineModel(boolean withGUI) {
		this.withGUI = withGUI;

		INSTRUCTION_MAP.put(0x0, (arg, level) -> {
			if(level != 0){
				throw new IllegalArgumentException(
					"Illegal indirection level in NOP instruction");
			}
			if(level > 0){
				INSTRUCTION_MAP.get(0x0).execute(memory.getData(arg), level-1);
			}else{
				cpu.programCounter ++;
			}
		});

		INSTRUCTION_MAP.put(0x1, (arg, level) -> {
			if(level < 0 || level > 2){
				throw new IllegalArgumentException(
					"Illegal indirection level in LOD instruction");
			}
			if(level > 0){
				INSTRUCTION_MAP.get(0x1).execute(memory.getData(arg), level-1);
			}
			if(level == 0){
				cpu.accumulator = arg;
				cpu.programCounter ++;
			}
		});

		INSTRUCTION_MAP.put(0x2, (arg, level) -> {
			if(level < 1 || level > 2){
				throw new IllegalArgumentException(
					"Illegal indirection level in STO instruction");
			}
			if(level > 1){
				INSTRUCTION_MAP.get(0x2).execute(memory.getData(arg), level-1);
			}else{
				memory.setData(arg, cpu.accumulator);
				cpu.programCounter ++;
			}
		});

		INSTRUCTION_MAP.put(0x3, (arg, level) -> {
			if(level < 0 || level > 2){
				throw new IllegalArgumentException(
					"Illegal indirection level in ADD instruction");
			}
			if(level > 0){
				INSTRUCTION_MAP.get(0x3).execute(memory.getData(arg), level-1);
			}else{
				cpu.accumulator += arg;
				cpu.programCounter ++;
			}
		});

		INSTRUCTION_MAP.put(0x4, (arg, level) -> {
			if(level < 0 || level > 2){
				throw new IllegalArgumentException(
					"Illegal indirection level in SUB instruction");
			}
			if(level > 0){
				INSTRUCTION_MAP.get(0x4).execute(memory.getData(arg), level-1);
			}else{
				cpu.accumulator -= arg;
				cpu.programCounter ++;
			}
		});

		INSTRUCTION_MAP.put(0x5, (arg, level) -> {
			if(level < 0 || level > 2){
				throw new IllegalArgumentException(
					"Illegal indirection level in MUL instruction");
			}
			if(level > 0){
				INSTRUCTION_MAP.get(0x5).execute(memory.getData(arg), level-1);
			}else{
				cpu.accumulator *= arg;
				cpu.programCounter ++;
			}
		});

		INSTRUCTION_MAP.put(0x6, (arg, level) -> {
			if(level < 0 || level > 2){
				throw new IllegalArgumentException(
					"Illegal indirection level in DIV instruction");
			}
			if(level > 0){
				INSTRUCTION_MAP.get(0x6).execute(memory.getData(arg), level-1);
			}else{
				cpu.accumulator /= arg;
				cpu.programCounter ++;
			}
		});

		INSTRUCTION_MAP.put(0x7, (arg, level) -> {
			if(level < 0 || level > 1){
				throw new IllegalArgumentException(
					"Illegal indirection level in AND instruction");
			}
			if(level > 0){
				INSTRUCTION_MAP.get(0x7).execute(memory.getData(arg), level-1);
			}else{
				if(arg != 0 && cpu.accumulator != 0){
					cpu.accumulator = 1;
				}
				else{
					cpu.accumulator = 0;
				}
				
				cpu.programCounter ++;
			}
		});

		INSTRUCTION_MAP.put(0x8, (arg, level) -> {
			if(level != 0){
				throw new IllegalArgumentException(
					"Illegal indirection level in NOT instruction");
			}else{
				if(cpu.accumulator == 0){
					cpu.accumulator = 1;
				}else{
					cpu.accumulator = 0;
				}

				cpu.programCounter ++;
			}
		});

		INSTRUCTION_MAP.put(0x9, (arg, level) -> {
			if(level != 1){
				throw new IllegalArgumentException(
					"Illegal indirection level in CMPZ instruction");
			}
			else{
				if(memory.getData(arg) == 0){
					cpu.accumulator = 1;
					cpu.programCounter ++;
				}else{
					cpu.accumulator = 0;
					cpu.programCounter ++;
				}
			}
		});

		INSTRUCTION_MAP.put(0xA, (arg, level) -> {
			if(level != 1){
				throw new IllegalArgumentException(
					"Illegal indirection level in CMPL instruction");
			}
			else{
				if(memory.getData(arg) < 0){
					cpu.accumulator = 1;
					cpu.programCounter ++;
				}else{
					cpu.accumulator = 0;
					cpu.programCounter ++;
				}
			}
		});

		INSTRUCTION_MAP.put(0xB, (arg, level) -> {
			if(level < 0 || level > 1){
				throw new IllegalArgumentException(
					"Illegal indirection level in JUMP instruction");
			}
			if(level == 1){
				INSTRUCTION_MAP.get(0xB).execute(memory.getData(arg), level-1);
			}else{
				if(level == 0){
					cpu.programCounter = arg;
				}
			}	
		});


		INSTRUCTION_MAP.put(0xC, (arg, level) -> {
			if(level < 0 || level > 1){
				throw new IllegalArgumentException(
					"Illegal indirection level in JMPZ instruction");
			}
			if(level > 0){
				INSTRUCTION_MAP.get(0xC).execute(memory.getData(arg), level-1);
			}else{
				if(level == 0 && cpu.accumulator == 0){
					cpu.programCounter = arg;
				}else if(cpu.accumulator != 0 && level == 0){
					cpu.programCounter ++;
				}	
			}	
		});

		/**
		This instruction is fed an argument "arg", which serves as a marker in memory.
		That location's value is then used as the starting point of the instruction's effect.
		The value at location "arg + 1" is used as the total number of memory locations that
		will be affected by the instruction.
		The value at location "arg+2" serves as the translation to be applied on the memory values
		within the instruction's range.
		
		If the value at "arg+2" is negative, then all the values within the instruction's range 
		will be shifted toward the lower memory values by the value at "arg+2". Values that are smaller than
		the lower bound of the instruction range will be placed at the newly-vacated memory locations
		near the upper bound of the instruction range.

		If the value at "arg+2" is positive, then all the values within the instruction's range 
		will be shifted toward the upper memory values by the value at "arg+2". Values that are larger than
		the upper bound of the instruction range will be placed at the newly-vacated memory locations
		near the lower bound of the instruction range.

		@param arg the initial memory location at which the instruction begins
		*/
		INSTRUCTION_MAP.put(0x14, (arg, level) -> {
			if(level != 0){
				throw new IllegalArgumentException(
					"Illegal indirection level in ROT instruction");
			}
			else{
				int start = getData(arg);
				int length = getData(arg + 1);
				int move = getData(arg + 2);

				if(start < 0 || length < 0 || start + length - 1 >= Memory.DATA_SIZE){
					throw new IllegalArgumentException(
						"Start or length are too small or large");
				}else if(start <= arg + 2 && start + length - 1 >= arg){
					throw new IllegalArgumentException(
						"Start or length are too small or large");
				}else{
					if(move < 0){
						while(move != 0){
							setAccumulator(getData(start));
							for(int i = 0; i< length; i++){
								setData(start + i, getData(start+i+1));
							}
							
							setData(start+length+1, getAccumulator());
							move++;
						}
					}else if(move > 0){
						while(move != 0){
							setAccumulator(getData(start+length-1));
							for(int i = length-1; i >0; i--){
								setData(start + i, getData(start+i-1));
							}
							setData(start, getAccumulator());
							move--;
						}
					}
				}
			}
		});

		INSTRUCTION_MAP.put(0xF, (arg, level) -> {
			halt();
		});
	}





	public class Registers {

		private int accumulator;
		private int programCounter;

		public Registers() {

		}
	}

	public int getData(int index) {
		return memory.getData(index);
	}

	public void setData(int index, int value) {
		memory.setData(index, value);
	}

	public Instruction get(Integer key) {
		return INSTRUCTION_MAP.get(key);
	}

	int[] getData() {
		return memory.getData();
	}

	public int getProgramCounter() {
		return cpu.programCounter;
	}

	public int getAccumulator() {
		return cpu.accumulator;
	}

	public void setAccumulator(int i) {
		cpu.accumulator = i;
	}

	//new
	public void setProgramCounter(int i) {
		cpu.programCounter = i;
	}

	public int getChangedIndex() {
		return memory.getChangedIndex();
	}

	public void halt() {
		if (withGUI) {
			running = false;
		} else {
			System.exit(0);
		}
	}

	public void clearMemory() {
		memory.clear();
	}

	//new 2
	// public void setCode(int op, int arg, int level) {
	// 	this.code.setCode(op, arg, level);
	// }	

	public void setCode(Code code) {
		this.code = code;
	}	

	public void step() {
		try {
			int pc = cpu.programCounter;
			get(code.getOp(pc)).execute(code.getArg(pc), code.getIndirectionLevel(pc));
		}
		catch(Exception e) {
			halt();
			throw e;
		}
	}

	public void clear() {
		memory.clear();
		if (code != null) {
			code.clear();
			cpu.programCounter = 0;
			cpu.accumulator = 0;
		}
	}

	//new 3
	public void setRunning(boolean running) {
		this.running = running;
	}

	public boolean isRunning() {
		return this.running;
	}

	public Code getCode() {
		return this.code;
	}

	/**
	* Main method that drives the whole simulator
	* @param args command line arguments are not used
	*/
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new MachineView(new MachineModel(true));
			}
		});
	}

}