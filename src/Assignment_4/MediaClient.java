package Assignment_4;

import java.io.*;
import java.net.*;

public class MediaClient {
	
	public static DataPacket convert(byte[] buf) throws Exception {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(buf);
		ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
		
		DataPacket pk = (DataPacket) is.readObject();
		is.close();
		
		return pk;
	}
	
	public static void main(String[] args) throws Exception {
		DatagramSocket socket = new DatagramSocket();
		int currentSequenceNo = 0;
		
		// send request
		byte[] buf = new byte[1000];
		
		InetAddress address = InetAddress.getByName("localhost");
		DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 5500);
		
		socket.send(packet);
		
		long statInterval = 1000; // 1s
		long lastTime = System.currentTimeMillis();
		int numberOfReceivedPackets = 0;
		long[] delays = new long[100000];
		
		// get response
		while(currentSequenceNo < 100000) {
			
			//receive packet
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			
			// record time packet was received at
			long currentTime = System.currentTimeMillis();
			numberOfReceivedPackets++;
			
			DataPacket pk = convert(packet.getData());
			currentSequenceNo = pk.seq;
			
			delays[numberOfReceivedPackets] = currentTime - pk.time;
			
			if((currentTime - lastTime) >= statInterval) {
				// throughput
				System.out.println("Throughput: " + numberOfReceivedPackets + "pps");
				
				// loss
				int loss = currentSequenceNo - numberOfReceivedPackets;
				System.out.println("Loss: " + loss + "pps");
				
				// delay
				System.out.println("Delay: " + delays[numberOfReceivedPackets] + "ms");
				
				// jitter
				long jitter = delays[numberOfReceivedPackets] - delays[numberOfReceivedPackets-1];
				System.out.println("Jitter: " + jitter + "ms");
				
				lastTime = currentTime;
				return;
			}
		}
		
		socket.close();
	}
}