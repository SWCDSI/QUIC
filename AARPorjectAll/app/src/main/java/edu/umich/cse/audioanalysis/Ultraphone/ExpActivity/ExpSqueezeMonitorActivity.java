package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import edu.umich.cse.audioanalysis.BigMoveDetectorListener;
import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.Ultraphone.Graphic.MonitorView;
import edu.umich.cse.audioanalysis.Ultraphone.UltraphoneControllerListener;

public class ExpSqueezeMonitorActivity extends AppCompatActivity implements UltraphoneControllerListener, BigMoveDetectorListener {
    MonitorView monitorView;
    Button btnStart;
    TextView txtDebugStatus, txtInfo;

    UltraphoneController uc;

    boolean ultraphoneHasStarted;
    boolean pressureSensingIsReady;
    boolean userHasPressed;

    RelativeLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exp_pressure_monitor);



        mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);

        ultraphoneHasStarted = false;
        userHasPressed = false;
        pressureSensingIsReady = false;

        txtInfo = (TextView) findViewById(R.id.textInfo);
        txtDebugStatus = (TextView) findViewById(R.id.txtDebugStatus);

        monitorView = (MonitorView) findViewById(R.id.viewMonitor);
        monitorView.showLines(1,0,200,-1,3);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionStart();
            }
        });
    }




    // NOTE: dispatchTouchEvent can get event even the touch is intercepted by other elements
    /*
    @Override
    public boolean dispatchTouchEvent(MotionEvent event){
        super.dispatchTouchEvent(event);
        int x = (int)event.getX();
        int y = (int)event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                Log.d(C.LOG_TAG, "dispatchTouchEvent: ACTION_DOWN: (x,y) = (" + x + "," + y + ")");
                if(uc!=null && ultraphoneHasStarted) {
                    uc.startCheckPressure(new Point(x, y));
                    userHasPressed = true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                Log.d(C.LOG_TAG, "dispatchTouchEvent: ACTION_UP: (x,y) = (" + x + "," + y + ")");
                if(uc!=null && ultraphoneHasStarted && userHasPressed) {
                    uc.stopCheckPressure();
                }

                break;
            }
        }
        return true;
    }
    */

    @Override
    protected void onResume (){
        super.onResume();
        //uc.startEverything();
    }

    @Override
    protected void onPause (){
        super.onPause();
        //uc.stopEverything();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_exp_pressure_moving_ball, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        Log.d(C.LOG_TAG, "onBackPressed");
        // code here to show dialog
        if(ultraphoneHasStarted) { // need to end the ultraphone
            actionEnd();
        } else {
            super.onBackPressed();  // optional depending on your needs
        }
    }
//===================================================================================
// UI updates
//===================================================================================
    void updateUI(){
        if(ultraphoneHasStarted){
            btnStart.setEnabled(false);

            if(!pressureSensingIsReady){ // ultraphone is enabled but still wait init
                mainLayout.setBackgroundColor(Color.parseColor("#555555"));
                txtInfo.setText("Wait initialization");
            } else {
                mainLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
                txtInfo.setText("Sensing is ready (click back to stop)");
            }

        } else { // ultraphone has been stoped or not inited yet
            txtInfo.setText("Sensing has not started");
            btnStart.setEnabled(true);
            mainLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
    }

    void bigMoveBegan(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainLayout.setBackgroundColor(Color.parseColor("#000055"));
            }
        });

    }

    void bigMoveEnded(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        });
    }

//===================================================================================
// Button actions
//===================================================================================
    // start ultraphone survey based on current device-specific setting
    void actionStart(){
        // init ultraphone controller
        // NOTE: this needs to be initialized every time, so new device setting can be applied
        uc = new UltraphoneController(D.DETECT_SSE, this, getApplicationContext());
        uc.bmd.setCaller(this);
        uc.startCheckSqueezeWhenPossible(); // NOTE: this must be called before startSesning/startEverything
        uc.startEverything();
        ultraphoneHasStarted = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pressureSensingIsReady = true;
                        updateUI();
                    }
                });
            }
        }).start();
        updateUI();
    }

    // end of survey
    void actionEnd(){
        uc.stopCheckSqueeze();
        uc.stopEverything();
        uc = null; // release the previous used controller
        ultraphoneHasStarted = false;
        pressureSensingIsReady = false;
        userHasPressed = false;

        updateUI();
    }


//===================================================================================
// Ultraphone callbacks
//===================================================================================
    @Override
    public void pressureUpdate(final double pressure) {
        // never use this callback here
    }

    @Override
    public void squeezeUpdate(final double check) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //ballMovingView.moveBallByScale(pressure);
                monitorView.addPoint(0, check);
                monitorView.invalidate();
            }
        });
    }

    // This callback is consistent for all caller
    @Override
    public void updateDebugStatus(final String stringToShow) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtDebugStatus.setText(stringToShow);
            }
        });
    }

    // This callback is consistent for all caller
    @Override
    public void showToast(final String stringToShow){
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), stringToShow, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void unexpectedEnd(int code, String reason){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                uc.stopEverything();
                finish();
            }
        });
    }

    // BigMoveDetector call backs
    @Override
    public void statusUpdate(int status) {
        /*
        if (status<=0) { // means normal move range -> bigMove ends
            //if (bodyWasBigMoved && !resetBodyWasBigMovedIsWaittingToTrigger) {
                bigMoveEnded();
            //}
        } else { // bigMove starts
            //if (!bodyWasBigMoved) {
                bigMoveBegan();
            //}
        }
        */
    }


    // NOTE: onTouchEvent will not get the event if event is intercepted by other UI elemnets (such as a button)
    /*
    @Override
    public boolean onTouchEvent(MotionEvent event){

        int x = (int)event.getX();
        int y = (int)event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                Log.d(C.LOG_TAG, "onTouchEvent: ACTION_DOWN: (x,y) = ("+x+","+y+")");
                break;
            }
            case MotionEvent.ACTION_UP: {
                Log.d(C.LOG_TAG, "onTouchEvent: ACTION_UP: (x,y) = ("+x+","+y+")");
                break;
            }
        }
        return true;
    }*/

}

