// 2015/03/07: remove unncessary sensors reading (for saving power)

package edu.umich.cse.audioanalysis;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MySensorController implements SensorEventListener {
	private static int MESSAGE_TO_START_RECORD = 0;

	public static final int MONITOR_STATE_IDLE = 0;
	public static final int MONITOR_STATE_WAIT_EVENT = 1;
	public static final int MONITOR_STATE_FINISH_EVENT = 2;
	public static final int MONITOR_STATE_EVENT_CHANGED = 3; // detect the location is no longer matched

	int monitorState;

	SensorManager sm = null;
	Sensor acc = null;
	Sensor gyro = null;
	Sensor geomag = null;
	
	int queueLen = 1000000;
	int SAMPLE_PERIOD = 100; // ms -> 10Hz
	boolean isRecording = false;
	
	MySensorControllerListener mCaller;
	
	RecordTimer mRecordTimer;
	
	float gravityX;
	float gravityY;
	float gravityZ;
	
	public float accMag;
	float linAccMag;
	
	double tiltX;
	double tiltY;
	double tiltZ;
	public DataQueue tiltXqueue;
	public DataQueue tiltYqueue;
	public DataQueue tiltZqueue;
	
	double tiltZDegree;
	
	float accX;
	float accY;
	float accZ;
	
	float gyroX;
	float gyroY;
	float gyroZ;
	public float gyroMag;
	public DataQueue gyroMagQueue;
	
	float geomagX;
	float geomagY;
	float geomagZ;
	public DataQueue geomagXqueue;
	public DataQueue geomagYqueue;
	public DataQueue geomagZqueue;

	FileOutputStream motionOutputStream;
	SimpleDateFormat mDateFormat;
	

	public MySensorController(MySensorControllerListener caller, SensorManager systemSensorManager, String logFolderPath, int queueLen) {
		sm = systemSensorManager;
        mCaller = caller;

		//acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		// use the liner acc to avoid earth g
		// ref: http://stackoverflow.com/questions/3377288/how-to-remove-gravity-factor-from-accelerometer-readings-in-android-3-axis-accel
		acc = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        geomag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);

        
        sm.registerListener(this, acc, SAMPLE_PERIOD*1000);
        sm.registerListener(this, gyro, SAMPLE_PERIOD*1000);
        //sm.registerListener(this, geomag, SensorManager.SENSOR_DELAY_FASTEST);
		sm.registerListener(this, geomag, SAMPLE_PERIOD*1000);
        Log.w(C.LOG_TAG, "WARN: mag is disabled for testing power measurement");
        
        mRecordTimer = new RecordTimer(this);
        this.queueLen = queueLen;
        
        tiltXqueue = new DataQueue(queueLen);
        tiltYqueue = new DataQueue(queueLen);
        tiltZqueue = new DataQueue(queueLen);
        gyroMagQueue = new DataQueue(queueLen);
        
        geomagXqueue = new DataQueue(queueLen);
        geomagYqueue = new DataQueue(queueLen);
        geomagZqueue = new DataQueue(queueLen);

        isRecording = false;
        mDateFormat = new SimpleDateFormat("MM-dd_HH-mm-ss");
	}

	public void startRecord(String logFolderPath){
		if(!isRecording){
			isRecording = true;
			try {
				motionOutputStream = new FileOutputStream(new File(logFolderPath +"motion.dat"),true);
			} catch (FileNotFoundException e) {
				Log.e(C.LOG_TAG, "ERROR: can't open sensor file to write");
				e.printStackTrace();
			}
			//mRecordTimer.sendMessageDelayed(Message.obtain(null, MESSAGE_TO_START_RECORD), SAMPLE_PERIOD);
		}
	}
	
	public void stopRecord(){
		isRecording = false;
		try {
			motionOutputStream.close();
		} catch (IOException e){
			Log.e(C.LOG_TAG, "ERROR: can't open sensor file to write");
			e.printStackTrace();
		}
	}
	
	public void dumpToFile(int index, String shotPath){
		Log.e(C.LOG_TAG, "ERROR: depreciated method: dumpToFile");
		// *** this function is depricated !!! ***
		//String stringToSave = String.format("%d|%s|%f:%f:%f|%f:%f:%f|%f:%f:%f|%f:%f:%f|%f:%f:%f", index, shotPath, tiltX, tiltY, tiltZ, accX, accY,accZ, gravityX, gravityY, gravityZ, lightMag,accMag,linAccMag,gyroX,gyroY,gyroZ);
		//fileToSave.println(stringToSave);
	}

	// ================== start of sensor callback ========================
	@Override
    public void onSensorChanged(SensorEvent event) {
    	if(event.sensor.getType() == Sensor.TYPE_GRAVITY) {
    		gravityX = event.values[0];
    		gravityY = event.values[1];
    		gravityZ = event.values[2];
    	}

     	if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
     		gyroX = event.values[0];
     		gyroY = event.values[1];
     		gyroZ = event.values[2];
     		gyroMag = (float) Math.sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ);
     	}

     	if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
     		linAccMag = (float) Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]);
     	}

     	if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED) {
     		geomagX = event.values[0];
     		geomagY = event.values[1];
     		geomagZ = event.values[2];
     		//Log.w(C.LOG_TAG, "MySensorController: geomag = ("+geomagX+","+geomagY+","+geomagZ+",)");
     	}

		/*
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			accX = -event.values[0]/ SensorManager.GRAVITY_EARTH;
			accY = -event.values[1]/ SensorManager.GRAVITY_EARTH;
			accZ = event.values[2]/ SensorManager.GRAVITY_EARTH;
			accMag = (float) Math.sqrt(accX * accX + accY * accY + accZ * accZ);
			tiltX =  Math.asin(accX / accMag);
			tiltY =  Math.asin(accY / accMag);
			tiltZ =  Math.asin(accZ / accMag);
			tiltZDegree = radiusToDegree(tiltZ);

			mCaller.onTiltChanged(tiltX, tiltY, tiltZ);
		}*/

		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			accX = event.values[0];
			accY = event.values[1];
			accZ = event.values[2];
			accMag = (float) Math.sqrt(accX * accX + accY * accY + accZ * accZ);
			// mCaller.onTiltChanged(tiltX, tiltY, tiltZ); // not used anymore
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}


	public void outputMotion(int recordStamp) {
		ByteBuffer bStamp = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		bStamp.putInt(recordStamp);
		ByteBuffer bData = getDataBuffer();

		/*
		ByteBuffer bData = ByteBuffer.allocate(6 * 4).order(ByteOrder.LITTLE_ENDIAN);
		bData.putFloat(accX);
		bData.putFloat(accY);
		bData.putFloat(accZ);
		bData.putFloat(gyroX);
		bData.putFloat(gyroY);
		bData.putFloat(gyroZ);
		*/

		if (C.TRACE_SAVE_TO_FILE) {
			try {
				motionOutputStream.write(bStamp.array());
				motionOutputStream.write(bData.array());
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}

	public ByteBuffer getDataBuffer(){
		int byteCnt = 6*4; // 6 varaible, each is float (4 bytes);
		ByteBuffer b = ByteBuffer.allocate(byteCnt).order(ByteOrder.LITTLE_ENDIAN);
		b.putFloat(accX);
		b.putFloat(accY);
		b.putFloat(accZ);
		b.putFloat(gyroX);
		b.putFloat(gyroY);
		b.putFloat(gyroZ);
		return b;
	}

	public static double radiusToDegree(double radius){
		return radius*(180/3.1415926);
	}

	// not use this part anymore
	// ================== end of sensor  callback =========================
    private class RecordTimer extends Handler {
    	MySensorController sensorController;
    	public RecordTimer(MySensorController sensorController) {
    		this.sensorController =  sensorController; 
		}
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == MESSAGE_TO_START_RECORD) {
				if(isRecording){
					// TODO: enable it in the real time computing
					/*
					tiltXqueue.add(tiltX);
					tiltYqueue.add(tiltY);
					tiltZqueue.add(tiltZ);
					gyroMagQueue.add(gyroMag);
					geomagXqueue.add(geomagX);
					geomagYqueue.add(geomagY);
					geomagZqueue.add(geomagZ);
					*/

					// TODO: fix the file problem
					/*
					if(!C.NO_TRACE_OUT){
						//String stringToSave = String.format("%f:%f:%f|%f:%f:%f|%f:%f:%f|%f:%f:%f|%f:%f:%f|%f:%f:%f", tiltX, tiltY, tiltZ, accX, accY,accZ, gravityX, gravityY, gravityZ, lightMag,accMag,linAccMag,gyroX,gyroY,gyroZ,geomagX,geomagY,geomagZ);
						//String stringToSaveShort = String.format("%f:%f:%f|%f:%f:%f|%f:%f:%f", tiltX, tiltY, tiltZ, gyroX, gyroY, gyroZ, geomagX, geomagY, geomagZ);
						String stringToSaveShort = getDataString();
						fileToSave.println(stringToSaveShort);
						fileToSave.flush();
						dateFileToSave.println(getTime());
						dateFileToSave.flush();
					}
					*/
					mCaller.onRecordedEnd();
					// create next event
					mRecordTimer.sendMessageDelayed(Message.obtain(null, MESSAGE_TO_START_RECORD), SAMPLE_PERIOD);
				} else {
					Log.d(C.LOG_TAG, "recording stops -> save results to file if necessary");
				}
			} else {
				Log.e(C.LOG_TAG, "RecordTimer: unknown message = " + msg.what);
			}
		}
    }

    
    String getTime(){
		Calendar c = Calendar.getInstance();
        int mseconds = c.get(Calendar.MILLISECOND);
        String currentDateandTime = mDateFormat.format(new Date()) + String.format("-%02d", mseconds);
        return currentDateandTime;
	}
    
}
