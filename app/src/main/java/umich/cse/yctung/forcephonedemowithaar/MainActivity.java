package umich.cse.yctung.forcephonedemowithaar;

import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.Ultraphone.ExpActivity.UltraphoneController;
import edu.umich.cse.audioanalysis.Ultraphone.UltraphoneControllerListener;

public class MainActivity extends AppCompatActivity implements UltraphoneControllerListener {
    UltraphoneController uc;
    String LOG_TAG = "ForcePhoneDemo";
    Boolean ultraphoneHasStarted;
    Boolean pressureSensingIsReady;
    Boolean userHasPressed;
    TextView textResult;

    boolean useRemoteMatlabModeInsteadOfStandaloneMode;
    boolean checkPressureInsteadOfSqueeze;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textResult = (TextView) findViewById(R.id.textResult);

        useRemoteMatlabModeInsteadOfStandaloneMode = true; // switch to use different mode of parsing
        checkPressureInsteadOfSqueeze = true; // switch to use different mode of aar lib

        if (useRemoteMatlabModeInsteadOfStandaloneMode) {
            //control global variables to enable the remote mode
            C.SERVER_ADDR = "10.0.0.229";
            // NOTE: the following two flags must be mutual exclusive
            C.TRACE_REALTIME_PROCESSING = false;
            C.TRACE_SEND_TO_NETWORK = true;
        }

        if (checkPressureInsteadOfSqueeze) uc = new UltraphoneController(D.DETECT_PSE, this, getApplicationContext());
        else {
            uc = new UltraphoneController(D.DETECT_SSE, this, getApplicationContext());
            uc.startCheckSqueezeWhenPossible(); // e.g., when the server is connected
        }
        
        ultraphoneHasStarted = false;
        pressureSensingIsReady = false;
        userHasPressed = false;
    }

    @Override
    protected void onResume (){
        super.onResume();
        uc.startEverything();
        ultraphoneHasStarted = true;
    }

    @Override
    protected void onPause (){
        super.onPause();
        uc.stopEverything();
        ultraphoneHasStarted = false;
        pressureSensingIsReady = false;
        userHasPressed = false;
    }

    // NOTE: dispatchTouchEvent can get event even the touch is intercepted by other elements
    @Override
    public boolean dispatchTouchEvent(MotionEvent event){
        super.dispatchTouchEvent(event);
        int x = (int)event.getX();
        int y = (int)event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                Log.d(C.LOG_TAG, "dispatchTouchEvent: ACTION_DOWN: (x,y) = (" + x + "," + y + ")");

                if(uc!=null && checkPressureInsteadOfSqueeze && ultraphoneHasStarted) {
                    uc.startCheckPressure(new Point(x, y)); // this will trigger the UltraphoneController to start send data to the callback
                    userHasPressed = true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                Log.d(C.LOG_TAG, "dispatchTouchEvent: ACTION_UP: (x,y) = (" + x + "," + y + ")");
                if(uc!=null && checkPressureInsteadOfSqueeze && ultraphoneHasStarted && userHasPressed) {
                    uc.stopCheckPressure();
                }
                break;
            }
        }
        return true;
    }


    /* Ultraphone callbacks */

    @Override
    public void pressureUpdate(final double v) {
        Log.d(LOG_TAG, "pressure = "+v);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textResult.setText(String.format("pressure = %.2f", v));
            }
        });
    }

    @Override
    public void squeezeUpdate(final double reference) {
        Log.d(LOG_TAG, "reference = "+reference);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textResult.setText(String.format("ref = %.2f", reference));
            }
        });
    }

    @Override
    public void updateDebugStatus(final String s) {
        Log.d(LOG_TAG,"Debug: "+s);
    }

    @Override
    public void showToast(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void unexpectedEnd(int i, final String s) {
        Log.e(LOG_TAG,"Error: "+s);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();
            }
        });
    }
}
