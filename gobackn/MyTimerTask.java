package gobackn;

import java.util.TimerTask;

public class MyTimerTask extends TimerTask {
	FastFtp ftp;
	String serverName;
	int portNumber;
	
	public MyTimerTask(FastFtp tX, String serverName, int portNumber){
		this.ftp = tX;
		this.serverName = serverName;
		this.portNumber = portNumber;
	}

	public void run() {
		for (int i = 0; i < ftp.getTx().size(); i++) {
			try {
				System.out.println("Resend Window");
				Segment s = ftp.getTx().remove();
				ftp.sendData(s, serverName, portNumber);
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
