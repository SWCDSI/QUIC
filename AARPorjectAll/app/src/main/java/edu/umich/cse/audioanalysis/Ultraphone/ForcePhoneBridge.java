package edu.umich.cse.audioanalysis.Ultraphone;

//import com.sec.forcephoneservice.fixme.ForcePhone;
//import com.sec.forcephoneservice.fixme.ForcePhoneControllerListener;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.Ultraphone.ExpActivity.UltraphoneController;

public class ForcePhoneBridge {

	private static final String TAG = "ForcePhoneBinder";
	
	protected Context mContext = null;
	
	protected ForcePhoneControllerListener mSamsungListener = null;
	protected UltraphoneControllerListener mUltraPhoneListener = new UltraphoneControllerListener() {
		@Override
		public void pressureUpdate(double pressure) { // bridge the result to Samsung's listener
			mSamsungListener.pressureUpdate(pressure);
		}
		@Override
		public void squeezeUpdate(double check) {

		}
		@Override
		public void updateDebugStatus(String stringToShow) {

		}
		@Override
		public void showToast(String stringToShow) {
			/*
			if(mContext!=null){
				Toast.makeText(mContext, stringToShow, Toast.LENGTH_LONG).show();
			}
			*/
		}
		@Override
		public void unexpectedEnd(int code, String reason) {
			mSamsungListener.error(code, reason);
		}
	};

	//protected ForcePhone mForcePhone = null;
	protected UltraphoneController mUltraPhone = null;
	private boolean mIsRunning = false;



	public ForcePhoneBridge(Context context, ForcePhoneControllerListener listener) {
		mContext = context;
		mSamsungListener = listener;
		mUltraPhone = new UltraphoneController(D.DETECT_PSE, mUltraPhoneListener, context);
	}

	/*
	protected void pressureUpdate(final double pressureNotCalibed) {
		if (mListener == null)
			return;
		
		mListener.pressureUpdate(pressureNotCalibed);
	}*/

	public void startSensing(){
		mUltraPhone.startEverything();
	}

	public void stopSensing(){
		mUltraPhone.stopEverything();
	}
	
	public void startCheckPressure(Point point) {
		Log.v(TAG, "startCheckPressure");
		
		if (mUltraPhone == null)
			return;

		mUltraPhone.startCheckPressure(point);
		mIsRunning = true;
		
	}
	
	public void stopCheckPressure() {
		Log.v(TAG, "stopCheckPressure");
		
		if (mUltraPhone == null)
			return;
		mUltraPhone.stopCheckPressure();
		mIsRunning = false;
	}
	
	public boolean isChecking() {
		return mIsRunning;
	}
	
}
