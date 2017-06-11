package edu.umich.cse.audioanalysis.Ultraphone;

// 2016/05/15: make new interface based on usb readed data

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.Ultraphone.Graphic.TouchCalibrationCircleView;


public class TouchCalibrationActivityOld extends AppCompatActivity {

    // Constant settings
    final static int CALIBRATE_STATUS_INIT = 0;
    final static int CALIBRATE_STATUS_IS_PREPARING_TO_START = 1;
    final static int CALIBRATE_STATUS_IS_STARTED = 2;
    final static int CALIBRATE_STATUS_IS_PREPARING_TO_END = 3;
    final static int CALIBRATE_STATUS_IS_ENDED = 4;
    final static double CALIBRATE_GAP_TIME = 3; // seconds
    final static double CALIBRATE_COLLECT_TIME = 5; // seconds

    // UI-related variables
    Button btnStartOrStop;
    TextView textInfo, textInfo2, textInstruction, textCountDown;
    TouchCalibrationCircleView circleView;
    List<TouchCalibrationCircleView> calibPointList;

    // Inputted train settings
    Point targetLocation;
    int[] trainForceValues;
    String traceSuffix;

    // Internal status
    int currentCalibrateForceIdx, currentCalibrateForceValue;
    boolean isStarted = false;
    int calibrateStatus;
    float countDownValueNow;
    boolean designedAreaIsTouched = false;

    /*
    public ExpTouchCalibrationByExtSensorActivity(Point targetLocationIn, ArrayList<Integer> trainForceValuesIn, String traceSuffixIn){

    }
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        /*
        setContentView(R.layout.activity_touch_calibration);
        Bundle b = getIntent().getExtras();
        targetLocation = new Point(b.getInt("targetX"), b.getInt("targetY"));
        traceSuffix = b.getString("traceSuffix");
        trainForceValues = b.getIntArray("trainForceValues");

        // init some internal status
        currentCalibrateForceIdx = -1;
        isStarted = false;
        calibrateStatus = CALIBRATE_STATUS_INIT;
        countDownValueNow = 0;
        designedAreaIsTouched = false;


        // make a circle indicate the train location
        circleView = new CircleView(this);
        circleView.updateStartPoint(targetLocation);
        circleView.updateColor(Color.GRAY);


        textInstruction = (TextView) findViewById(R.id.textInstruction);
        textInfo = (TextView) findViewById(R.id.textInfo);
        textInfo2 = (TextView) findViewById(R.id.textInfo2);
        btnStartOrStop = (Button) findViewById(R.id.btnStartOrStop);
        btnStartOrStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionStartOrStop();
            }
        });
        */

        // hide status bar
        // ref: http://developer.android.com/training/system-ui/status.html
        // ref: http://stackoverflow.com/questions/8273186/android-show-hide-status-bar-power-bar
        // Hide Status Bar
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = getWindow().getDecorView();
            // Hide Status Bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }

        // get screen resolution -> so we can adapt the size of background based on resolution
        // ref: http://stackoverflow.com/questions/1016896/get-screen-dimensions-in-pixels
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        // get calibration points (TODO: should be inputted by the caller)
        final int CIRCLE_WIDTH = 200;
        final int CALIB_X_CNT = 5;
        final int CALIB_Y_CNT = 6;
        final int MARGIN_X = 20;
        final int MARGIN_Y = 20;
        int CALIB_X_OFFSET = (screenWidth - CIRCLE_WIDTH - MARGIN_X*2)/(CALIB_X_CNT-1);
        int CALIB_Y_OFFSET = (screenHeight - CIRCLE_WIDTH - MARGIN_Y*2)/(CALIB_Y_CNT-1);
        List<Point> TARGET_POINT_LIST = new ArrayList<Point>();
        for(int y=0;y<CALIB_Y_CNT;y++){
            for(int x=0;x<CALIB_X_CNT;x++){
                Point p = new Point(MARGIN_X + CIRCLE_WIDTH/2 + x*CALIB_X_OFFSET , MARGIN_Y + CIRCLE_WIDTH/2 + y*CALIB_Y_OFFSET);
                TARGET_POINT_LIST.add(p);
            }
        }


