package gobackn;


import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;


/**
 * FastFtp Class
 * 
 * FastFtp implements a basic FTP application based on UDP data transmission.
 * The main mehtod is send() which takes a file name as input argument and send the file 
 * to the specified destination host.
 * 
 */
public class FastFtp {

	private TxQueue 		transfer;
	private int				rtoTimer;
	private Timer			timer;
	private List<Segment>	segFile;

	/**
	 * Constructor to initialize the program 
	 * 
	 * @param windowSize	Size of the window for Go-Back_N (in segments)
	 * @param rtoTimer		The time-out interval for the retransmission timer (in milli-seconds)
	 */
	public FastFtp(int windowSize, int rtoTimer) {
		transfer = new TxQueue(windowSize);
		this.rtoTimer = rtoTimer;
		timer = new Timer();
	}

	public void sendData(Segment s, String serverName, int serverPort){
		try {
			DatagramPacket dp = new DatagramPacket(s.getBytes(), 
					s.getLength(), 
					InetAddress.getByName(serverName), 
					serverPort);
			DatagramSocket ds = new DatagramSocket();
			ds.send(dp);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public TxQueue getTx() {
		return transfer;
	}
	
	/**
	 * Splits a file up into a collection of segments, with the maximum
	 * segment size as the partitions. The leftover, or for values
	 * smaller than the maximum size are all handled as well.
	 * 
	 * @param fileName
	 * @return ArrayList<Segments> An arrayList of segments containing byte data of the file.
	 */
	private ArrayList<Segment> splitFile(String fileName){
		if (fileName.equals("")){
			System.out.println("Can not open an empty string as a filepath, exiting");
			System.exit(-1);
		}

		int max = 5; // For testing, a small maximum value was used instead.

		//int max = Segment.MAX_PAYLOAD_SIZE;

		ArrayList<Segment> segmentedFile = new ArrayList<Segment>();
		Path f = Paths.get(fileName);
		try {
			byte[] fileBytes = Files.readAllBytes(f);
			byte[] payload = new byte[max];
			int i = 0;
			int j = 0;
			int seq = 0;
			while(i < fileBytes.length){
				if (j == payload.length){
					segmentedFile.add(new Segment(seq++, payload));
					j = 0;
				}
				payload[j] = fileBytes[i];				
				i++;
				j++;
			}
			// Last odd bytes/ if file < max
			byte[] tail = new byte[j];
			for(i = 0; i < tail.length; i++){
				tail[i] = payload[i];
			}
			segmentedFile.add(new Segment(seq++, tail));
		} catch (IOException e) {
			System.out.println("Could not locate file");
			System.exit(-1);
		}

		// Debug
		System.out.println("We have " + segmentedFile.size() + " Segments");
		for (int i = 0; i < segmentedFile.size(); i++){
			System.out.println("segment " + i + " size: " + segmentedFile.get(i).getLength());
		}

		return segmentedFile;
	}

	/**
	 * Sends the specified file to the specified destination host:
	 * 1. send file name and receiver server confirmation over TCP
	 * 2. send file segment by segment over UDP
	 * 3. send end of transmission over tcp
	 * 3. clean up
	 * 
	 * @param serverName	Name of the remote server
	 * @param serverPort	Port number of the remote server
	 * @param fileName		Name of the file to be trasferred to the rmeote server
	 */
	public void send(String serverName, int serverPort, String fileName) {
		try {
			// 0. Get/segment file
			segFile = splitFile(fileName);


			// 1. TCP overhead w/ server
			Socket soc = new Socket(serverName, serverPort);
			DataInputStream in = new DataInputStream(soc.getInputStream());
			DataOutputStream out = new DataOutputStream(soc.getOutputStream());

			out.writeUTF(fileName);
			out.flush();

			byte response = in.readByte();
			if (response == 0){

				// 1. Start listening for replies
				Thread t = new Thread(new MyAckListener(this, serverPort));
				t.start();

				// 2. send segments and add to queue
				for (int i = 0; i < segFile.size(); i++){
					Segment s = segFile.get(i);
					sendData(s, serverName, serverPort);
					timer.schedule(new MyTimerTask(this, serverName, serverPort), rtoTimer);
				}
			} else {
				System.out.println("The server has replied with error number: " + response);
			}

			soc.shutdownInput();
			soc.shutdownOutput();
			soc.close();
		} catch (IOException e) {
			System.out.println("Could not connect to server");
			System.exit(-1);
		}
	}


	/**
	 * A simple test driver
	 * 
	 */
	public static void main(String[] args) {
		int windowSize = 10; //segments
		int timeout = 100; // milli-seconds

		String serverName = "localhost";
		String fileName = "";
		int serverPort = 0;

		// check for command line arguments
		if (args.length == 3) {
			// either provide 3 paramaters
			serverName = args[0];
			serverPort = Integer.parseInt(args[1]);
			fileName = args[2];
		}
		else if (args.length == 2) {
			// or just server port and file name
			serverPort = Integer.parseInt(args[0]);
			fileName = args[1];
		}
		else {
			System.out.println("wrong number of arguments, try agaon.");
			System.out.println("usage: java FastFtp server port file");
			System.exit(0);
		}


		FastFtp ftp = new FastFtp(windowSize, timeout);

		System.out.printf("sending file \'%s\' to server...\n", fileName);
		ftp.send(serverName, serverPort, fileName);
		System.out.println("file transfer completed.");
	}
}