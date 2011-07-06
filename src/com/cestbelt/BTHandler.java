package com.cestbelt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

public class BTHandler extends Thread {

	// the singleton instance
	private static BTHandler instance;
	// sender singleton instance
	private static BTSender sender;
	
	// the parent activity for gui output
	private Context parent;
	
	// the name for the BT RFCOMM Service
	static final String NAME = "CorBeltServer";
	// UUID for the RFCOMM Service
	static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// UUID vom Device: 06-17 22:35:15.522: VERBOSE/CachedBluetoothDevice(2878):   00001101-0000-1000-8000-00805f9b34fb

	// the buffer size for the income buffer
	final int BUFFER_SIZE = 1024*4;
	  
	// TODO Keep Alive Timer mit Ping CMD
	
	/*
	 * Commands - see specification for further information
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
	
	// the choosen remote device
	BluetoothDevice remote;
	// BT I/O Streams
	BluetoothSocket sock;	  
	InputStream in;
	OutputStream out;
	
	// pulseoutput
	private SurfaceView displayPanel;

	// variable for resetting the connection
	private boolean RESET;
	
	// variable for thread running
	private boolean RUNNING = true;
	
	// for answering PING Requests
	private boolean PING_SEND = false;
	
	// mediaplayer for playing the sound (ping) 
	private MediaPlayer mMediaPlayer;
	
	/**
	 * Constructor
	 * @param dev the remote Bluetooth device
	 * @param p the parent activity
	 */
	public BTHandler(BluetoothDevice dev, Context p) {
		parent = p;
		remote = dev;
	}	
	
	/* Setter / Getter */
	
	public SurfaceView getDisplayPanel() {
		return displayPanel;
	}

	public void setDisplayPanel(SurfaceView surfaceView) {
		this.displayPanel = surfaceView;
	}

	public Context getParent() {
		return parent;
	}


	public void setParent(Context parent) {
		this.parent = parent;
	}


