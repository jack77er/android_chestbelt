package com.cestbelt;

import android.content.Intent;
import android.graphics.Canvas;
import android.view.SurfaceView;

public class PulseHandler extends Thread{

	private Intent pulseIntent;
	private boolean _run;
	static PulseHandler ps;
	Panel panel;
	
	public PulseHandler(SurfaceView panel, Intent pulseIntent){
		if( ps == null)
			ps = this;
		
		this.panel = (Panel)panel;
		this.pulseIntent = pulseIntent;
	}
	
	static public PulseHandler getInstance(){
		return ps;
	}
	

	
    @Override
    public void run() {
        Canvas c;
        while (_run) {
            c = null;
            try {
            	
                
                int pulseVal = pulseIntent.getExtras().getInt("PulsData");
                
                panel.addPulseValue(pulseVal);
                
                }
            } finally {

                }
            }
        }
    }
	
}
