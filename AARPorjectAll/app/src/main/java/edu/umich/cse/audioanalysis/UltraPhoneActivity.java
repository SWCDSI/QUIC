package edu.umich.cse.audioanalysis;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/*
* 2015/11/12
* */

public class UltraPhoneActivity extends AppCompatActivity  implements SurveyEndListener, MySensorControllerListener{


    Button btnStartSurvey;
    Button btnPressureTest;
    TextView txtResult;
    TextView txtPressure;


    // here is the test parameter setting
    int RECORDER_SAMPLERATE = 48000;
    float volSelected = C.DEFAULT_VOL;
    String soundNameSelected = "default";
    String soundSettingSelected = "48000rate-5000repeat-2400period+chirp-18000Hz-24000Hz-1200samples+namereduced"; // 4min, 20Hz version


    SpectrumSurvey ss;
    MySensorController msc;
    LogController lc;
    JniController jc;
    BigMoveDetector bmd;

    int bigMoveStatus;

    boolean needToRecordPressure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ultra_phone);
        needToRecordPressure = false;

        // connect UI
        btnStartSurvey = (Button) findViewById(R.id.btnStartSurvey);
        btnStartSurvey.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ss == null || !ss.isSurveying) {
                    startSurvey();
                    btnStartSurvey.setText("Stop Survey");
                } else {
                    stopSurvey();
                    btnStartSurvey.setText("Wait survey ends");
                    btnStartSurvey.setEnabled(false);
                }
            }
        });
        txtResult = (TextView) findViewById(R.id.txtResult);


        btnPressureTest = (Button) findViewById(R.id.btnPressureTest);
        btnPressureTest.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch ( event.getAction() ) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(C.LOG_TAG, "PressureTest button is on");
                        // NOTE: pseRef must be set before needToRecordPressure is set to true
                        if(C.TRACE_REALTIME_PROCESSING && jc != null){
                            // NOTE: this method is deprecated
                            //jc.setPseRefToNow(); // update pressure sensitive estimation reference point
                        }
                        needToRecordPressure = true;

                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(C.LOG_TAG, "PressureTest button is off");
                        needToRecordPressure = false;
                        break;
                }
                return true;
            }
        });
        txtPressure = (TextView) findViewById(R.id.txtPressure);
        txtPressure.setText("Pressure is connected");

        bmd = new BigMoveDetector();
        bigMoveStatus = 0;

        // connect sensor controller
        msc = new MySensorController(this, (SensorManager) getSystemService(Context.SENSOR_SERVICE), C.DEBUG_FOLDER, 1000);
    }

//===================================================================================
//                                UI actions
//===================================================================================

    void startSurvey(){
        ss = new SpectrumSurvey(RECORDER_SAMPLERATE, soundSettingSelected, this);

        boolean initSuccess = ss.initSurvey(this, C.SURVEY_MODE_TRAIN); // reuse the train as chirps mode
        if(initSuccess==false){
            Toast.makeText(this, "Please wait the previos sensing ends", Toast.LENGTH_LONG).show();
        } else {
            lc = new LogController(C.appFolderPath+C.DEBUG_FOLDER, null); // NOTE the folder should be consistent to what data is saved to
            jc = new JniController(C.appFolderPath+C.DEBUG_FOLDER+C.JNI_LOG_FOLER);
            msc.startRecord(C.appFolderPath +C.DEBUG_FOLDER);
            txtResult.setText("Wait survey ends");
            ss.startSurvey();
        }
    }

    void stopSurvey(){
        ss.stopSurvey();
    }

    CharSequence[] folderSuffixArray = {"5m","4m","3m","2m","1m","move","other"};
    public void onSurveyEnd() {
        // close necessary componenet
        msc.stopRecord();
        btnStartSurvey.setText("Start survey");
        btnStartSurvey.setEnabled(true);

        // release ss
        ss = null;
    }

    float pseResult;
    @Override
    public void audioRecorded(byte[] data, long audioRecordedSampleCnt) {
        // update sensor data
        // estimate if there is big movmement
        bigMoveStatus = bmd.update(ss.audioTotalRecordedSampleCnt, msc.accMag, msc.gyroMag);
        msc.outputMotion(ss.audioTotalRecordedSampleCnt);

        if(C.TRACE_REALTIME_PROCESSING){
            //if(audioRecordedSampleCnt <= 480000){ // only for debug
            if(true){
                int result = jc.addAudioSamples(data);
                if(result==-1){
                    Log.e(C.LOG_TAG, "[ERROR]: unable to find pilot (wrong pilot setting?)");
                }

                if(result>20){
                    Log.d(C.LOG_TAG, "result of jni add = " + result);
                }

                if(result>5 && needToRecordPressure){
                    // NOTE: this method is deprecated
                    //pseResult = jc.getPseResult();
                    Log.d(C.LOG_TAG, "pse Pressure = "+pseResult);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            txtPressure.setText("P=" + pseResult);
                        }
                    });
                }
            } else {
                Log.d(C.LOG_TAG, "stop at deubg audio point");
            }
        }
    }

    void moveResultFolder(String newFolderPath){
        String oriFolderPath = C.appFolderPath + C.DEBUG_FOLDER;
        File oldFolder = new File(oriFolderPath);

        int trialIdx = 1;
        while(true){
            String newFolderTrial = newFolderPath+"_"+trialIdx+"/";

            File newFolder = new File(newFolderTrial);
            if(newFolder.exists()){
                trialIdx += 1; // check the next trial index
            } else {
                oldFolder.renameTo(newFolder);
                Toast.makeText(this, "Trace has been moved to "+newFolderTrial, Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    // other callback functions
    public static final int MENU_ADD = Menu.FIRST;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_bump_free, menu);
        menu.add(Menu.NONE, MENU_ADD, Menu.NONE, "Go to AudioAnalysisActivity");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id){
            case MENU_ADD:
                Log.d(C.LOG_TAG, "MENU_ADD");
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

        /*
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
        */
    }


    @Override
    public void onTiltChanged(double tiltX, double tiltY, double tiltZ) {
        //txtTiltSensed.setText("Tilt(v) = "+Math.ceil(MySensorController.radiusToDegree(tiltZ)));
    }

    @Override
    public void onRecordedEnd() {

    }
}
