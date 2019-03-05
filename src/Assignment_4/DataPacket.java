package Assignment_4;

import java.io.*;

public class DataPacket implements Serializable {
	int seq;
	long time;
	byte[] data;
	
	public DataPacket(int seq, byte[] data, long time){
		this.seq = seq;
		this.data = data;
		this.time = time;
	}
}