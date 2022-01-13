public class CPU {
	
	private enum EOpCode{
		eHalt,
		eLDC,
		eLDA,
		eSTA,
		eADDA,
		eADDC,
		eSUBA,
		eSUBC,
		eMULA,
		eDIVA,
		eANDA,
		eNOTA,
		eJMPZ,
		eJMPBZ,
		eJMP,
		eJMPBEZ,// 0과 같거나 0보다 작을 때 jmp
		eDIVC // div constant

	}
	
	private enum ERegisters {
		ePC,
		eSP,
		eAC,
		eIR,
		eSR,
		eMAR,
		eMBR
	}
	

	
	private class Register{
		protected short value;
		
		public short getValue() {
			return this.value;
		}
		
		public void setValue(short value) {
			this.value = value;
		}
	}
	
	private class IR extends Register{
		//반으로 뜯어서 각각 가져오기
		public short getOpCode() {
			return (short)(this.value >> 8);
		}
		
		public short getOperand() {
			return (short)(this.value & 0x00FF);
		}
		

		
	}

	
	private class CU {

		public boolean isZero(Register sr) {
			if((short) (sr.getValue() & 0x8000) == 0) {
			return false;
			}
			return true;
		}
		
		public boolean isBZ(Register sr) {			
			if((short) (sr.getValue() & 0x4000) == 0) {
			return false;
			}
			return true;
		}

		public boolean isBEZ(Register sr) {
			if((short) (sr.getValue() & 0x8000) == 0 && (sr.getValue() & 0x4000) == 0) {
				//sr.getValue()와 0x8000을 masking 했을 때 0이고 (=뺀 값이 0이 아니라서 zero flag가 0이면)
				//sr.getValue()와 0x4000을 masking 했을 때 0이면 (=뺀 값이 0보다 작지 않아서 bz flag가 0이면)
				//뺀 값이 BEZ가 아닐때 즉 0이 아니고 0보다 크면 false return
				
				return false;
				}
				return true;
		}
		
	}
	
	private class ALU {
		//alu에 ac 저장하여 계산함.
		private short ac;
		
		
		public void add(short ac) {
			this.ac =  (short) (this.ac + ac);

		}
		
		public void sub(short ac) {
			this.ac = (short) (this.ac - ac);
		}


		public void store(short ac) {
			this.ac = ac;
		}


		public void mul(short ac) {
			this.ac =  (short) (this.ac * ac);
			
		}
		
		public void div(short ac) {
			this.ac = (short) (this.ac / ac);
			
		}

		
	}


	
	
	//components
	private CU cu;
	private ALU alu;
	private Register registers[];

	//associations
	private Memory memory;

	
	//status
	private boolean bPowerOn;
	private short startAddress;
	
	private boolean isPowerOn() {
		return this.bPowerOn;
	}
	
	public void setPowerOn() {
		this.bPowerOn = true;
		//pc, sp 값 초기화
		this.run();
	}
	
	public void shutDown() {
		this.bPowerOn = false;
	}
	

	//instructions

	private void Halt() {
		this.shutDown();

	}
	
	private void LDC() {
		// IR.operand -> MBR
		this.registers[ERegisters.eMBR.ordinal()].setValue(
				((CPU.IR) this.registers[ERegisters.eIR.ordinal()]).getOperand());
		// MBR -> AC
		this.registers[ERegisters.eAC.ordinal()].setValue(this.registers[ERegisters.eMBR.ordinal()].getValue());
	}
	
	private void LDA() {
		short address = (short) (((IR)this.registers[ERegisters.eIR.ordinal()]).getOperand() + this.registers[ERegisters.eSP.ordinal()].getValue());
		// IR.operand -> MAR
		this.registers[ERegisters.eMAR.ordinal()].setValue(address);
		//memory.load(MAR) -> MBR
		this.registers[ERegisters.eMBR.ordinal()].setValue(
				this.memory.load(this.registers[ERegisters.eMAR.ordinal()].getValue()));
		// MBR -> AC
		this.registers[ERegisters.eAC.ordinal()].setValue(this.registers[ERegisters.eMBR.ordinal()].getValue());
		
	}
	
	private void STA() {
		short address = (short) (((IR)this.registers[ERegisters.eIR.ordinal()]).getOperand() + this.registers[ERegisters.eSP.ordinal()].getValue());
		//IR.operand -> MAR
		this.registers[ERegisters.eMAR.ordinal()].setValue(address);
		//AC -> MBR
		this.registers[ERegisters.eMBR.ordinal()].setValue(this.registers[ERegisters.eAC.ordinal()].getValue());
		//memory.store(MAR, MBR)
		this.memory.store(this.registers[ERegisters.eMAR.ordinal()].getValue(),this.registers[ERegisters.eMBR.ordinal()].getValue());
	}
	
	private void ADDA() {
		// AC -> alu
		this.alu.store(this.registers[ERegisters.eAC.ordinal()].getValue());
		//IR.operand -> MAR
		this.LDA();
		//alu add
		this.alu.add(this.registers[ERegisters.eAC.ordinal()].getValue());
		this.registers[ERegisters.eAC.ordinal()].setValue(this.alu.ac);
	}
	
	private void ADDC() {
		this.alu.store(this.registers[ERegisters.eAC.ordinal()].getValue());
		//IR.operand -> MAR
		this.LDC();
		//alu add
		this.alu.add(this.registers[ERegisters.eAC.ordinal()].getValue());
		this.registers[ERegisters.eAC.ordinal()].setValue(this.alu.ac);
		
	}
	
	private void SUBA() {
		this.alu.store(this.registers[ERegisters.eAC.ordinal()].getValue());
		//IR.operand -> MAR
		this.LDA();
		//alu sub
		this.alu.sub(this.registers[ERegisters.eAC.ordinal()].getValue());

		this.registers[ERegisters.eAC.ordinal()].setValue(this.alu.ac);
		
		//subtract 후에 CU에서 flag 확인하고 SR 세팅
		if(this.registers[ERegisters.eAC.ordinal()].getValue() == 0) {
			this.registers[ERegisters.eSR.ordinal()].setValue((short) 0x8000);
		} else if(this.registers[ERegisters.eAC.ordinal()].getValue() < 0) {
			this.registers[ERegisters.eSR.ordinal()].setValue((short) 0x4000);
		} 
		else {
			this.registers[ERegisters.eSR.ordinal()].setValue((short) 0);
		}
	}
	
	private void SUBC() {
		this.alu.store(this.registers[ERegisters.eAC.ordinal()].getValue());
		//IR.operand -> MAR
		this.LDC();
		//alu sub
		this.alu.sub(this.registers[ERegisters.eAC.ordinal()].getValue());	
		this.registers[ERegisters.eAC.ordinal()].setValue(this.alu.ac);
		
		//subtract 후에 CU에서 flag 확인하고 SR 세팅
		if(this.registers[ERegisters.eAC.ordinal()].getValue() == 0) {
			this.registers[ERegisters.eSR.ordinal()].setValue((short) 0x8000);
		} else if(this.registers[ERegisters.eAC.ordinal()].getValue() < 0) {
			this.registers[ERegisters.eSR.ordinal()].setValue((short) 0x4000);
		} 
		else {
			this.registers[ERegisters.eSR.ordinal()].setValue((short) 0);
		}
	}
	
	private void MULA() {
		this.alu.store(this.registers[ERegisters.eAC.ordinal()].getValue());
		//IR.operand -> MAR
		this.LDA();
		//alu mul
		this.alu.mul(this.registers[ERegisters.eAC.ordinal()].getValue());
		this.registers[ERegisters.eAC.ordinal()].setValue(this.alu.ac);
	}
	
	private void DIVA() {
		this.alu.store(this.registers[ERegisters.eAC.ordinal()].getValue());
		//IR.operand -> MAR
		this.LDA();
		//alu mul
		this.alu.div(this.registers[ERegisters.eAC.ordinal()].getValue());
		this.registers[ERegisters.eAC.ordinal()].setValue(this.alu.ac);
	}
	
	private void ANDA() {
		
	}
	
	private void NOTA() {
		
	}
	
	//add와 똑같음 alu에게 시키기
	
	private void JMPZ() {
		if(this.cu.isZero(this.registers[ERegisters.eSR.ordinal()])) {
			// ir.operand -> PC // 주소를 PC로
			JMP();
		}
	}
	
	private void JMPBZ() {
		if(this.cu.isBZ(this.registers[ERegisters.eSR.ordinal()])) {
			// ir.operand -> PC // 주소를 PC로
			JMP();
		}
	}
	
	private void JMP() {
		this.registers[ERegisters.ePC.ordinal()].setValue(
				(short) (((CPU.IR) this.registers[ERegisters.eIR.ordinal()]).getOperand()+this.startAddress-1));
		//jump할 때 operand에 start address 더하여 memory에 저장된 code segment 시작점 맞추고
		//execute 후에 PC 값이 1 증가되므로 1을 빼줌.
	}
	
	private void JMPBEZ() {
		if(this.cu.isBEZ(this.registers[ERegisters.eSR.ordinal()])) {
			// ir.operand -> PC // 주소를 PC로
			JMP();
		}
	}
	
	private void DIVC() {
		this.alu.store(this.registers[ERegisters.eAC.ordinal()].getValue());
		//IR.operand -> MAR
		this.LDC();
		//alu div
		this.alu.div(this.registers[ERegisters.eAC.ordinal()].getValue());
		this.registers[ERegisters.eAC.ordinal()].setValue(this.alu.ac);
	}


		
		//constructor
		public CPU() {
			this.cu = new CU();
			this.alu = new ALU();		
			this.registers = new Register[ERegisters.values().length];
			
			for (ERegisters eRegister : ERegisters.values()) {
				this.registers[eRegister.ordinal()] = new Register();
			}
			
			this.registers[ERegisters.eIR.ordinal()] = new IR();
			
		}
		
		public void associate(Memory memory) {
			this.memory = memory;
		}
		
	

	
	private void fetch() {
		// PC -> MAR
		this.registers[ERegisters.eMAR.ordinal()].setValue(this.registers[ERegisters.ePC.ordinal()].getValue());
		// memory.load(MAR)
		this.registers[ERegisters.eMBR.ordinal()].setValue(this.memory.load(this.registers[ERegisters.eMAR.ordinal()].getValue()));
		// MBR -> IR
		this.registers[ERegisters.eIR.ordinal()].setValue(this.registers[ERegisters.eMBR.ordinal()].getValue());

	}
	
	private void decode() {
		//operand가 주소인 경우(A가 달린 경우) mar까지 옮기는 것까지해서 SP 더해줌
		
	}

	
	private void execute() {

		switch(EOpCode.values()[((IR)this.registers[ERegisters.eIR.ordinal()]).getOpCode()]) {
		case eHalt:
			this.Halt();
			System.out.println("please debug here");
			break;
		case eLDC:
			this.LDC();
			break;
		case eLDA:
			this.LDA();
			break;
		case eSTA:
			this.STA();
			break;
		case eADDA:
			this.ADDA();
			break;
		case eADDC:
			this.ADDC();
			break;
		case eSUBA:
			this.SUBA();
			break;
		case eSUBC:
			this.SUBC();
			break;
		case eMULA:
			this.MULA();
			break;
		case eDIVA:
			this.DIVA();
			break;
		case eANDA:
			this.ANDA();
			break;
		case eNOTA:
			this.NOTA();
			break;
		case eJMPZ:
			this.JMPZ();
			break;
		case eJMPBZ:
			this.JMPBZ();
			break;
		case eJMP:
			this.JMP();
			break;
		case eJMPBEZ:
			this.JMPBEZ();
			break;
		case eDIVC:
			this.DIVC();
			break;
			default:
				break;
			
		}
		

	}
	
	private void checkInterrupt() {
		//this.bPowerOn = false;
	}
	

	public void run() {
		while(this.isPowerOn()) {
		this.fetch();
		this.decode();
		this.execute();
		this.registers[ERegisters.ePC.ordinal()].setValue((short) (this.registers[ERegisters.ePC.ordinal()].getValue()+1));
		//execute 후에 PC 값 1 증가
		}
		
	}
	
	


	public static void main(String args[]) {
		CPU cpu = new CPU();
		Memory memory = new Memory();
		cpu.associate(memory);
		Loader loader = new Loader(cpu, memory);
		loader.loadProcess("test");
		cpu.setPowerOn();
		
	}

	public void setPC(int i) {
		// TODO Auto-generated method stub
		this.registers[ERegisters.ePC.ordinal()].setValue((short) i);
	}
	
	public void setSP(int i) {
		// TODO Auto-generated method stub
		this.registers[ERegisters.eSP.ordinal()].setValue((short) i);
	}

	public void setStartAddress(int i) {
		this.startAddress = (short) i;
	}
	

}
