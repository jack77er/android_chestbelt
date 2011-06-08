package com.cestbelt.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

public class BTHandler extends Thread {

	private static BTHandler instance;

	static final String NAME = "CorBeltServer";
	static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	BluetoothSocket sock;
	
	BluetoothDevice remote;
	
	public BTHandler(BluetoothDevice dev) {
		try {
				sock = dev.createInsecureRfcommSocketToServiceRecord(MY_UUID);
				sock.connect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		remote = dev;
	}
	
	// Thread 
    public void run() {

    	final int BUFFER_SIZE = 1024*8;
    	
    	byte[] buffer = new byte[BUFFER_SIZE];
    	byte data = 0;
    	int ptr = 0;
  
    	InputStream in;
    	OutputStream out;
    	
    	try {
			//sock.connect();
			
			in = sock.getInputStream();
			out = sock.getOutputStream();
			
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
    	while (true) {
            if (in != null) {
            	try {
            		byte length = 0;
            		short cmd = 0;
            		
					while((data = (byte) in.read()) != -1) {
						
						buffer[ptr++] = data;
						switch(ptr) {
						case 4:
							// command
							cmd = (short) (buffer[2] << 8);
							cmd |= (short) (buffer[3]);
							break;
						case 5:
							//length
							length = buffer[4];
							break;
						}
						if ( ptr > 5 ) {
							if( ptr == length + 5 + 3) {
								// end of paket reached, execute
								checkPaket();
								// TODO hier weiter machen , ACK senden und so nen kram
							}
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}            	
            }
            ptr = 0;
            System.out.println(buffer.toString());
        }
    }
    
	private void checkPaket() {
		
		
	}

	/*
	 * CRC16 check
	 */

	private static short polynomial = (short) 0x1021;
	private short crc = (short) 0xFFFF;;

	/**
	 * Feed a bitstring to the crc calculation (0 < length <= 32).
	 */
	public void add_bits(int bitstring, int length) {
		int bitmask = 1 << (length - 1);
		do
			if (((crc & 0x8000) == 0) ^ ((bitstring & bitmask) == 0)) {
				crc <<= 1;
				crc ^= polynomial;
			} else
				crc <<= 1;
		while ((bitmask >>>= 1) != 0);
	}

	/**
	 * Return the calculated checksum. Erase it for next calls to add_bits().
	 */
	public short checksum() {
		short sum = crc;
		crc = (short) 0xFFFF;
		return sum;
	}

	/*
	 * CRC16 end
	 */

	/**
	 * 
	 * @param dev
	 * @return
	 */
	public static BTHandler getInstance(BluetoothDevice dev) {
		if (instance == null) {
			instance = new BTHandler(dev);
		}
		return instance;
	}
}