	// Thread 
    public void run() {
    	
    	this.setName("BT Handler");
    	// create local buffer
    	final byte[] buffer = new byte[BUFFER_SIZE];
    	// last read byte
    	byte data = 0;
    	// arraypointer
    	int ptr = 0;
    	
    	try {	
    		// try to create a new insecure connection
			sock = remote.createInsecureRfcommSocketToServiceRecord(MY_UUID);
    		// and connect
    		sock.connect();
			// send a msg for the user 
    		((Activity)parent).runOnUiThread(new Runnable() {
    		    public void run() {
    		        Toast.makeText(parent, "connected to " + remote.getAddress(), Toast.LENGTH_SHORT).show();
    		    }
    		});
    		// set all strams
			in = sock.getInputStream();
			out = sock.getOutputStream();
			// create a sender thread and run it
			sender = BTSender.getInstance(out);
			sender.start();
			
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		// endless loop (till RUNNING = true)
    	while (RUNNING) {
            if (in != null) {
            	try {
            		// local variables for the current packet
            		short length = 0; // packetlength
            		short cmd = 0; // command in this packet
            		byte packetNumber = 0; // # of this packet
            		
					while(((data = (byte) in.read()) != -1) && RUNNING) {
						// Reset
						if(RESET) {
							cleanUp();
							return; // close this thread
						}
						
						buffer[ptr++] = data;
						switch(ptr) {
						case 2:
							// packet number read
							packetNumber = (byte) buffer[1]; 
							break;
						case 4:
							// command
							// vertausch wgn. big endian uebertragung
							cmd = (short) (buffer[3] << 8);
							cmd |= (short) (buffer[2]);
							break;
						case 5:
							//length
							length = (short)(0xff & buffer[4]);
							break;
						}
						if ( ptr > 5 ) {
							if(data == STOPBYTE) {
								// end of packet reached, execute
								// check the packet with the crc check
								byte[] crcCheck = crc16ccittCheck(Arrays.copyOfRange(buffer, 1, length + 5));
								if(crcCheck[0] == buffer[length + 5] && crcCheck[1] == buffer[length + 6]) {
									// packet ok
									System.out.println("crc check ok");									
								} else {
									System.out.println("crc check fail");
									// check failed - rejeck current packet
									sender.sendReject(packetNumber);
									// set pointer to start
									ptr = 0;
									break;
								}
								
								// execute the Command
								switch(cmd) {
								case CMD_TX_DATA_START:
									System.out.println("income: CMD_TX_DATA_START");
									// cestbelt request for sending new data
									sender.sendDataRequest();
									break;
								case CMD_TX_DATA_RUNNING:
									System.out.println("income: CMD_TX_DATA_RUNNING");
									// data valid - ACK it
									sender.sendAcknowledge(packetNumber);
									// read out just the pulse value at index 220
									if(buffer[220] > 0) {
										// value is available ( > 0 )
							    		((Activity)parent).runOnUiThread(new Runnable() {
							    		    public void run() {
							    		    	// update the user interface
									    		TextView v = (TextView) ((Activity) parent).findViewById(R.id.txtPulse);
					            	        	v.setText("current pulse: " + String.valueOf((int)buffer[220]));
					            	        	if(displayPanel != null) {
													((Panel)displayPanel).addPulseValue((int)buffer[220]);
												}
					            	        	// beep
					            	        	playAudio();
						    		        }
							    		});
									}						
									break;
								case CMD_TX_DATA_STOP:
									System.out.println("income: CMD_TX_DATA_STOP");
									sender.sendAcknowledge(packetNumber);
									break;
								case CMD_TX_RECDATA_START:
									System.out.println("income: CMD_TX_RECDATA_START");
									break;		
								case CMD_TX_RECDATA_RUNNING:
									// see @CMD_TX_DATA_RUNNING
									System.out.println("income: CMD_TX_RECDATA_RUNNING");
									sender.sendAcknowledge(packetNumber);
									System.out.println("rate: " + buffer[219]);
									if(buffer[220] > 0) {
							    		((Activity)parent).runOnUiThread(new Runnable() {
							    		    public void run() {
									    		TextView v = (TextView) ((Activity) parent).findViewById(R.id.txtPulse);
					            	        	v.setText("current pulse: " + String.valueOf((int)buffer[220]));
					            	        	if(displayPanel != null) {
													((Panel)displayPanel).addPulseValue((int)buffer[220]);

												}
					            	        	playAudio();
						    		        }
							    		});
									}
									break;	
								case CMD_TX_RECDATA_STOP:
									sender.sendAcknowledge(packetNumber);
									System.out.println("income: CMD_TX_RECDATA_STOP");
									break;	
								case CMD_CLOSE_CONNECTION:
									// unused by cestbelt
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
									if(this.PING_SEND) {
										this.PING_SEND = false;
										((Activity)parent).runOnUiThread(new Runnable() {
							    		    public void run() {
							    		    	Toast.makeText(parent, "Pong", Toast.LENGTH_SHORT).show();
						    		        }
							    		});
									}
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
									// cestbelt is in idyle - start sending data
									if(sender != null) sender.sendPulseRequest();
									break;	
								case CMD_REJECT:
									System.out.println("income: CMD_REJECT");
									break;	
								default:
									System.out.println("error: unknown command");
									sender.sendReject(packetNumber);
								}
								ptr = 0;
							}
						}
					}
				} catch (IOException e) {
					// clean up the local environment
					ptr = 0; // reset pointer
					cleanUp(); // close connection
					destroyInstance(); // destroy singleton instance
					return;
				}            	
            }
            ptr = 0;
           
        }
    	// thread should carefully end
    	// close the local streams and sockets
		cleanUp();
		destroyInstance();
    }
    
	private void cleanUp() {
		if(this.equals(instance)) {
			try {
				if(sender != null) {
			    	// end the sender thread
			    	sender.interrupt();
				}
				if(in != null) {
					// close InputStream
					in.close();
				}
				// watchdog if thread get deadlocked 
				Timer wd = new Timer();
				wd.schedule(new TimerTask(){

					@Override
					public void run() {
						// hard killing thread
						instance.interrupt();
						instance = null;
						// restart BT adapter
						BluetoothAdapter.getDefaultAdapter().disable();
						BluetoothAdapter.getDefaultAdapter().enable();
						
					}}, 3000);
				// TODO manchmal klemmt er hier wenn er ins onDestroy kippt
				if( sock != null) {
					// close the socket
					sock.close();						
				}
				// kill the watchdag if all went ok
				wd.cancel();
				wd.purge();
			} catch (Exception e) {
				return;
			}
		}
		
	}

	/**
	 * CRC16 check (CCITT)
	 */
	public static byte[] crc16ccittCheck(byte[] bytes) { 
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12) 

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
	
	/**
	 * Get the Thread to Reset the cestbelt and itself
	 */
	public void resetCestbelt() {
		RESET = true;
	}
	
	/**
	 * Increment the packetNumber and return the new value
	 * @return the new packetNumber
	 */
	public byte getPacketnumber() {
		if(PACKETNUMBER >= 251) {
			PACKETNUMBER = 0;
		}
		return ++PACKETNUMBER;
	}
	
	/**
	 * Singleton implementation
	 * @param dev the remote BT Device
	 * @param p the parent activity
	 * @return the Singleton instance
	 */
	public static BTHandler getInstance(BluetoothDevice dev, Context p) {
		if (instance == null) {
			instance = new BTHandler(dev,p);
		}
		return instance;
	}
	
	/* send Request through the sender */
	public void sendPingRequest() {
		this.PING_SEND = true;
		if(sender != null) sender.sendPingRequest();
	}
	
	public void sendPulseRequest() {
		if(sender != null) sender.sendPulseRequest();
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
	
	/**
	 * kill this instance
	 */
	public static void destroyInstance() {
		instance = null;
	}

	/**
	 * Carefull close this thread ans subthread
	 */
	public void closeConnection() {
		RUNNING = false;
		if(sender != null) {
			sender.interrupt();
		}
		cleanUp();
	}
	
	/**
	 * Simply plays the beep sound when new data is income
	 */
	private void playAudio() {
		try {
			if(mMediaPlayer == null) {
				// http://www.soundjay.com/beep-sounds-1.html lots of free beeps here
				mMediaPlayer = MediaPlayer.create(parent, R.raw.beep1);
				mMediaPlayer.setLooping(false);
				mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
					public void onCompletion(MediaPlayer arg0) {
						//finish();
					}
				});
			}
			mMediaPlayer.start();

		} catch (Exception e) {
		}
	}
	
}
