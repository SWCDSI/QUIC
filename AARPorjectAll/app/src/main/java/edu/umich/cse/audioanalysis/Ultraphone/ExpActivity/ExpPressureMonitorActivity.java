package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

import android.graphics.Color;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.Ultraphone.CalibrationController;
import edu.umich.cse.audioanalysis.Ultraphone.Graphic.MonitorView;
import edu.umich.cse.audioanalysis.Ultraphone.Graphic.TouchCalibrationCircleView;
import edu.umich.cse.audioanalysis.Ultraphone.UltraphoneControllerListener;

public class ExpPressureMonitorActivity extends AppCompatActivity implements UltraphoneControllerListener {
    MonitorView monitorView;
    Button btnStart;
    TextView txtDebugStatus, txtInfo;
    Spinner spinnerMonitorScale;

    UltraphoneController uc;

    boolean ultraphoneHasStarted;
    boolean pressureSensingIsReady;
    boolean userHasPressed;

    RelativeLayout mainLayout;

    public static String SELECT_MONITOR_SCALE_TITLES[] = {"2", "1", "0.5", "0.2", "0.1"};
    public static double SELECT_MONITOR_SCALE_VALUES[] = {2, 1, 0.5, 0.2, 0.1};

    CalibrationController calibrationController;

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
        monitorView.showLines(2,0,200,0,1);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionStart();
            }
        });



        spinnerMonitorScale = (Spinner) findViewById(R.id.spinnerMonitorScale);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this,   android.R.layout.simple_spinner_item, SELECT_MONITOR_SCALE_TITLES);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
        spinnerMonitorScale.setAdapter(spinnerArrayAdapter);
        spinnerMonitorScale.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                double selectedScale = SELECT_MONITOR_SCALE_VALUES[position];
                if(monitorView != null) {
                    monitorView.updateRect(0, 200, 0, selectedScale);
                    monitorView.invalidate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // set the default value
        int spinnerPosition = spinnerArrayAdapter.getPosition("1");
        if (spinnerPosition>0) { // otherwise -> use the first value by default
            spinnerMonitorScale.setSelection(spinnerPosition);
        }

        calibrationController = new CalibrationController("", this); // load default setting
        if(C.SHOW_CALIBRATION_LAYOUT && calibrationController.calibrationIsReady) {
            for (int i = 0; i < calibrationController.calibPointList.size(); i++) {
                TouchCalibrationCircleView c = calibrationController.calibPointList.get(i);
                c.updateRadius(200 / 2);
                mainLayout.addView(c);
                mainLayout.invalidate();
            }
        }

        //this.setContentView(mainLayout);

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
                if(calibrationController!=null && calibrationController.calibrationIsReady) {
                    calibrationController.lockTouchLocation(x, y);
                }

                if(uc!=null && ultraphoneHasStarted) {
                    uc.startCheckPressure(new Point(x, y));
                    userHasPressed = true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                Log.d(C.LOG_TAG, "dispatchTouchEvent: ACTION_UP: (x,y) = (" + x + "," + y + ")");
                if(calibrationController!=null && calibrationController.calibrationIsReady) {
                    calibrationController.unlockTouchLocation();
                }

                if(uc!=null && ultraphoneHasStarted && userHasPressed) {
                    uc.stopCheckPressure();
                }

                break;
            }
        }
        return true;
    }

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
            btnStart.setText("Click back button to stop");

            if(!pressureSensingIsReady){ // ultraphone is enabled but still wait init
                mainLayout.setBackgroundColor(Color.parseColor("#555555"));
                txtInfo.setText("Wait initialization");
            } else {
                mainLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
                txtInfo.setText("Touch to sense force");
            }

        } else { // ultraphone has been stoped or not inited yet
            txtInfo.setText("Sensing has stopped");
            btnStart.setText("Start Sensing");
            btnStart.setEnabled(true);
            mainLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
    }

//===================================================================================
// Button actions
//===================================================================================
    // start ultraphone survey based on current device-specific setting
    void actionStart(){
        // init ultraphone controller
        // NOTE: this needs to be initialized every time, so new device setting can be applied
        uc = new UltraphoneController(D.DETECT_PSE, this, getApplicationContext());
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
        final double pressureCalibed;
        if(calibrationController!=null && calibrationController.calibrationIsReady){
            pressureCalibed = calibrationController.getLockedCalibResult(pressure);
        } else {
            pressureCalibed = pressure;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //ballMovingView.moveBallByScale(pressure);
                monitorView.addPoint(0, pressure);
                monitorView.addPoint(1, pressureCalibed);
                monitorView.invalidate();
            }
        });
    }

    @Override
    public void squeezeUpdate(double check) {
        // never use this callback here
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
                //uc.stopEverything();
                actionEnd();
                //finish();
            }
        });
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

