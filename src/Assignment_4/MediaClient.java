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
		int previousSequenceNo = 0;
		long previousDelay = 0;
		
		// get response
		while(currentSequenceNo < 10000000) {
			
			//receive packet
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			
			// record time packet was received at
			long currentTime = System.currentTimeMillis();
			
			// record number of received packets over time interval
			numberOfReceivedPackets++;
			
			DataPacket pk = convert(packet.getData());
			currentSequenceNo = pk.seq;
			
			// record delay
			long delay = currentTime - pk.time;
			
			if((currentTime - lastTime) >= statInterval) {
				
				// throughput
				System.out.println("Throughput: " + numberOfReceivedPackets + "pps");
				
				// loss
				int shouldHaveReceived = currentSequenceNo - previousSequenceNo;
				int loss = shouldHaveReceived - numberOfReceivedPackets;
				double percentageLoss = (loss * 100.0) / shouldHaveReceived;
				System.out.println("Loss: " + loss + "pps, Percentage: " + percentageLoss + "%");
				previousSequenceNo = currentSequenceNo;

				// delay
				System.out.println("Delay: " + delay + "ms");
				
				// jitter
				long jitter = delay - previousDelay;
				System.out.println("Jitter: " + jitter + "ms\n\n");
				
				lastTime = currentTime;
				numberOfReceivedPackets = 0;
			}
			
			// update previous delay
			previousDelay = delay;
		}
		
		socket.close();
	}
}