        RelativeLayout layout = new RelativeLayout(this);
        android.view.ViewGroup.LayoutParams layoutParams;
        layout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        layout.setBackgroundColor(Color.BLACK);

        // make a circle indicate the train location
        circleView = new TouchCalibrationCircleView(this);
        //circleView.updateStartPoint(targetLocation);
        //circleView.updateColor(Color.GRAY);

        layout.addView(circleView);


        // create calibration test points
        calibPointList = new ArrayList<>();
        for(int i=0;i<TARGET_POINT_LIST.size();i++){
            TouchCalibrationCircleView c = new TouchCalibrationCircleView(this);
            c.updateStartPoint(TARGET_POINT_LIST.get(i));
            c.updateRadius(CIRCLE_WIDTH/2);

            layout.addView(c);
            calibPointList.add(c);
        }


        // set layout to this dynamic generated layouts
        setContentView(layout);

        // adjust layout positions
        circleView.setTranslationX(110);
        circleView.setTranslationY(650);

        updateUI();
    }

    void updateUI() {

        /*
        if (!isStarted) { // not started yet
            circleView.updateColor(Color.GRAY);

            btnStartOrStop.setText("Start");


            updateLabelInfos("Not started yet");
        } else {
            circleView.updateColor(Color.YELLOW);


            // update the next level of target to press
            currentCalibrateForceValue = trainForceValues[currentCalibrateForceIdx];

            updateLabelInfos(String.format("Next: %d", currentCalibrateForceValue));
            btnStartOrStop.setText("Stop");
        }
        */
    }

    void updateLabelInfos(String s){
        textInfo.setText(s);
        textInfo2.setText(s);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event){
        super.dispatchTouchEvent(event);

        int x = (int)event.getX();
        int y = (int)event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {

                // check if the hit inside any rect
                // ref: http://stackoverflow.com/questions/11370721/how-do-i-detect-if-a-touch-event-has-landed-within-an-edittext
                // NOTE: this doesn't work -> it seems I hack the way to add view
                Log.d(C.LOG_TAG, "Prssed: ("+x+", "+y+")");
                for(int i=0;i<calibPointList.size();i++){
                    TouchCalibrationCircleView p = calibPointList.get(i);
                    if(p.contain(x,y)){
                        Log.d(C.LOG_TAG, "Found match: ("+p.start.x+", "+p.start.y+")");
                        p.pressed = true;
                        p.invalidate();
                    }
                }

                break;
            }
            case MotionEvent.ACTION_UP: {
                for(int i=0;i<calibPointList.size();i++){
                    TouchCalibrationCircleView p = calibPointList.get(i);
                    p.pressed = false;
                    p.invalidate();
                }
                break;
            }
        }
        return true;
    }



    void prepareToStartCalibrateForce() {
        textInstruction.setText("Prepare to make force");

        currentCalibrateForceValue = trainForceValues[currentCalibrateForceIdx];

        updateLabelInfos(String.format("%d (%d/%d)", currentCalibrateForceValue, currentCalibrateForceIdx+1, trainForceValues.length));


        // give short preparing time when the current target value is for external sensor reference only
        final double waitTimeInSec = currentCalibrateForceValue<0? 1:CALIBRATE_GAP_TIME;

        /*
        if (calibrateStatus == CALIBRATE_STATUS_INIT) {
            // first time of calibrate
            calibrateStatus = CALIBRATE_STATUS_IS_PREPARING_TO_START;
            //[self startCountDown:waitTimeInSec]; // TODO: add count down
            [self performSelector:@selector(startCalibrateForce) withObject:nil afterDelay:waitTimeInSec];
        } else if (calibrateStatus == CALIBRATE_STATUS_IS_ENDED) {
            // go to the next round of calibration
            //[self startCountDown:waitTimeInSec]; // TODO: add count down
            calibrateStatus = CALIBRATE_STATUS_IS_PREPARING_TO_START;
            [self performSelector:@selector(startCalibrateForce) withObject:nil afterDelay:waitTimeInSec];
        }*/
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep((long) (waitTimeInSec*1000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startCalibrateForce();
                    }
                });
            }
        }).start();
    }

    void startCalibrateForce() {
        textInstruction.setText("Keep the force");

        currentCalibrateForceValue = trainForceValues[currentCalibrateForceIdx];

        // TODO: add ultraohone functions
        //[ultraphoneController startCalibrate:currentCalibrateForceValue];

        calibrateStatus = CALIBRATE_STATUS_IS_STARTED;
        Log.d(C.LOG_TAG, String.format("startCalibrateForce: start calibrate for the currentCalibrateForceIdx = %d", currentCalibrateForceIdx));

        getWindow().getDecorView().setBackgroundColor(Color.GREEN);

        prepareToEndCalibrateForce();
    }

    void prepareToEndCalibrateForce() {
        // give short preparing time when the current target value is for external sensor reference only
        final double waitTimeInSec = currentCalibrateForceValue<0? 1:CALIBRATE_COLLECT_TIME;

        // TODO: add countdown back
        //[self startCountDown:waitTimeInSec];


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep((long) (waitTimeInSec*1000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        endCalibrateForce();
                    }
                });
            }
        }).start();
    }

    void endCalibrateForce() {
        calibrateStatus = CALIBRATE_STATUS_IS_ENDED;

        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        // TODO: add ultraphone
        //[ultraphoneController stopCalibrate];

        // go to the next nevels
        currentCalibrateForceIdx += 1;
        if (currentCalibrateForceIdx >= trainForceValues.length) {
            Log.d(C.LOG_TAG,"endCalibrateForce: reaches the end of all calibirate forces, stop this calibration");

            isStarted = false;
            updateUI();

            showSaveTraceAlert();
        } else {
            //[self prepareToStartCalibrateForce];
            updateUI();
        }
    }


    void actionStartOrStop(){
        if (!isStarted) { // need to start
            // start actions for calibiration
            currentCalibrateForceIdx = 0; // start force values from 0
            calibrateStatus = CALIBRATE_STATUS_INIT;

            // update status and UI
            isStarted = true;
            updateUI();

            // start the clibration process
            //[self prepareToStartCalibrateForce];
        } else {
            isStarted = false;
            updateUI();
        }
    }



    double COUNT_DOWN_OFFSET = 1.0;
    // TODO: add countdown back
    /*
    - (void)startCountDown:(double) value {
        // remove the previous countdown events
        [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(keepCountDown) object:nil];
        countDownValueNow = value;

        [self keepCountDown];
    }

    - (void)keepCountDown {
        // update UI
        [labelCountDown setText:[NSString stringWithFormat:@"%0.0f", countDownValueNow]];


        // update count down value
        BOOL endOfCountDown = NO;
        if (countDownValueNow <= 0.0001) {
            endOfCountDown = YES;
        } else {
            countDownValueNow = countDownValueNow - COUNT_DOWN_OFFSET;
            if (countDownValueNow <= 0.0001) { // enforce the update countdown value to >0
                countDownValueNow = 0;
            }
        }

        // make next countdown if necessary
        if (!endOfCountDown) {
            [self performSelector:@selector(keepCountDown) withObject:nil afterDelay:COUNT_DOWN_OFFSET];
        }
    }
    */

    void showSaveTraceAlert(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save the trace?");
        builder.setMessage(String.format("with the suffix = %s", traceSuffix));
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO: popout this activity
            }
        });
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO: add save function
            }
        });

        builder.create().show();
    }

}
