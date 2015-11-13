package gobackn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class MyAckListener implements Runnable{
	private FastFtp f;
	private int portNum;
	private int completedAckNumber;
	
	public MyAckListener(FastFtp f, int portNum, int completed){
		this.f = f;
		this.completedAckNumber = completed;
	}
	
	@Override
	public void run() {
		try {
			System.out.println("Started MyAckListener");
			DatagramSocket serverSocket = new DatagramSocket(portNum);
			byte[] in = new byte[Segment.MAX_PAYLOAD_SIZE];
			int segnum = 0;
			
			// need a way to beat this
			while(segnum != completedAckNumber){
				DatagramPacket dp = new DatagramPacket(in, in.length);
				System.out.println("Listening for responses");
				serverSocket.receive(dp);
				System.out.println("Received a segment");
				processAck(new Segment(dp));
			}
			
			serverSocket.close();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private synchronized void processAck(Segment ack){
		System.out.println("Processing received Ack");
		Segment[] tmp = f.getTx().toArray();
		boolean found  = false;
		int i = 0;
		while (!found && i < tmp.length){
			if (tmp[i].getSeqNum() == ack.getSeqNum()){
				found = true;
				break;
			} else {
				i++;
			}
		}
		if (found){
			while (f.getTx().element().getSeqNum() < ack.getSeqNum()){
				try {
					System.out.println("Removed ack #" + f.getTx().element().getSeqNum());
					f.getTx().remove();
				} catch (InterruptedException e) {
					System.out.println("The processing thread could not remove ack #: " + ack.getSeqNum());
					e.printStackTrace();
				}
			}
		}
	}
}
