package com.cestbelt;

import java.io.IOException;
import java.io.OutputStream;

import com.cestbelt.BTHandler;

import java.util.ArrayList;
import java.util.Arrays;

public class BTSender extends Thread {

	static BTSender instance;
	
	public static enum sendCMD { SENDPING, SENDVERSION, SENDBUZZER, SENDACK, SENDREQUEST, SENDRESET }; 
	
	OutputStream out;
	ArrayList<sendCMD> jobs;


	private short PACKETNUMBER;
	private boolean RUNNING;
	
	public BTSender(OutputStream o) {
		out = o;
		jobs = new ArrayList<sendCMD>();
		RUNNING = true;
	}

	public void run() {
		if(out == null) {
			throw new IllegalArgumentException("no output stream referenced");
		}
		// infinite loop
		while(RUNNING) {
			if(jobs.size() > 0) {
				executeJob(jobs.get(0));
				jobs.remove(0);
			}
			try {
				// TODO hier weiter machen wait/notify einbauen um 100% auslastung zu umgehen
				this.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		destroyInstance();
	}

	private void executeJob(sendCMD s) {
		switch (s) {
		case SENDPING:
			sendPing();
			break;
		case SENDVERSION:
			sendVersion();
			break;
		case SENDBUZZER:
			sendBuzzer();
			break;
		case SENDREQUEST:
			sendDataRequest();
			break;		
		case SENDRESET:
			sendReset();
			break;
		}

	}
	
	public synchronized void interrupt() {
		if(RUNNING) {
			RUNNING  = false;
		} else {
			try {
				throw new InterruptedException("thread already requested to interrupt!");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}


	private synchronized void sendDataRequest() {
		if(out != null) {
			byte[] toSend = new byte[10];
			System.out.println("outgoing: CMD_REQUEST_DATA");
			toSend[0] = BTHandler.STARTBYTE;						// Startflag
			toSend[1] = (byte) (0xff & getPacketnumber());				// running Packetnumber
			toSend[2] = (byte) BTHandler.CMD_REQUEST_DATA;  // Command (upper part)
			toSend[3] = (byte) (BTHandler.CMD_REQUEST_DATA >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x02;					// length of payload
			toSend[5] = (byte) BTHandler.CMD_TX_DATA_RUNNING;// payload
			toSend[6] = (byte) (BTHandler.CMD_TX_DATA_RUNNING >> 8);	// payload
			byte[] crc = BTHandler.crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 7));
			toSend[7] = crc[0];
			toSend[8] = crc[1];
			toSend[9] = BTHandler.STOPBYTE;
			try {
				out.write(toSend);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	

	public void sendPingRequest() {
		jobs.add(sendCMD.SENDPING);		
		this.notify();
	}

	public void sendVersionRequest() {
		jobs.add(sendCMD.SENDVERSION);			
		this.notify();
	}

	public void sendResetRequest() {
		jobs.add(sendCMD.SENDRESET);			
		this.notify();
	}

	public void sendBuzzerRequest() {
		jobs.add(sendCMD.SENDBUZZER);			
		this.notify();
	}
	
	private synchronized void sendPing() {
		if(out != null) {
			byte[] toSend = new byte[8];
			System.out.println("outgoing: CMD_PING");
			toSend[0] = BTHandler.STARTBYTE;						// Startflag
			toSend[1] = (byte)(0xff & getPacketnumber());	// running Packetnumber
			toSend[2] = (byte) BTHandler.CMD_PING;  // Command (upper part)
			toSend[3] = (byte) (BTHandler.CMD_PING >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x00;
			byte[] crc = BTHandler.crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 5));
			toSend[5] = crc[0];
			toSend[6] = crc[1];
			toSend[7] = BTHandler.STOPBYTE;
			try {
				out.write(toSend);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

	private synchronized void sendReset() {
		if(out != null) {
			byte[] toSend = new byte[8];
			System.out.println("outgoing: CMD_ACKNOWLEDGE");
			toSend[0] = BTHandler.STARTBYTE;						// Startflag
			toSend[1] = (byte)(0xff & getPacketnumber());		// running Packetnumber
			toSend[2] = (byte) BTHandler.CMD_RESET;  // Command (upper part)
			toSend[3] = (byte) (BTHandler.CMD_RESET >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x00;					// length of payload
			byte[] crc = BTHandler.crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 5));
			toSend[5] = crc[0];
			toSend[6] = crc[1];
			toSend[7] = BTHandler.STOPBYTE;
			try {
				out.write(toSend);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private synchronized void sendAcknowledge(byte packetNumber) {
		if(out != null) {
			byte[] toSend = new byte[9];
			System.out.println("outgoing: CMD_ACKNOWLEDGE");
			toSend[0] = BTHandler.STARTBYTE;						// Startflag
			toSend[1] = (byte)(0xff & getPacketnumber());		// running Packetnumber
			toSend[2] = (byte) BTHandler.CMD_ACKNOWLEDGE;  		// Command (upper part)
			toSend[3] = (byte) (BTHandler.CMD_ACKNOWLEDGE >> 8);	// Command (lower part)
			toSend[4] = (byte) 0x01;					// length of payload
			toSend[5] = packetNumber;
			byte[] crc = BTHandler.crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 6));
			toSend[6] = crc[0];
			toSend[7] = crc[1];
			toSend[8] = BTHandler.STOPBYTE;
			try {
				out.write(toSend);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private synchronized void sendReject(byte packetNumber) {
		if(out != null) {
			byte[] toSend = new byte[9];
			System.out.println("outgoing: CMD_REQUEST_DATA");
			toSend[0] = BTHandler.STARTBYTE;						// Startflag
			toSend[1] = (byte)(0xff & getPacketnumber());			// running Packetnumber
			toSend[2] = (byte) BTHandler.CMD_REJECT;  			// Command (upper part)
			toSend[3] = (byte) (BTHandler.CMD_REJECT >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x01;					// length of payload
			toSend[5] = packetNumber;
			byte[] crc = BTHandler.crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 6));
			toSend[6] = crc[0];
			toSend[7] = crc[1];
			toSend[8] = BTHandler.STOPBYTE;
			try {
				out.write(toSend);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private synchronized void sendVersion() {
		if(out != null) {
			byte[] toSend = new byte[8];
			System.out.println("outgoing: CMD_SOFTWARE_VERSION");
			toSend[0] = BTHandler.STARTBYTE;						// Startflag
			toSend[1] = (byte)(0xff & getPacketnumber());		    	// running Packetnumber
			toSend[2] = (byte) BTHandler.CMD_SOFTWARE_VERSION;  			// Command (upper part)
			toSend[3] = (byte) (BTHandler.CMD_SOFTWARE_VERSION >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x00;					// length of payload
			byte[] crc = BTHandler.crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 5));
			toSend[5] = crc[0];
			toSend[6] = crc[1];
			toSend[7] = BTHandler.STOPBYTE;
			try {
				out.write(toSend);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private synchronized void sendBuzzer() {
		if(out != null) {
			byte[] toSend = new byte[8];
			System.out.println("outgoing: CMD_ENABLE_BUZZER");
			toSend[0] = BTHandler.STARTBYTE;						// Startflag
			toSend[1] = (byte)(0xff & getPacketnumber());		    	// running Packetnumber
			toSend[2] = (byte) BTHandler.CMD_ENABLE_BUZZER;  			// Command (upper part)
			toSend[3] = (byte) (BTHandler.CMD_ENABLE_BUZZER >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x00;					// length of payload
			byte[] crc = BTHandler.crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 5));
			toSend[5] = crc[0];
			toSend[6] = crc[1];
			toSend[7] = BTHandler.STOPBYTE;
			try {
				out.write(toSend);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public short getPacketnumber() {
		if(PACKETNUMBER >= 251) {
			PACKETNUMBER = 0;
		}
		return ++PACKETNUMBER;
	}
	
	
	public static BTSender getInstance(OutputStream o) {
		if(instance == null) {
			instance = new BTSender(o);
		}
		return instance;
	}
	
	public static void destroyInstance() {
		instance = null;
	}
}
