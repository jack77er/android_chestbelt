package com.cestbelt.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class cest_activity extends Activity {
	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	
	public final static int REQUEST_ENABLE_BT = 0;
	private boolean btWasEnabled = false;
	
	// UI View elements
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button btnRescan = (Button)findViewById(R.id.btnRescan);
        btnRescan.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "button clicked", Toast.LENGTH_SHORT).show();	
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
        
    }
    
    private void scanForCestbelt() {

    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			System.out.println(resultCode);
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(getApplicationContext(), "bt not available", Toast.LENGTH_SHORT).show();		
				scanForCestbelt();
			} else {
				Toast.makeText(getApplicationContext(), "bt enabled AR", Toast.LENGTH_SHORT).show();
			}
		}				
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
                    //onBluetoothEnable();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                	Toast.makeText(getApplicationContext(), "bt disabled", Toast.LENGTH_SHORT).show();
                    //onBluetoothDisable();
                    break;
                }
            } 
        }
    };
}