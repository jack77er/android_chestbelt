package com.cestbelt.test;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

public class cest_activity extends Activity {
	
	private final Context mainContext = this;
	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	
	public final static int REQUEST_ENABLE_BT = 0;
	private boolean btWasEnabled = true;
	private boolean btisEnabled = false;
	
	static final int DIALOG_BT_SCANNING = 1;
	
	
	
	private ArrayList<BluetoothDevice> remoteDevices;
	private BluetoothDevice choosenDevice;

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bcastReceiver, filter); // Don't forget to unregister during onDestroy
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bcastReceiver, filter); // Don't forget to unregister during onDestroy
        
        
        remoteDevices = new ArrayList<BluetoothDevice>();
        
        setContentView(R.layout.main);
        ((Button)findViewById(R.id.btnRescan)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
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
          });

    	if(!adapter.isEnabled()) {
    		// bluetooth disabled
    		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);    	
    	}
    	
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!btWasEnabled) {
        	// quick and dirty
        	adapter.disable();
        }
	}

    /**
     * Method for creating dialogs
     */
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_BT_SCANNING) {
			ProgressDialog loadingDialog = new ProgressDialog(this);
			loadingDialog.setMessage("searching...");
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(true);
			return loadingDialog;
		}
		return super.onCreateDialog(id);
	}

    
    private void scanForCestbelt() {

    }
    
    private final BroadcastReceiver bcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                case BluetoothAdapter.STATE_ON:
                	Toast.makeText(getApplicationContext(), "bt enabled", Toast.LENGTH_SHORT).show();
                	btWasEnabled = false;
                	btisEnabled = true;
                    //onBluetoothEnable();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                	Toast.makeText(getApplicationContext(), "bt disabled", Toast.LENGTH_SHORT).show();
                	btisEnabled = false;
                    //onBluetoothDisable();
                    break;
                }
            } else if(action.equals(BluetoothDevice.ACTION_FOUND)) {
            	if(!remoteDevices.contains((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE))) {
            		remoteDevices.add((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
            	}
            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
            	//Toast.makeText(getApplicationContext(), "scan finished", Toast.LENGTH_SHORT).show();
            	
            	AlertDialog.Builder builder = new AlertDialog.Builder(mainContext);
            	builder.setTitle("Choose a Device");
            	
            	final CharSequence[] items = new CharSequence[remoteDevices.size()];
            	for(int i = 0 ; i < remoteDevices.size() ; i++) {
            		items[i] = remoteDevices.get(i).getName();
            	}
            	
            	builder.setItems(items, new DialogInterface.OnClickListener() {
            	    public void onClick(DialogInterface dialog, int item) {
            	        choosenDevice = remoteDevices.get(item);
            	    }
            	});
            	
            	dismissDialog(DIALOG_BT_SCANNING);
            	AlertDialog alert = builder.create();
            	alert.show();
            	
            } 
        }
    };
    
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			System.out.println(resultCode);
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(getApplicationContext(), "bt not available", Toast.LENGTH_SHORT).show();	
				btisEnabled = false;
			} else {
				Toast.makeText(getApplicationContext(), "bt enabled AR", Toast.LENGTH_SHORT).show();
				btWasEnabled = false;
				btisEnabled = true;
				scanForCestbelt();
			}
		}				
	}
	
	

    
}