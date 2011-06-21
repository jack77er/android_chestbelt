package com.cestbelt;

import java.util.Iterator;
import java.util.Vector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class Panel extends SurfaceView implements SurfaceHolder.Callback {
	public Vector<Integer> pulse = new Vector<Integer>();

	private CanvasThread canvasthread;

    
	public Panel(Context context, AttributeSet attrs) {
          super(context, attrs);
          // TODO Auto-generated constructor stub
      getHolder().addCallback(this);
      canvasthread = new CanvasThread(getHolder(), this);
      setFocusable(true);
      
  }
	
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                    int height) {
            // TODO Auto-generated method stub
           
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
            // TODO Auto-generated method stub
        canvasthread.setRunning(true);
        canvasthread.start();
           
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
            // TODO Auto-generated method stub
    		pulse = null;
            boolean retry = true;
            canvasthread.setRunning(false);
            while (retry) {
                    try {
                            canvasthread.join();
                            retry = false;
                    } catch (InterruptedException e) {
                            // we will try it again and again...
                    }
            }

    }
 
	   @Override
       public void onDraw(Canvas canvas) {
		   super.onDraw(canvas);
		   
		   
               Paint paint = new Paint();
            // if not reset then red retangle
               canvas.drawColor(Color.BLACK);

               
               
               // delete first 3 values => sliding 
               if(pulse.size() >150 ){
            	   pulse.removeElementAt(0);
            	   pulse.removeElementAt(1);
            	   pulse.removeElementAt(2);
               }
              
               
               // drawing coordinates
               paint.setColor(Color.GRAY);
               canvas.drawLine(50, 0,50, 220,paint);
               canvas.drawLine(49, 0,49, 220,paint);
               canvas.drawLine(49, 222,349, 222,paint);
               canvas.drawLine(49, 221,349, 221,paint);
               canvas.drawLine(49, 220,349, 220,paint);
               
               // markers on y-axis
               paint.setTextSize(20);
               paint.setTextScaleX(1.4f);
               canvas.drawLine(45, 220,349, 220,paint);
               canvas.drawText("0", 30, 220, paint);
               canvas.drawLine(45, 170,349, 170,paint);
               canvas.drawText("50", 0, 170, paint);
               canvas.drawLine(45, 120,349, 120,paint);
               canvas.drawText("100", 0, 120, paint);
               canvas.drawLine(45, 70,349, 70,paint);
               canvas.drawText("150", 0, 70, paint);
               canvas.drawLine(45, 20,349, 20,paint);
               canvas.drawText("200", 0, 20, paint);
               
               
               paint.setColor(Color.RED);
               Iterator<Integer> iter = pulse.listIterator();
               int i = 0;
               while(iter.hasNext()){
            	   int stopY = (int) iter.next() ;
            	   // thicker lines and inverting stopY because y = 0 is at the top of canvas element
            	   i++;
            	   canvas.drawLine(50+i, 220,50+i, 220 - stopY, paint);
            	   i++;
            	   canvas.drawLine(50+i, 220,50+i, 220 - stopY, paint);
               }
               
       }
	   
	   public void addPulseValue(int value){
       	pulse.add(value);
       }
	   
	   public void addPulseValues(byte[] input ) {
		   
	   }
	   
	   private static int getPulseValue(byte[] input) {
		   if(input.length != 2) {
			   return -1;
		   }
		   return 1; // TODO Dummy value
	   }
}

