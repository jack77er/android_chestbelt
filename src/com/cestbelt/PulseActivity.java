package com.cestbelt;

import java.util.Random;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class PulseActivity extends Activity{
	
	private Random r = new Random();
	private IntentFilter filter;
	static final String NEW_PULSE_DATA = "cestbelt_newPulseData";
	static private PulseActivity pulseActivity;
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
//		unregisterReceiver(bcastReceiver); 
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        //Bundle myBundle = getIntent().getExtras();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pulse);
        pulseActivity = this;
       filter = new IntentFilter(PulseActivity.NEW_PULSE_DATA);
//        registerReceiver(bcastReceiver, filter); // Don't forget to unregister during onDestroy
        
        TextView currentPulse = (TextView) findViewById(R.id.textCurrentPulse);
        String test2 = getIntent().getExtras().getString("test2");
        int a = getIntent().getExtras().getInt("pulseData");
        Toast.makeText(getApplicationContext(), test2+a, Toast.LENGTH_SHORT).show();
        
        
        Panel surfaceView = (Panel) this.findViewById(R.id.SurfaceView);
        if(surfaceView != null){
        	surfaceView.setIntent(getIntent());
        }
        
	}
	

    static public PulseActivity getPulseActivity(){
    	return pulseActivity;
    }
    
}





