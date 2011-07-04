package com.cestbelt;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class cest_activity extends Activity {
	// final Context for forwading to subclasses
	private final Context mainContext = this;
	// global definition of the BluetoothAdapter
	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	
	// int for the BT Intent
	public final static int REQUEST_ENABLE_BT = 0;
	
	// status flags for recovering bluetooth connections
	private boolean btWasEnabled = true;
	private boolean btisEnabled = false;
	
	// int for the BT waiting dialog
	static final int DIALOG_BT_SCANNING = 1;
	
	// flags for the manual connection 
	private boolean MANUAL_CONNECT = false;
	private boolean MANUAL_CONNECT_FOUND = false;
	
	// global definition of the pulseview for forwarding
	private SurfaceView surfaceView;
	
	// global list of found remote devices
	private ArrayList<BluetoothDevice> remoteDevices;
	
	// the used device
	private BluetoothDevice choosenDevice;

	
    /** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// create a List for all found devices
		remoteDevices = new ArrayList<BluetoothDevice>();
		
		/* Intent filters for all used intents */
		// Bluetoothdevice found
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(bcastReceiver, filter); 
		// Bluetoothstate changed
		filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(bcastReceiver, filter);
		// Bluetoothdiscovery finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(bcastReceiver, filter); 
		
		// recover old device from shared preferences
		SharedPreferences p = getSharedPreferences("prevBelt", MODE_PRIVATE);
		String oldMAC = p.getString("prevMAC", "00:00:00:00:00:00");

		if (!oldMAC.equals("00:00:00:00:00:00")) {
			// preferences found
			if (adapter.isEnabled() && adapter.getState() == adapter.STATE_ON
					&& !adapter.isDiscovering())
				choosenDevice = adapter.getRemoteDevice(oldMAC);
		}
		
		// set primary content view 
		setContentView(R.layout.main);
		
		// find pulsepanel and set it as the global one
		final Panel surfaceView = (Panel) findViewById(R.id.SurfaceView);
		if (surfaceView != null) {
			this.surfaceView = surfaceView;
		}

		// Actionlistener for the Rescan Button
		((Button) findViewById(R.id.btnRescan))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						scanForDevices();
					}
				});

		// Actionlistener for the Ping button
		((Button) findViewById(R.id.btnPing))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						try {
							BTHandler.getInstance(choosenDevice, mainContext)
									.sendPingRequest();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});

		// Actionlistener for the pulse request button
		((Button) findViewById(R.id.btnVersion))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						try {
							BTHandler.getInstance(choosenDevice, mainContext)
									.sendPulseRequest();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});

		// Actionlistener for the reset button
		((Button) findViewById(R.id.btnReset))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						try {
							BTHandler.getInstance(choosenDevice, mainContext)
									.sendResetRequest();
							BTHandler.destroyInstance();

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});

		// Actionlistener for the connect button
		((Button) findViewById(R.id.btnConnect))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						try {
							if (choosenDevice != null) {
								// if choosen device is not empty try a scan
								// and detect the device
								MANUAL_CONNECT = true;
								MANUAL_CONNECT_FOUND = false;
								scanForDevices();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});

		// Actionlistener for the beep button
		((Button) findViewById(R.id.btnBuzzer))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						try {
							BTHandler.getInstance(choosenDevice, mainContext)
									.sendBuzzerRequest();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});

		// Actionlistener for the pulseview button
		((Button) findViewById(R.id.btnPulseActivity))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						try {
							// add some crappy values
							for (int i = 0; i < 100; i++) {
								surfaceView.addPulseValue(50);
								surfaceView.addPulseValue(50);
								surfaceView.addPulseValue(50);
								surfaceView.addPulseValue(50);
								surfaceView.addPulseValue(90);
							}
						} catch (Exception e) {
							e.printStackTrace();

						}
					}
				});


		// Actionlistener for the close connection button
		((Button) findViewById(R.id.btnDisconnect))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						try {
							BTHandler.getInstance(choosenDevice, mainContext)
									.closeConnection();

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});

		// last action in onCreate: enable Bluetooth if necessary  
		if (!adapter.isEnabled()) {
			// bluetooth disabled
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

	}
    
    @Override
    protected void onResume() {
    	super.onResume();
    	// recover last used device
        SharedPreferences p = getSharedPreferences("prevBelt", MODE_PRIVATE);
        String oldMAC = p.getString("prevMAC", "00:00:00:00:00:00");
        
        if(!oldMAC.equals("00:00:00:00:00:00")) {
        	choosenDevice = adapter.getRemoteDevice(oldMAC);
        	TextView v = (TextView) findViewById(R.id.txtDev);
        	v.setText("used device: " + oldMAC);
        } else {
        	TextView v = (TextView) findViewById(R.id.txtDev);
        	v.setText("used device: none");
        }
    }
    
    protected void scanForDevices() {
    	// if bluetooth is disabled
		if(!adapter.isEnabled()) {
    		// bluetooth disabled - enable it
    		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);    	
    	} else {		    		
    		// interrupt discovery if already started
    		adapter.cancelDiscovery();
    		if(adapter.startDiscovery()) {
    			// scanning started - lets show the waiting dialog
    			showDialog(DIALOG_BT_SCANNING);
    		} else {
    			// fallback
    			Toast.makeText(getApplicationContext(), "scan failed", Toast.LENGTH_SHORT).show();
    		}
    	}	
	}
    
    @Override
    protected void onPause() {
    	super.onPause();
    	// save last used device
    	SharedPreferences p = getSharedPreferences("prevBelt", MODE_PRIVATE);
    	SharedPreferences.Editor e = p.edit();
    	if(choosenDevice != null) {
    		e.putString("prevMAC", choosenDevice.getAddress());
        	e.commit();
    	}    	
    }

	@Override
    protected void onDestroy() {

		super.onDestroy();
		
		// clean all registered receivers
		unregisterReceiver(bcastReceiver);
        
		// save used device in shared preferences
    	SharedPreferences p = getSharedPreferences("prevBelt", MODE_PRIVATE);
    	SharedPreferences.Editor e = p.edit();
    	if(choosenDevice != null) {
    		e.putString("prevMAC", choosenDevice.getAddress());
        	e.commit();
    	}
        // close the bluetooth connection if necessary
    	BTHandler.getInstance(choosenDevice, mainContext).closeConnection();
    	BTHandler.destroyInstance();
    	
        if(!btWasEnabled) {
        	// quick and dirty
        	//adapter.disable();
        }

        
	}

	/**
	 * onStop Event (see Android Documentation)
	 */
	protected void onStop() {
		super.onStop();
		// save used device in shared preferences
    	SharedPreferences p = getSharedPreferences("prevBelt", MODE_PRIVATE);
    	SharedPreferences.Editor e = p.edit();
    	if(choosenDevice != null) {
    		e.putString("prevMAC", choosenDevice.getAddress());
        	e.commit();
    	}
	}
    /**
     * Method for creating dialogs
     */
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_BT_SCANNING) {
			// bluetooth waiting dialog
			ProgressDialog loadingDialog = new ProgressDialog(this){
				@Override
				public void onBackPressed(){
					// discovery was aborted by the user
					adapter.cancelDiscovery();
				}
				
			};
			loadingDialog.setMessage("searching for bluetooth devices...");
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(true);
			
			return loadingDialog;
		}
		return super.onCreateDialog(id);
	}

	/**
	 * The receiver for all incoming broadcast from the OS
	 */
    private final BroadcastReceiver bcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // extract the found device out of this intent
            try {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			} catch (Exception e1) {
				// nothing found
			}
            
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            	// bluetooth state changed
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                case BluetoothAdapter.STATE_ON:
                	// bt is on
                	Toast.makeText(getApplicationContext(), "bt enabled", Toast.LENGTH_SHORT).show();
                	btWasEnabled = false;
                	btisEnabled = true;
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                	// bt is turning off
                	Toast.makeText(getApplicationContext(), "bt disabled", Toast.LENGTH_SHORT).show();
                	btisEnabled = false;
                    break;
                }
            } else if(action.equals(BluetoothDevice.ACTION_FOUND)) {
            	// device found during discovery
            	if(!remoteDevices.contains((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE))) {
            		// device not already in list
            		BluetoothDevice tmp = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            		// get the device (again...)
            		if(MANUAL_CONNECT && tmp.getAddress().equals(choosenDevice.getAddress())) {
            			// should directly connect if manual connect 
            			MANUAL_CONNECT_FOUND = true;
            			// stop discovery
            			adapter.cancelDiscovery();
            			// close waiting dialog
            			dismissDialog(DIALOG_BT_SCANNING);
            			choosenDevice = tmp;
            			// connect to the device
            			connectToDevice();
            		}
            		// add my device to the global list
            		remoteDevices.add(tmp);
            	}
            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
            	if(MANUAL_CONNECT) {
            		if(!MANUAL_CONNECT_FOUND) {
            			// previous device not found
            			Toast.makeText(getApplicationContext(), "device not found... please rescan", Toast.LENGTH_SHORT).show();
            		}            		
            		MANUAL_CONNECT = false;
            		MANUAL_CONNECT_FOUND = false;
        			// close waiting dialog
            		dismissDialog(DIALOG_BT_SCANNING);
            	} else {
            		// new scan completed - create a list to choose one
	            	AlertDialog.Builder builder = new AlertDialog.Builder(mainContext);
	            	builder.setTitle("Choose a Device");
	            	
	            	final CharSequence[] items = new CharSequence[remoteDevices.size()];
	            	for(int i = 0 ; i < remoteDevices.size() ; i++) {
	            		items[i] = remoteDevices.get(i).getName();
	            	}
	            	
	            	builder.setItems(items, new DialogInterface.OnClickListener() {
	            	    public void onClick(DialogInterface dialog, int item) {
	            	    	// device from list choosen - add it to the lastuse preference
	            	        choosenDevice = remoteDevices.get(item);
	            	    	SharedPreferences p = getSharedPreferences("prevBelt", MODE_PRIVATE);
	            	    	SharedPreferences.Editor e = p.edit();
	            	    	if(choosenDevice != null) {
	            	    		e.putString("prevMAC", choosenDevice.getAddress());
	            	        	e.commit();
	            	        	TextView v = (TextView) findViewById(R.id.txtDev);
	            	        	v.setText("used device: " + choosenDevice.getAddress());
	            	    	}
	            	    	// connect to the found device
	            	        connectToDevice();
	            	    }
	            	});

        			// close waiting dialog
	            	dismissDialog(DIALOG_BT_SCANNING);
	            	// show alert
	            	AlertDialog alert = builder.create();
	            	alert.show();
            	}
            } 
        }
    };
    
    /**
     * creates a list of all paired devices
     */
    private void listPairedDevices() {
    	final Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
    	// If there are paired devices
    	if (pairedDevices.size() > 0) {
    	    // Loop through paired devices
    		
    		AlertDialog.Builder builder = new AlertDialog.Builder(mainContext);
        	builder.setTitle("Choose paired a Device");
        	
        	final CharSequence[] items = new CharSequence[pairedDevices.size()];
        	
        	int i = 0;
        	for (BluetoothDevice device : pairedDevices) {
    	        // Add the name and address to an array adapter to show in a ListView
        		items[i] = device.getName();
        		i++;
    	    }
        	
        	builder.setItems(items, new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int item) {
        	    	int i = 0;
                	for (BluetoothDevice device : pairedDevices) {
                		if(i == item) {
                			choosenDevice = device;
                		}
                		i++;
            	    }
                	// connect to the selected device
        	        connectToDevice();
        	    }
        	});
        	// show the alert
        	AlertDialog alert = builder.create();
        	alert.show();
    	} else {
    		scanForDevices();
    	}
    }
    
    /**
     * initiate the actual connection and create the handler thread
     */
    private void connectToDevice() {
    	if(choosenDevice != null) {
    		// device is present
    		if(BluetoothAdapter.checkBluetoothAddress(choosenDevice.getAddress())) {
    			// address is valid
    			BTHandler r = BTHandler.getInstance(choosenDevice, this);
    			if(r.isAlive()) {
    				// if is already connected close the connection
    				r.closeConnection();    				
    			}
    			// set parameters ans run the thread
    			r.setParent(this);
    			r.setDisplayPanel(surfaceView);
    			r.start();
    		}
    	} else {
    		Toast.makeText(getApplicationContext(), "no device selected", Toast.LENGTH_SHORT).show();
    	}
    }
    
    /**
     * local broadcastReciver for ActivityResult
     * @deprecated
     */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		/*if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(getApplicationContext(), "bt not available", Toast.LENGTH_SHORT).show();	
				btisEnabled = false;
			} else {
				Toast.makeText(getApplicationContext(), "bt enabled AR", Toast.LENGTH_SHORT).show();
				btWasEnabled = false;
				btisEnabled = true;
			}
		}*/				
	}    
}