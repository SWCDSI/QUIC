package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.Network.RemoteTriggerController;
import edu.umich.cse.audioanalysis.Network.RemoteTriggerControllerListener;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.Ultraphone.Graphic.BallMovingView;
import edu.umich.cse.audioanalysis.Ultraphone.UltraphoneControllerListener;

public class ExpPressureMovingBallActivity extends AppCompatActivity implements UltraphoneControllerListener, RemoteTriggerControllerListener {

    BallMovingView ballMovingView;
    Button btnMoveBall;
    TextView txtDebugStatus, txtInstruction;
    double debugScale;

    UltraphoneController uc;
    RemoteTriggerController rtc;

    int boxCnt = 2;
    int boxSelected = -1;
    boolean isTriggerEnabled;
    boolean isTriggerAskedActionFinished;
    boolean needToMoveBall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exp_pressure_moving_ball);

        txtDebugStatus = (TextView) findViewById(R.id.txtDebugStatus);
        txtInstruction = (TextView) findViewById(R.id.txtInstruction);

        ballMovingView = (BallMovingView) findViewById(R.id.viewBallMoving);
        ballMovingView.showBoxs(boxCnt, boxSelected);
        if(C.TRIGGERED_BY_LOCAL){
            localTriggerUpdateBox();
        }

        debugScale = 0;

        isTriggerEnabled = false;
        isTriggerAskedActionFinished = false;
        needToMoveBall = false;

        btnMoveBall = (Button) findViewById(R.id.btnMoveBall);
        /*
        btnMoveBall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugScale = debugScale + 0.1;
                ballView.moveBallByScale(debugScale);
            }
        });*/
        btnMoveBall.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: // need to move the ball
                        needToMoveBall = true;

                        if (rtc!=null && rtc.isConnected()){
                            if (isTriggerEnabled && !isTriggerAskedActionFinished) {
                                rtc.sendTriggerCheck(RemoteTriggerController.TRIGGER_CHECK_TOUCHED);
                            }
                        }

                        uc.startCheckPressure(new Point(0, 0));
                        return true;
                        //break;
                    case MotionEvent.ACTION_UP:
                        needToMoveBall = false;
                        uc.stopCheckPressure();

                        txtInstruction.setText("Done!");
                        txtInstruction.setTextColor(Color.BLUE);


                        // check if the result is correct (local trigger version)
                        if (C.TRIGGERED_BY_LOCAL){
                            boolean inSelectedBox = ballMovingView.isBallInTheSelectedIdx();
                            if(inSelectedBox) {
                                txtInstruction.setText("Success!");
                                txtInstruction.setTextColor(Color.RED);
                                localTriggerCnt ++;
                            } else {
                                txtInstruction.setText("Fail :(");
                                txtInstruction.setTextColor(Color.GRAY);
                            }

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //Do something after 100ms
                                    localTriggerUpdateBox();
                                }
                            }, 1000);

                        }

                        // check if the result is correct (remove trigger version)
                        if (rtc!=null && rtc.isConnected()) {
                            if (isTriggerEnabled && !isTriggerAskedActionFinished) {
                                isTriggerAskedActionFinished = true;
                                boolean inSelectedBox = ballMovingView.isBallInTheSelectedIdx();
                                if(inSelectedBox) {
                                    rtc.sendTriggerCheck(RemoteTriggerController.TRIGGER_CHECK_OK);
                                } else {
                                    rtc.sendTriggerCheck(RemoteTriggerController.TRIGGER_CHECK_NO);
                                }
                            }
                        }
                        return true;
                        //break;
                }
                return false;
            }
        });


        // init ultraphone controller
        uc = new UltraphoneController(D.DETECT_PSE, this, getApplicationContext());


        // connect remote trigger
        if(C.TRIGGERED_BY_NETWORK) {
            rtc = new RemoteTriggerController(this);
        }

    }

    @Override
    protected void onResume (){
        super.onResume();
        uc.startEverything();
        if(rtc!=null){
            rtc.connectServer();
        }
    }

    @Override
    protected void onPause (){
        if(uc!=null) {
            uc.stopEverything();
        }
        if (rtc!=null && rtc.isConnected()){
            rtc.closeServerIfServerIsAlive();
        }
        super.onPause();
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

    int localTriggerCnt = 0;
    void localTriggerUpdateBox (){
        if(localTriggerCnt > 3){ // change to the next scale
            localTriggerCnt = 0;
            boxCnt = boxCnt+1;
        }
        // update UI
        txtInstruction.setText("Move the ball");
        txtInstruction.setTextColor(Color.BLACK);

        Random generator = new Random();
        boxSelected = generator.nextInt(boxCnt);
        ballMovingView.showBoxs(boxCnt, boxSelected);
    }
//===================================================================================
// Ultraphone callbacks
//===================================================================================
    @Override
    public void pressureUpdate(final double pressure) {
        if (needToMoveBall) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    double amp = 0.9*4; // TODO: use my model here
                    ballMovingView.moveBallByScale(pressure*amp);
                }
            });
        }
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
                uc.stopEverything();
                finish();
            }
        });
    }

//===================================================================================
// Start of RemoteServer callbacks
//===================================================================================
    @Override
    public void remoteTriggerIsConnected(final boolean success, String resp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtDebugStatus.setText("remote trigger: "+success);
            }
        });
    }

    @Override
    public void remoteTriggerAskStart(int select) {
        isTriggerEnabled = true;
        isTriggerAskedActionFinished = false;

        // save traces
        uc.setTriggerLog(1, boxCnt, select);

        boxSelected = select;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtInstruction.setText("Move Now!");
                txtInstruction.setTextColor(Color.RED);
                ballMovingView.showBoxs(boxCnt, boxSelected);
            }
        });
    }

    @Override
    public void remoteTriggerAskStop() {
        isTriggerEnabled = false;
        uc.setTriggerLog(2, boxCnt, -1); // code 2 indicates stop

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtInstruction.setText("Action Canceled");
                txtInstruction.setTextColor(Color.GRAY);
                ballMovingView.showBoxs(boxCnt, -1);
            }
        });
    }

    @Override
    public void remoteTriggerAskSave(final String suffix) {
        // go back to the previous activity
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                uc.moveTraceFolderWhenSensingEnded(suffix);
                uc.stopEverything();
                finish();
            }
        });
    }

    @Override
    public void remoteTriggerAskRevoke() {
        // just remember to save this rovoke to local traces
        uc.setTriggerLog(3,-1,-1);  // code 3 indicates ends
    }

    @Override
    public void remoteTriggerChangeScaleTo(final int scale) {
        boxCnt = scale;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ballMovingView.showBoxs(scale, -1);
            }
        });
    }

    @Override
    public void remoteTriggerChangeTarget(String target) {

    }

}
