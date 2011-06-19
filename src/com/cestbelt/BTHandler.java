package com.cestbelt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.widget.Toast;

public class BTHandler extends Thread {

	private static BTHandler instance;
	private static BTSender sender;
	private Context parent;
	
	static final String NAME = "CorBeltServer";
	static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// UUID vom Device: 06-17 22:35:15.522: VERBOSE/CachedBluetoothDevice(2878):   00001101-0000-1000-8000-00805f9b34fb

	final int BUFFER_SIZE = 1024*1024*1;
	  
	
	/*
	 * Commands
	 */
	
	public static final short CMD_CLOSE_CONNECTION	  = 0x0000;
	public static final short CMD_PING 			  = 0x0001;
	public static final short CMD_RESET 			  = 0x00ff;
	public static final short CMD_PROTOCOL			  = 0x0100;
	public static final short CMD_ACKNOWLEDGE		  = 0x0200;
	public static final short CMD_NOT_ACKNOWLEDGE	  = 0x0300;
	public static final short CMD_REJECT			  = 0x0400;
	public static final short CMD_IDENTIFICATION 	  = 0x0500;
	public static final short CMD_SOFTWARE_VERSION   = 0x0501;
	public static final short CMD_SELF_TEST		  = 0x0600;
	public static final short CMD_CONFIG_EVENTS      = 0x0603;
	public static final short CMD_SET_TIME			  = 0x0610;
	public static final short CMD_CONF_REMOTE_EVENT  = 0x0611;
	public static final short CMD_CONF_MOVE_DETECT   = 0x0612;
	public static final short CMD_CONF_SECURE_MODE   = 0x0613;
	public static final short CMD_SNIFF_MODE_STATE   = 0x06c0;
	// Transmit Data
	public static final short CMD_TX_DATA_START      = 0x0700;
	public static final short CMD_TX_DATA_RUNNING    = 0x0701;
	public static final short CMD_TX_DATA_STOP       = 0x0705;
	public static final short CMD_TX_RECDATA_START   = 0x0710;
	public static final short CMD_TX_RECDATA_RUNNING = 0x0711;
	public static final short CMD_TX_RECDATA_STOP	  = 0x0715;
	// Request Data
	public static final short CMD_REQUEST_DATA		  = 0x0800;
	public static final short CMD_ENABLE_BUZZER	  = 0x0801;
	public static final short CMD_DISABLE_BUZZER	  = 0x0802;
	public static final short CMD_REQUEST_DATA_STOP  = 0x0805;
	
	/*
	 * Configs
	 */
	
	static final byte STARTBYTE 			  = (byte) 0xfc;
	static final byte STOPBYTE				  = (byte) 0xfd;
	
	private byte PACKETNUMBER				  = 0x0;
	
	
	BluetoothSocket sock;	
	BluetoothDevice remote;	  
	InputStream in;
	OutputStream out;

	private boolean RESET;
	private boolean SENDPING;

	private boolean SENDVERSION;

	private boolean SENDBUZZER;
	private boolean CONNECTED;
	private boolean RUNNING = true;
	
