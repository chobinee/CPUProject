public class Memory {
	
	private short memory[];

	
	public Memory() {
		this.memory = new short[512]; //2바이트씩 512, 1024 bytes 할당
	}
	
	
	public short load(short mar) {
		//mar 주소 값을 찾아가 mbr로 넣음		
		return this.memory[mar];
	}
	
	public void store(short mar, short mbr) {
		this.memory[mar] = mbr;
	}


	public short allocate(int i) {	
		return 0;
	}

	
	

}
