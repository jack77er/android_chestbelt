package com.cestbelt;

import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cestbelt.test.R;

public class PulseActivity extends Activity{
	
	public SurfaceView surfacePanel;
	private Random r = new Random();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        //Bundle myBundle = getIntent().getExtras();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pulse);
        

        TextView currentPulse = (TextView) findViewById(R.id.textCurrentPulse);
        String test = savedInstanceState != null ? (String) savedInstanceState.getString("test2") : null;
      
        
//        if(test == null){
//        	Bundle extras = getIntent().getExtras();
//        	test = extras != null ? extras.getString("test2") : "nothing passed in";
//        }
        
//        Toast toast=Toast.makeText(this, test, Toast.LENGTH_LONG);
//        toast.show();
        
        Panel surfaceView = (Panel) this.findViewById(R.id.SurfaceView);
//        currentPulse.setText("Text" + test );

        
        for (int i = 0; i< 15; i++){
        surfaceView.addPulseValue(50);
        surfaceView.addPulseValue(100);
        surfaceView.addPulseValue(100);
        surfaceView.addPulseValue(170);
        currentPulse.setText("current pulse: " + 210 );
        surfaceView.addPulseValue(210);
        surfaceView.addPulseValue(200);
        surfaceView.addPulseValue(200);
        surfaceView.addPulseValue(200);
        surfaceView.addPulseValue(100);
        surfaceView.addPulseValue(150);
        surfaceView.addPulseValue(150);
        surfaceView.addPulseValue(180);
        surfaceView.addPulseValue(180);
//        currentPulse.setText("current pulse: " + 180 );
        }
	}	

}




