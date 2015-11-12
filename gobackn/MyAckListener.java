package gobackn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class MyAckListener implements Runnable{
	FastFtp f;
	int portNum;
	
	public MyAckListener(FastFtp f, int portNum){
		this.f = f;
	}
	
	@Override
	public void run() {
		try {
			DatagramSocket serverSocket = new DatagramSocket(portNum);
			
			byte[] in = new byte[Segment.MAX_PAYLOAD_SIZE];
			
			// need a way to beat this
			while(true){
				DatagramPacket dp = new DatagramPacket(in, in.length);
				serverSocket.receive(dp);
				Segment s = new Segment(dp);
				System.out.println("Received Ack: " + s.getSeqNum());
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
