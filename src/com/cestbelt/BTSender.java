package com.cestbelt;

import java.io.IOException;
import java.io.OutputStream;

import com.cestbelt.BTHandler;

import java.util.ArrayList;
import java.util.Arrays;

public class BTSender extends Thread {

	// Singleton instance
	static BTSender instance;
	// Object for thread synchronisation
	public Object synchonizer = new Object();

	// Commands for sending stored as an enum
	public static enum sendCMD { SENDPING, SENDVERSION, SENDBUZZER, SENDACK, SENDREQUEST, SENDRESET, SENDPULSE }; 
	
	// the BT outputstream
	OutputStream out;
	// tje joblist - globaly defined
	ArrayList<sendCMD> jobs;

	// running packetNumber
	private short PACKETNUMBER;
	// running thread variable
	private boolean RUNNING;
	
	/**
	 * Constructor 
	 * @param o the OutputStream from the BT Socket
	 */
	public BTSender(OutputStream o) {
		out = o;
		// initiate the Joblist
		jobs = new ArrayList<sendCMD>();
		// set Thread running to true
		RUNNING = true;
	}
	
	/* GETTER / SETTER */
	public Object getSynchonizer() {
		return synchonizer;
	}

	public void setSynchonizer(Object synchonizer) {
		this.synchonizer = synchonizer;
	}
	
	// Thread
	public void run() {
		this.setName("BT Sender");
		if(out == null) {
			throw new IllegalArgumentException("no output stream referenced");
		}
		// infinite loop
		while(RUNNING) {
			// Execute jobs if needed
			while(jobs.size() > 0) {
				executeJob(jobs.get(0));
				jobs.remove(0);
			}
			
			try {
				// wait at synchronizer if all jobs done
				synchronized (synchonizer) {
					synchonizer.wait();
				}
			} catch (InterruptedException e) {
			}
		}
		// thread ended carefull
		try {
			// flush and close outputstream
			out.flush();
			out.close();
		} catch (IOException e) {
			out = null;
		}		
		destroyInstance();
	}

	/**
	 * execute a Job from the Joblist
	 * @param s
	 */
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
		case SENDPULSE:
			sendPulse();
			break;
		}

	}
	/**
	 * stop the thread carefully
	 */
	@Override
	public synchronized void interrupt() {
		if(RUNNING) {
			RUNNING  = false;
			synchronized(synchonizer) {
				synchonizer.notifyAll();
			}
		} else {
			try {
				throw new InterruptedException("thread already requested to interrupt!");
			} catch (InterruptedException e) {
			}
		}
		
	}


	/**
	 * send the command to the cestbelt to send new pulse data
	 */
	synchronized void sendDataRequest() {
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
	
	/**
	 * add a new Job to the Joblist
	 */
	public void sendPingRequest() {
		jobs.add(sendCMD.SENDPING);	
		synchronized(synchonizer) {
			synchonizer.notifyAll();
		}
	}

	/**
	 * add a new Job to the Joblist
	 */
	public void sendVersionRequest() {
		jobs.add(sendCMD.SENDVERSION);
		synchronized(synchonizer) {
			synchonizer.notifyAll();
		}
	}

	/**
	 * add a new Job to the Joblist
	 */
	public void sendResetRequest() {
		jobs.add(sendCMD.SENDRESET);			
		synchronized(synchonizer) {
			synchonizer.notifyAll();
		}
	}

	/**
	 * add a new Job to the Joblist
	 */
	public void sendBuzzerRequest() {
		jobs.add(sendCMD.SENDBUZZER);			
		synchronized(synchonizer) {
			synchonizer.notifyAll();
		}
	}

	/**
	 * add a new Job to the Joblist
	 */
	public void sendPulseRequest() {
		jobs.add(sendCMD.SENDPULSE);			
		synchronized(synchonizer) {
			synchonizer.notifyAll();
		}
	}
	
	/**
	 * send the Ping Command to the cestbelt
	 */
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
				
			}
			
		}
	}

	/**
	 * send a reset command to the cestbelt
	 */
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
			
			}
		}
	}
	

	/**
	 * send a request to the cestbelt for initiate a new sending of data
	 */
	private synchronized void sendPulse() {
		if(out != null) {
			byte[] toSend = new byte[10];
			System.out.println("outgoing: CMD_REQUEST_DATA (CMD_TX_DATA_START)");

			toSend[0] = BTHandler.STARTBYTE;						// Startflag
			toSend[1] = (byte) (0xff & getPacketnumber());				// running Packetnumber
			toSend[2] = (byte) BTHandler.CMD_REQUEST_DATA;  // Command (upper part)
			toSend[3] = (byte) (BTHandler.CMD_REQUEST_DATA >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x02;					// length of payload
			toSend[5] = (byte) BTHandler.CMD_TX_DATA_START;// payload
			toSend[6] = (byte) (BTHandler.CMD_TX_DATA_START >> 8);	// payload
			byte[] crc = BTHandler.crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 7));
			toSend[7] = crc[0];
			toSend[8] = crc[1];
			toSend[9] = BTHandler.STOPBYTE;
			try {
				out.write(toSend);
				out.flush();
			} catch (IOException e) {
			}
			
		}
	}
	
	/**
	 * sent an ACK to the cestbelt with the given packetnumber
	 * @param packetNumber the number to ACK
	 */
	synchronized void sendAcknowledge(byte packetNumber) {
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
	
	/**
	 * reject the <code>packetNumber</code> packet
	 * @param packetNumber
	 */
	synchronized void sendReject(byte packetNumber) {
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
	
	/**
	 * crap
	 */
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
	
	/**
	 * enable the beeper on the cestbelt
	 */
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
	
	/**
	 * increment and return the new packetnumber
	 * @return
	 */
	public short getPacketnumber() {
		if(PACKETNUMBER >= 251) {
			PACKETNUMBER = 0;
		}
		return ++PACKETNUMBER;
	}
	
	/**
	 * Singleton
	 * @param o
	 * @return
	 */
	public static BTSender getInstance(OutputStream o) {
		if(instance == null) {
			instance = new BTSender(o);
		}
		return instance;
	}
	
	public static void destroyInstance() {
		instance = null;
	}
	
	public synchronized void closeConnection() {
		interrupt();		
	}
}
