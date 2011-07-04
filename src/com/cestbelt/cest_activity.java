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
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class cest_activity extends Activity {
	
	private final Context mainContext = this;
	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	
	public final static int REQUEST_ENABLE_BT = 0;
	private boolean btWasEnabled = true;
	private boolean btisEnabled = false;
	
	static final int DIALOG_BT_SCANNING = 1;
	private boolean MANUAL_CONNECT = false;
	private boolean MANUAL_CONNECT_FOUND = false;
	private SurfaceView surfaceView;
	private final int PULSE_ACTIVITY_REQUEST_CODE = 2;
	
	private ArrayList<BluetoothDevice> remoteDevices;
	private BluetoothDevice choosenDevice;

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

        
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bcastReceiver, filter); // Don't forget to unregister during onDestroy
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bcastReceiver, filter); // Don't forget to unregister during onDestroy
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bcastReceiver, filter); // Don't forget to unregister during onDestroy
        
        // recover old entry
        
        remoteDevices = new ArrayList<BluetoothDevice>();
        SharedPreferences p = getSharedPreferences("prevBelt", MODE_PRIVATE);
        String oldMAC = p.getString("prevMAC", "00:00:00:00:00:00");
        
        if(!oldMAC.equals("00:00:00:00:00:00")) {
        	if(adapter.isEnabled() && adapter.getState() == adapter.STATE_ON && !adapter.isDiscovering())
        	choosenDevice = adapter.getRemoteDevice(oldMAC);
        } 
        
        
        setContentView(R.layout.main);
        
        
        final Panel surfaceView = (Panel) findViewById(R.id.SurfaceView);
      if(surfaceView != null){
      	this.surfaceView = surfaceView;
      }
        
        ((Button)findViewById(R.id.btnRescan)).setOnClickListener(new OnClickListener() {


        	
			@Override
			public void onClick(View arg0) {
				scanForDevices();
			}
          });
        
        ((Button)findViewById(R.id.btnPing)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					BTHandler.getInstance(choosenDevice,mainContext).sendPingRequest();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
          });
        
        ((Button)findViewById(R.id.btnVersion)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					BTHandler.getInstance(choosenDevice,mainContext).sendPulseRequest();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
          });
        
        ((Button)findViewById(R.id.btnReset)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					BTHandler.getInstance(choosenDevice,mainContext).sendResetRequest();
					BTHandler.destroyInstance();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
          });
        
        ((Button)findViewById(R.id.btnConnect)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					if(choosenDevice != null) {
						MANUAL_CONNECT = true;
						MANUAL_CONNECT_FOUND = false;
						scanForDevices();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
          });

        ((Button)findViewById(R.id.btnBuzzer)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					BTHandler.getInstance(choosenDevice,mainContext).sendBuzzerRequest();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
          });
        
        
        ((Button)findViewById(R.id.btnPulseActivity)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					

//					t.start();
				for(int i =0; i <100; i++){
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
        
        ((Button)findViewById(R.id.btnDisconnect)).setOnClickListener(new OnClickListener() {

        	
        	
			@Override
			public void onClick(View arg0) {
				try {
					BTHandler.getInstance(choosenDevice, mainContext).closeConnection();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
          });
    	if(!adapter.isEnabled()) {
    		// bluetooth disabled
    		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);    	
    	}
    	
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
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
		if(!adapter.isEnabled()) {
    		// bluetooth disabled
    		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);    	
    	} else {		    		
    		// interrupt discovery if already started
    		adapter.cancelDiscovery();
    		if(adapter.startDiscovery()) {
    			//Toast.makeText(getApplicationContext(), "scan initiated", Toast.LENGTH_SHORT).show();
    			showDialog(DIALOG_BT_SCANNING);
    		} else {
    			Toast.makeText(getApplicationContext(), "scan failed", Toast.LENGTH_SHORT).show();
    		}
    	}	
	}
    
    @Override
    protected void onPause() {
    	super.onPause();
    	SharedPreferences p = getSharedPreferences("prevBelt", MODE_PRIVATE);
    	SharedPreferences.Editor e = p.edit();
    	if(choosenDevice != null) {
    		e.putString("prevMAC", choosenDevice.getAddress());
        	e.commit();
    	}
//    	try {
//			th.join();
//		} catch (InterruptedException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
    	
    }

	@Override
    protected void onDestroy() {
		unregisterReceiver(bcastReceiver);
        
		super.onDestroy();
    	SharedPreferences p = getSharedPreferences("prevBelt", MODE_PRIVATE);
    	SharedPreferences.Editor e = p.edit();
    	if(choosenDevice != null) {
    		e.putString("prevMAC", choosenDevice.getAddress());
        	e.commit();
    	}
        
    	BTHandler.getInstance(choosenDevice, mainContext).closeConnection();
    	BTHandler.destroyInstance();
    	
        if(!btWasEnabled) {
        	// quick and dirty
        	//adapter.disable();
        }

        
	}

	protected void onStop() {
		super.onStop();
		
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
			ProgressDialog loadingDialog = new ProgressDialog(this){
				@Override
				public void onBackPressed(){
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
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                case BluetoothAdapter.STATE_ON:
                	Toast.makeText(getApplicationContext(), "bt enabled", Toast.LENGTH_SHORT).show();
                	btWasEnabled = false;
                	btisEnabled = true;
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                	Toast.makeText(getApplicationContext(), "bt disabled", Toast.LENGTH_SHORT).show();
                	btisEnabled = false;
                    break;
                }
            } else if(action.equals(BluetoothDevice.ACTION_FOUND)) {
            	if(!remoteDevices.contains((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE))) {
            		BluetoothDevice tmp = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            		if(MANUAL_CONNECT && tmp.getAddress().equals(choosenDevice.getAddress())) {
            			MANUAL_CONNECT_FOUND = true;
            			adapter.cancelDiscovery();
            			dismissDialog(DIALOG_BT_SCANNING);
            			choosenDevice = tmp;
            			connectToDevice();
            		}
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
            		dismissDialog(DIALOG_BT_SCANNING);
            	} else {
            		// new scan completed
	            	AlertDialog.Builder builder = new AlertDialog.Builder(mainContext);
	            	builder.setTitle("Choose a Device");
	            	
	            	final CharSequence[] items = new CharSequence[remoteDevices.size()];
	            	for(int i = 0 ; i < remoteDevices.size() ; i++) {
	            		items[i] = remoteDevices.get(i).getName();
	            	}
	            	
	            	builder.setItems(items, new DialogInterface.OnClickListener() {
	            	    public void onClick(DialogInterface dialog, int item) {
	            	        choosenDevice = remoteDevices.get(item);
	            	    	SharedPreferences p = getSharedPreferences("prevBelt", MODE_PRIVATE);
	            	    	SharedPreferences.Editor e = p.edit();
	            	    	if(choosenDevice != null) {
	            	    		e.putString("prevMAC", choosenDevice.getAddress());
	            	        	e.commit();
	            	        	TextView v = (TextView) findViewById(R.id.txtDev);
	            	        	v.setText("used device: " + choosenDevice.getAddress());
	            	    	}
	            	        connectToDevice();
	            	    }
	            	});
	            	
	            	dismissDialog(DIALOG_BT_SCANNING);
	            	AlertDialog alert = builder.create();
	            	alert.show();
            	}
            } 
        }
    };
    
    
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
        	        connectToDevice();
        	    }
        	});
        	
        	AlertDialog alert = builder.create();
        	alert.show();
    	} else {
    		scanForDevices();
    	}
    }
    
    private void connectToDevice() {
    	if(choosenDevice != null) {
    		if(BluetoothAdapter.checkBluetoothAddress(choosenDevice.getAddress())) {
    			BTHandler r = BTHandler.getInstance(choosenDevice, this);
    			if(r.isAlive()) {
    				r.closeConnection();
    				
    			}
    			r.setParent(this);
    			r.start();
    		}
    	} else {
    		Toast.makeText(getApplicationContext(), "no device selected", Toast.LENGTH_SHORT).show();
    	}
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(getApplicationContext(), "bt not available", Toast.LENGTH_SHORT).show();	
				btisEnabled = false;
			} else {
				Toast.makeText(getApplicationContext(), "bt enabled AR", Toast.LENGTH_SHORT).show();
				btWasEnabled = false;
				btisEnabled = true;
			}
		}				
	}
	

//	Thread t = new Thread(){
//		
//		public void run(){
//			for(int i =0; i <100; i++){
//		        ((Panel)surfaceView).addPulseValue(50);
//			}
//		}
//		
//	};
	
//	public void test(Thread t){
//		Intent i = new Intent();
//        i.putExtra("pulseValue", 90);
//        i.setAction(com.cestbelt.PulseActivity.NEW_PULSE_DATA);
//        ((Activity)mainContext).sendBroadcast(i);
//        ((Activity)mainContext).sendBroadcast(i);
//        i.putExtra("pulseValue", 190);
//        ((Activity)mainContext).sendBroadcast(i);
//        i.putExtra("pulseValue", 190);
//        ((Activity)mainContext).sendBroadcast(i);
//        synchronized (t) {
//        	t.notify();
//		}
        
	
    
}