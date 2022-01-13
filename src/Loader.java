import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


public class Loader {
	//private FileManager fileManager;
	private CPU cpu;
	private Memory memory; //원래 얘도 MemoryManager여야 함.



	private Process currentProcess;
	
	public Loader(CPU cpu, Memory memory) {
		this.cpu = cpu;
		this.memory = memory;
	}

	public void loadProcess(String fileName) {
		this.currentProcess = new Process();
		this.currentProcess.load(fileName);
	}
	
	class Process { //process manager가 어떤 process를 실행하는지 알아야 함
		//process마다 헤더가 따로 있고 start address 다 있어야 함.
		//class여야 프로세스 여러 개 만들 수 있음.
		static final short sizeHeader = 2; //PC, SP만 집어넣음
		//1바이트씩 보기 때문에 sizeHeader = 2
		static final short indexPC = 0;
		static final short indexSP = 1;
		
		private short startAddress;
		private short sizeData, sizeCode;
		private short PC, SP;
		
		private void loadHeader(Scanner scanner) {
			this.sizeData = scanner.nextShort(16);
			this.sizeCode = scanner.nextShort(16);
			this.startAddress = memory.allocate(sizeHeader+this.sizeData+this.sizeCode);
			cpu.setPC(startAddress + sizeHeader);
			cpu.setSP(startAddress + sizeHeader + this.sizeCode);
			cpu.setStartAddress(startAddress + sizeHeader);
			//jmp할 때 code segment에서 지정한 만큼 딱 jmp되도록 시작 address setting
		}
		
		private void loadBody(Scanner scanner) {
			//code segment
			short currentAddress = (short) (this.startAddress + sizeHeader);
			while(scanner.hasNext()) {
				memory.store(currentAddress, scanner.nextShort(16)); //16진법 읽을 때
				currentAddress++;
			}
		}
		
		public void load(String fileName) {
			try {
				Scanner scanner = new Scanner(new File("exe/"+fileName));
				this.loadHeader(scanner);
				this.loadBody(scanner);
				scanner.close();	

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

}