	public BTHandler(BluetoothDevice dev, Context p) {
		parent = p;
		try {
			/*Timer t = new Timer(); // Watchdog for failed connections
			t.schedule(new TimerTask(){
				@Override
				public void run() {
						//sock = null;
						try {
							sock.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						Toast.makeText(parent, "couldn't connect to the device", Toast.LENGTH_SHORT).show();
				}}, 5000); //Watchdog timeout = 5000 msec*/
			sock = dev.createInsecureRfcommSocketToServiceRecord(MY_UUID);
			sock.connect();
			//t.cancel(); // kill watchdog
			Toast.makeText(parent, "connected to " + dev.getAddress(), Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("got an exception");
			cleanUp();
		}
		remote = dev;
	}
	
	public Context getParent() {
		return parent;
	}


	public void setParent(Context parent) {
		this.parent = parent;
	}


	// Thread 
    public void run() {
    	
    	byte[] buffer = new byte[BUFFER_SIZE];
    	byte data = 0;
    	int ptr = 0;
  
    	try {		
			in = sock.getInputStream();
			out = sock.getOutputStream();
			sender = BTSender.getInstance(out);
			sender.start();
			
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
    	while (RUNNING) {
            if (in != null) {
            	try {
            		short length = 0;
            		short cmd = 0;
            		byte packetNumber = 0;
            		
					while(((data = (byte) in.read()) != -1) && RUNNING) {
						// Reset
						if(RESET) {
							cleanUp();
							return; // close this thread
						}
						
						
						buffer[ptr++] = data;
						switch(ptr) {
						case 2:
							packetNumber = (byte) buffer[1]; 
							break;
						case 4:
							// command
							// vertausch wgn. big endian �bertragung
							cmd = (short) (buffer[3] << 8);
							cmd |= (short) (buffer[2]);
							break;
						case 5:
							//length
							length = (short)(0xff & buffer[4]);
							break;
						}
						if ( ptr > 5 ) {
							//if( ptr == length + 5 + 3) {
							if(data == STOPBYTE) {
								// end of packet reached, execute
								byte[] crcCheck = crc16ccittCheck(Arrays.copyOfRange(buffer, 1, length + 5));
								if(crcCheck[0] == buffer[length + 5] && crcCheck[1] == buffer[length + 6]) {
									System.out.println("crc check ok");									
								} else {
									System.out.println("crc check fail");
									sendReject(packetNumber);
									ptr = 0;
									break;
								}
								
								switch(cmd) {
								case CMD_TX_DATA_START:
									System.out.println("income: CMD_TX_DATA_START");
									sendDataRequest();
									break;
								case CMD_TX_DATA_RUNNING:
									System.out.println("income: CMD_TX_DATA_RUNNING");
									sendAcknowledge(packetNumber);
									// TODO DATEN Auswerten
									break;
								case CMD_TX_DATA_STOP:
									System.out.println("income: CMD_TX_DATA_STOP");
									sendAcknowledge(packetNumber);
									break;
								case CMD_TX_RECDATA_START:
									System.out.println("income: CMD_TX_RECDATA_START");
									break;		
								case CMD_TX_RECDATA_RUNNING:
									System.out.println("income: CMD_TX_RECDATA_RUNNING");
									sendAcknowledge(packetNumber);
									// TODO DATEN Auswerten
									break;	
								case CMD_TX_RECDATA_STOP:
									System.out.println("income: CMD_TX_RECDATA_STOP");
									break;	
								case CMD_CLOSE_CONNECTION:
									System.out.println("income: CMD_CLOSE_CONNECTION");
									break;	
								case CMD_PING:
									System.out.println("income: CMD_PING");
									break;	
								case CMD_RESET:
									System.out.println("income: CMD_RESET");
									break;	
								case CMD_PROTOCOL:
									System.out.println("income: CMD_PROTOCOL");
									break;	
								case CMD_ACKNOWLEDGE:
									System.out.println("income: CMD_ACKNOWLEDGE");
									break;	
								case CMD_NOT_ACKNOWLEDGE:
									System.out.println("income: CMD_NOT_ACKNOWLEDGE");
									break;	
								case CMD_IDENTIFICATION:
									System.out.println("income: CMD_IDENTIFICATION");
									break;	
								case CMD_SOFTWARE_VERSION:
									System.out.println("income: CMD_SOFTWARE_VERSION");
									break;	
								case CMD_SELF_TEST:
									System.out.println("income: CMD_SELF_TEST");
									break;	
								case CMD_CONF_REMOTE_EVENT:
									System.out.println("income: CMD_CONF_REMOTE_EVENT");
									break;	
								case CMD_SET_TIME:
									System.out.println("income: CMD_SET_TIME");
									break;	
								case CMD_CONF_MOVE_DETECT:
									System.out.println("income: CMD_CONF_MOVE_DETECT");
									break;	
								case CMD_CONF_SECURE_MODE:
									System.out.println("income: CMD_CONF_SECURE_MODE");
									break;	
								case CMD_SNIFF_MODE_STATE:
									System.out.println("income: CMD_SNIFF_MODE_STATE");
									break;	
								case CMD_ENABLE_BUZZER:
									System.out.println("income: CMD_ENABLE_BUZZER");
									break;	
								case CMD_DISABLE_BUZZER:
									System.out.println("income: CMD_DISABLE_BUZZER");
									break;	
								case CMD_REQUEST_DATA_STOP:
									System.out.println("income: CMD_REQUEST_DATA_STOP");
									break;	
								case CMD_REQUEST_DATA:
									System.out.println("income: CMD_REQUEST_DATA");
									break;	
								case CMD_REJECT:
									System.out.println("income: CMD_REJECT");
									break;	
								default:
									System.out.println("error: unknown command");
									sendReject(packetNumber);
								}
								ptr = 0;
							}
						}
					}
				} catch (IOException e) {
					ptr = 0;
					// TODO Auto-generated catch block
					e.printStackTrace();
					cleanUp();
					destroyInstance();
					return;
				}            	
            }
            ptr = 0;
            System.out.println(buffer.toString());
        }
    	sender.interrupt();
    }
    
	private void cleanUp() {
		if(this.equals(instance)) {
			try {
				in.close();
				out.flush();
				out.close();
				sock.close();
				
			} catch (IOException e) {
				//e.printStackTrace();
				return;
			}
		}
		
	}

	/*
	 * CRC16 check
	 */

	public static byte[] crc16ccittCheck(byte[] bytes) { 
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12) 

        // byte[] testBytes = "123456789".getBytes("ASCII");

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
             }
        }

        crc &= 0xffff;
        System.out.println("CRC16-CCITT = " + Integer.toHexString(crc));
        byte[] ret = new byte[2];

