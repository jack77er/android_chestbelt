package com.cestbelt;

import java.util.Random;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.cestbelt.R;

public class PulseActivity extends Activity{
	
	private Random r = new Random();
	private IntentFilter filter;
	static final String NEW_PULSE_DATA = "cestbelt_newPulseData";
	
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(bcastReceiver); 
		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        //Bundle myBundle = getIntent().getExtras();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pulse);
        
       filter = new IntentFilter(PulseActivity.NEW_PULSE_DATA);
        registerReceiver(bcastReceiver, filter); // Don't forget to unregister during onDestroy

        TextView currentPulse = (TextView) findViewById(R.id.textCurrentPulse);
        String test = savedInstanceState != null ? (String) savedInstanceState.getString("test2") : null;
        Toast.makeText(getApplicationContext(), test, Toast.LENGTH_SHORT).show();
        
//        if(test == null){
//        	Bundle extras = getIntent().getExtras();
//        	test = extras != null ? extras.getString("test2") : "nothing passed in";
//        }
        
//        Toast toast=Toast.makeText(this, test, Toast.LENGTH_LONG);
//        toast.show();
        
        Panel surfaceView = (Panel) this.findViewById(R.id.SurfaceView);
//        currentPulse.setText("Text" + test );
        
        
        for (int i = 0; i< 150; i++){
	        surfaceView.addPulseValue(50);
	        surfaceView.addPulseValue(100);
	        surfaceView.addPulseValue(100);
	        surfaceView.addPulseValue(170);
	        currentPulse.setText("current pulse: " + 210 );
//        currentPulse.setText("current pulse: " + 180 );
        }
        BTHandler.getInstance(null, null).setDisplayPanel(surfaceView);
	}
	
    private final BroadcastReceiver bcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
           
            intent.getDataString();
            if (action.equals(PulseActivity.NEW_PULSE_DATA)) {
            	Panel surfaceView = (Panel) findViewById(R.id.SurfaceView);
            	surfaceView.addPulseValue(intent.getIntExtra("pulseValue", 0));
            }
        }
    };

}





