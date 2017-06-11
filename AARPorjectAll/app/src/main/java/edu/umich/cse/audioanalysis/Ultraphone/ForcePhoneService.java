package edu.umich.cse.audioanalysis.Ultraphone;

//import com.sec.forcephoneservice.bridge.ForcePhoneBridge;
//import com.sec.forcephoneservice.fixme.ForcePhoneControllerListener;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import edu.umich.cse.audioanalysis.MyApp;

public class ForcePhoneService extends Service{

	private static final String TAG = "ForcePhoneService";
	
	////// share below constants
	// msg to service
	public static final int MSG_CONNECT_TO_SERVICE = 0;
	public static final int MSG_DISCONNECT_TO_SERVICE = 1;
	public static final int MSG_START_CHECK_TO_SEVICE = 2; // arg1 : x , arg2 : y
	public static final int MSG_STOP_CHECK_TO_SEVICE = 3;
	
	// msg from service
	public static final int MSG_ACK_FROM_SERVICE = 4;
	public static final int MSG_SEND_PRESSURE_FROM_SERVICE = 5; // arg1 : pressureNoteCalibedInt, arg2 : scale
	public static final int MSG_ERROR_FROM_SERVICE = 6;
	///////////////////////////////
	
	protected ForcePhoneBridge mForcePhoneBridge = null;
	protected CalibrationController mCalibrationController;
	protected Messenger mClientMessenger = null;
	protected final Messenger mMessenger = new Messenger(new MessageHandler());
	protected class MessageHandler extends Handler {
		
		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case MSG_CONNECT_TO_SERVICE:
				Log.v(TAG,"MSG_CONNECT_TO_SERVICE");
				if (mClientMessenger != null) {
					Toast.makeText(getApplicationContext(), "Only 1 Application is allowed to connect the service", Toast.LENGTH_SHORT).show();
					break;
				}
				mClientMessenger = msg.replyTo;
				Message toClientMsg = Message.obtain(null, MSG_ACK_FROM_SERVICE, 0, 0);
				try {
					// send ack
					mClientMessenger.send(toClientMsg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}

				// init the bridge here
				if(mForcePhoneBridge != null){
					Log.e(TAG, "[ERROR]: Service is bind when the mForcePhoneBridge already exist (server is connected twice?)");
				} else {
					// Laod default setting of calibration
					mCalibrationController = new CalibrationController("", (Context) getApplicationContext(),(MyApp)getApplicationContext());

					mForcePhoneBridge = new ForcePhoneBridge(getApplicationContext(), mForcePhoneListener);
					mForcePhoneBridge.startSensing();
				}
				
				break;
			case MSG_DISCONNECT_TO_SERVICE:
				Log.v(TAG,"MSG_DISCONNECT_TO_SERVICE");
				if (mClientMessenger == null)
					return;
				
				if (mForcePhoneBridge != null) {
					mForcePhoneBridge.stopSensing();
					mForcePhoneBridge = null;
				}

				mClientMessenger = null;
				
				break;
			case MSG_START_CHECK_TO_SEVICE:
				Log.v(TAG,"MSG_START_CHECK_TO_SEVICE");
				
				if (mClientMessenger == null)
					break;
				
				int x = msg.arg1;
				int y = msg.arg2;

				Point point = new Point(x, y);
				if(mCalibrationController!=null && mCalibrationController.calibrationIsReady) {
					mCalibrationController.lockTouchLocation(x, y);
				}
				mForcePhoneBridge.startCheckPressure(point);
				break;
			
			case MSG_STOP_CHECK_TO_SEVICE:
				Log.v(TAG,"MSG_STOP_CHECK_TO_SEVICE");
				
				if (mForcePhoneBridge == null)
					break;

				if(mCalibrationController!=null && mCalibrationController.calibrationIsReady) {
					mCalibrationController.unlockTouchLocation();
				}
				mForcePhoneBridge.stopCheckPressure();
				
				break;
				
			default:
				break;
			}

			super.handleMessage(msg);
		}
		
	}
	
	
	
	
	protected ForcePhoneControllerListener mForcePhoneListener = new ForcePhoneControllerListener() {


		@Override
		public void pressureUpdate(double pressureNotCalibed) {
			Log.v(TAG,"pressureUpdate()");

			double pressure = pressureNotCalibed;
			// update pressure by calibartion if needed
			if(mCalibrationController!=null && mCalibrationController.calibrationIsReady) {
				pressure = mCalibrationController.getLockedCalibResult(pressureNotCalibed);
			}

			int scale = 100000; // you may change it
			int pressureCalibedIfPossibleInt = (int) (pressure * scale);
			
			// Message only can contain Integer data.
			Message toClientMsg = Message.obtain(null, MSG_SEND_PRESSURE_FROM_SERVICE, pressureCalibedIfPossibleInt, scale);
			try {
				// send pressure data
				mClientMessenger.send(toClientMsg);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void error(int code, String msg) {
			Log.v(TAG,"error()");
			Log.e(TAG,"Service get error message = "+msg);

			// Message only can contain Integer data.
			Message toClientMsg = Message.obtain(null, MSG_ERROR_FROM_SERVICE, code, code);
			try {
				// send pressure data
				mClientMessenger.send(toClientMsg);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
	};
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG,"onBind(Intent intent)");
		
		return mMessenger.getBinder();
	}
	
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG,"onStartCommant()");
		
		final int id = 0;
		startForeground(id, null);



		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {

		
		
		
		stopForeground(true);
		
		
		super.onDestroy();
	}
	
}