        ret[1] = (byte) (crc >> 8);
        ret[0] = (byte) (crc);
        return ret;
    }

	/*
	 * CRC16 end
	 */

	private synchronized void sendDataRequest() {
		if(out != null) {
			byte[] toSend = new byte[10];
			System.out.println("outgoing: CMD_REQUEST_DATA");
			toSend[0] = STARTBYTE;						// Startflag
			toSend[1] = getPacketnumber();				// running Packetnumber
			toSend[2] = (byte) CMD_REQUEST_DATA;  // Command (upper part)
			toSend[3] = (byte) (CMD_REQUEST_DATA >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x02;					// length of payload
			toSend[5] = (byte) CMD_TX_DATA_RUNNING;// payload
			toSend[6] = (byte) (CMD_TX_DATA_RUNNING >> 8);	// payload
			byte[] crc = crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 7));
			toSend[7] = crc[0];
			toSend[8] = crc[1];
			toSend[9] = STOPBYTE;
			try {
				out.write(toSend);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	private synchronized void sendPing() {
		if(out != null) {
			byte[] toSend = new byte[8];
			System.out.println("outgoing: CMD_PING");
			toSend[0] = STARTBYTE;						// Startflag
			toSend[1] = getPacketnumber();				// running Packetnumber
			toSend[2] = (byte) CMD_PING;  // Command (upper part)
			toSend[3] = (byte) (CMD_PING >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x00;
			byte[] crc = crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 5));
			toSend[5] = crc[0];
			toSend[6] = crc[1];
			toSend[7] = STOPBYTE;
			try {
				out.write(toSend);
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
			toSend[0] = STARTBYTE;						// Startflag
			toSend[1] = getPacketnumber();				// running Packetnumber
			toSend[2] = (byte) CMD_RESET;  // Command (upper part)
			toSend[3] = (byte) (CMD_RESET >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x00;					// length of payload
			byte[] crc = crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 5));
			toSend[5] = crc[0];
			toSend[6] = crc[1];
			toSend[7] = STOPBYTE;
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
			toSend[0] = STARTBYTE;						// Startflag
			toSend[1] = getPacketnumber();				// running Packetnumber
			toSend[2] = (byte) CMD_ACKNOWLEDGE;  		// Command (upper part)
			toSend[3] = (byte) (CMD_ACKNOWLEDGE >> 8);	// Command (lower part)
			toSend[4] = (byte) 0x01;					// length of payload
			toSend[5] = packetNumber;
			byte[] crc = crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 6));
			toSend[6] = crc[0];
			toSend[7] = crc[1];
			toSend[8] = STOPBYTE;
			try {
				out.write(toSend);
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
			toSend[0] = STARTBYTE;						// Startflag
			toSend[1] = getPacketnumber();				// running Packetnumber
			toSend[2] = (byte) CMD_REJECT;  			// Command (upper part)
			toSend[3] = (byte) (CMD_REJECT >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x01;					// length of payload
			toSend[5] = packetNumber;
			byte[] crc = crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 6));
			toSend[6] = crc[0];
			toSend[7] = crc[1];
			toSend[8] = STOPBYTE;
			try {
				out.write(toSend);
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
			toSend[0] = STARTBYTE;						// Startflag
			toSend[1] = getPacketnumber();		    	// running Packetnumber
			toSend[2] = (byte) CMD_SOFTWARE_VERSION;  			// Command (upper part)
			toSend[3] = (byte) (CMD_SOFTWARE_VERSION >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x00;					// length of payload
			byte[] crc = crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 5));
			toSend[5] = crc[0];
			toSend[6] = crc[1];
			toSend[7] = STOPBYTE;
			try {
				out.write(toSend);
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
			toSend[0] = STARTBYTE;						// Startflag
			toSend[1] = getPacketnumber();		    	// running Packetnumber
			toSend[2] = (byte) CMD_ENABLE_BUZZER;  			// Command (upper part)
			toSend[3] = (byte) (CMD_ENABLE_BUZZER >> 8);		// Command (lower part)
			toSend[4] = (byte) 0x00;					// length of payload
			byte[] crc = crc16ccittCheck(Arrays.copyOfRange(toSend, 1, 5));
			toSend[5] = crc[0];
			toSend[6] = crc[1];
			toSend[7] = STOPBYTE;
			try {
				out.write(toSend);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void resetCestbelt() {
		RESET = true;
	}
	
	
	public byte getPacketnumber() {
		if(PACKETNUMBER >= 251) {
			PACKETNUMBER = 0;
		}
		return ++PACKETNUMBER;
	}
	
	/**
	 * 
	 * @param dev
	 * @return
	 */
	public static BTHandler getInstance(BluetoothDevice dev, Context p) {
		if (instance == null) {
			instance = new BTHandler(dev,p);
		}
		return instance;
	}

	public void sendPingRequest() {
		if(sender != null) sender.sendPingRequest();
	}

	public void sendVersionRequest() {
		if(sender != null) sender.sendVersionRequest();
	}

	public void sendResetRequest() {
		if(sender != null) sender.sendResetRequest();
		RUNNING = false;
	}

	public void sendBuzzerRequest() {
		if(sender != null) sender.sendBuzzerRequest();
	}
	
	public static void destroyInstance() {
		instance = null;
	}
	
	
}
