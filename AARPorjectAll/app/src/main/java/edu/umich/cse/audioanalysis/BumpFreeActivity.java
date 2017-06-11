package edu.umich.cse.audioanalysis;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/*
* 2015/10/05: This activity is used to survey BumpFree audio response
* 2015/10/27: Add log controller logic to remember sensed data
* */

public class BumpFreeActivity extends AppCompatActivity  implements SurveyEndListener, MySensorControllerListener{

    Button btnSelectVol;
    Button btnSelectSound;
    Button btnStartSurvey;
    Button btnSelectTilt;
    Button btnTag;
    TextView txtVol;
    TextView txtSound;
    TextView txtResult;
    TextView txtTilt;
    TextView txtTiltSensed;
    TextView txtTagInfo;

    // here is the test parameter setting
    int RECORDER_SAMPLERATE = 48000;
    float DEFAULT_VOL = 0.5f;
    float[] VOLS = {1.0f, 0.5f, 0.1f, 0.05f};
    CharSequence[] TILT_OPTS = {"0", "30", "45", "60", "90", "change"};
    String tiltOptSelected = null;
    float volSelected = C.DEFAULT_VOL;

    int DEFAULT_TAG_DISTANCE  = 8; // start from 10m away from objects
    int tagDistanceNow = 0;

    SpectrumSurvey ss;
    MySensorController msc;
    LogController lc;

    JniController jc;
    /*
    static {
            System.loadLibrary("detection"); // load jni libarary
        }

        public native void helloLog(String logThis);
    */

    /*
    CharSequence[] SOUND_NAMES = {"mid", "narrow", "ultra","beepmid","beephigh","hammingmid","hammingmid10hz"};
    String[] SOUND_SETTINGS = {
            "48000rate-30repeat-24000period+chirp-10000Hz-22000Hz-1200samples+pilotchirp"
            ,"48000rate-30repeat-24000period+chirp-18000Hz-22000Hz-1200samples+pilotchirp"
            ,"48000rate-30repeat-24000period+chirp-20000Hz-24000Hz-1200samples+pilotchirp"
            ,"48000rate-50repeat-4800period+chirp-12000Hz-12000Hz-30samples+pilotchirp"
            ,"48000rate-50repeat-4800period+chirp-20000Hz-20000Hz-30samples+pilotchirp"
            ,"48000rate-30repeat-24000period+chirp-18000Hz-24000Hz-1200samples+customhamming+pilotchirp" // custom hamming window size = 600, 2hz sweep rate
            ,"48000rate-50repeat-4800period+chirp-18000Hz-24000Hz-1200samples+chwin600+pilotchirp"
            };
    */

    // final setting
    /*
    CharSequence[] SOUND_NAMES = {"custompilot20Hzlong"};
    String[] SOUND_SETTINGS = {"48000rate-5000repeat-2400period+chirp-18000Hz-24000Hz-1200samples+namereduced"};
    */
    CharSequence[] SOUND_NAMES = {"ultrasound_half_cycle","ultrasound_full_cycle","tone_12kHz","tone_22kHz"};
    String[] SOUND_SETTINGS = {"48000rate-5000repeat-2400period+chirp-18000Hz-24000Hz-1200samples+namereduced"
        ,"48000rate-5000repeat-1200period+chirp-18000Hz-24000Hz-1200samples+namereduced"
        ,"48000rate-1repeat-6000000period+chirp-12000Hz-12000Hz-6000000samples+pilotchirprepeat"
        ,"48000rate-1repeat-6000000period+chirp-20000Hz-20000Hz-6000000samples+pilotchirprepeat"};

    String soundNameSelected = null;
    String soundSettingSelected = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bump_free);

        if(C.FORCE_TO_USE_TOP_SPEAKER){
            Log.w(C.LOG_TAG, "Top speaker is using");
            // ref: http://stackoverflow.com/questions/2119060/android-getting-audio-to-play-through-earpiece
            AudioManager m_amAudioManager;
            m_amAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            m_amAudioManager.setMode(AudioManager.MODE_IN_CALL);
            m_amAudioManager.setSpeakerphoneOn(false);
        }


        // connect UI
        btnSelectVol = (Button) findViewById(R.id.btnSelectVol);
        btnSelectVol.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectVol();
            }
        });
        btnSelectSound = (Button) findViewById(R.id.btnSelectSound);
        btnSelectSound.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectSound();
            }
        });
        btnSelectTilt = (Button) findViewById(R.id.btnSelectTilt);
        btnSelectTilt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectTilt();
            }
        });
        btnStartSurvey = (Button) findViewById(R.id.btnStartSurvey);
        btnStartSurvey.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(ss == null || !ss.isSurveying) {
                    startSurvey();
                    btnStartSurvey.setText("Stop Survey");
                } else {
                    stopSurvey();
                    btnStartSurvey.setText("Wait survey ends");
                    btnStartSurvey.setEnabled(false);
                }
            }
        });
        btnTag = (Button) findViewById(R.id.btnTag);
        btnTag.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tagGroundTruth();
            }
        });

        txtVol = (TextView) findViewById(R.id.txtVol);
        txtSound = (TextView) findViewById(R.id.txtSound);
        txtResult = (TextView) findViewById(R.id.txtResult);
        txtTilt = (TextView) findViewById(R.id.txtTilt);
        txtTiltSensed = (TextView) findViewById(R.id.txtTiltSensed);
        txtTagInfo = (TextView) findViewById(R.id.txtTagInfo);

        // make defaut value
        C.DEFAULT_VOL = this.DEFAULT_VOL; // update the global vols
        txtVol.setText("Vol = " + C.DEFAULT_VOL + " (default)");

        // set sound to the first posiible value
        soundNameSelected = (String)SOUND_NAMES[0];
        soundSettingSelected = SOUND_SETTINGS[0];
        txtSound.setText("Sound = " + soundNameSelected + " (default)");

        // connect sensor controller
        msc = new MySensorController(this, (SensorManager) getSystemService(Context.SENSOR_SERVICE), C.DEBUG_FOLDER, 1000);


        // disalbe most ui if testing vibration
        btnTag.setEnabled(false);
        btnSelectTilt.setEnabled(false);
        btnSelectVol.setEnabled(false);
    }

//===================================================================================
//                                UI actions
//===================================================================================

    void tagGroundTruth(){
        if(ss.isSurveying){
            if(C.TRACE_SAVE_TO_FILE && tagDistanceNow > 0) {
                //lc.save(ss.audioTotalRecordedSampleCnt, LogController.TAG_METER_FROM_OBJECT, tagDistanceNow);
                lc.addLogAndOutputDirectly(ss.audioTotalRecordedSampleCnt, LogController.TAG_METER_FROM_OBJECT, tagDistanceNow, 0, 0);
                tagDistanceNow = tagDistanceNow - 1;

                if (tagDistanceNow == 0) {
                    txtTagInfo.setText("Done");
                } else {
                    txtTagInfo.setText(String.format("%d (m)", tagDistanceNow));
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "[WARN]: Tag needs to be set when survey is runing", Toast.LENGTH_LONG).show();
        }
    }

    int selectedIdx = -1;
    // action functions
    void selectVol(){
        selectedIdx = -1;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        CharSequence[] array = new CharSequence[VOLS.length];
        for(int i=0;i<VOLS.length;i++){
            array[i] = String.format("%f", VOLS[i]);
        }
        builder.setTitle("Please select vol");
        builder.setSingleChoiceItems(array, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                selectedIdx = which;
            }
        });
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if(selectedIdx>=0) {
                    volSelected = VOLS[selectedIdx];
                    C.DEFAULT_VOL = volSelected;
                    txtVol.setText("Vol = " + C.DEFAULT_VOL);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        Dialog dialog = builder.create();
        dialog.show();
    }


    void selectTilt(){
        selectedIdx = -1;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Please select tilt");
        builder.setSingleChoiceItems(TILT_OPTS, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                selectedIdx = which;
            }
        });
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if(selectedIdx>=0) {
                    tiltOptSelected = (String)TILT_OPTS[selectedIdx];
                    txtTilt.setText("Tilt = "+tiltOptSelected);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                tiltOptSelected = null;
                txtTilt.setText("Tilt = ? (optional)");
            }
        });

        Dialog dialog = builder.create();
        dialog.show();
    }

    void selectSound(){
        selectedIdx = -1;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Please select vol");
        builder.setSingleChoiceItems(SOUND_NAMES, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                selectedIdx = which;
            }
        });
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if(selectedIdx>=0) {
                    soundNameSelected = (String) SOUND_NAMES[selectedIdx];
                    soundSettingSelected = SOUND_SETTINGS[selectedIdx];
                    txtSound.setText("Sound = " + soundNameSelected + "\n"+soundSettingSelected);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        Dialog dialog = builder.create();
        dialog.show();
    }

    void startSurvey(){
        ss = new SpectrumSurvey(RECORDER_SAMPLERATE, soundSettingSelected, this);

        boolean initSuccess = ss.initSurvey(this, C.SURVEY_MODE_TRAIN); // reuse the train as chirps mode
        if(initSuccess==false){
            Toast.makeText(this, "Please wait the previos sensing ends", Toast.LENGTH_LONG).show();
        } else {
            lc = new LogController(C.appFolderPath+C.DEBUG_FOLDER, null); // NOTE the folder should be consistent to what data is saved to
            jc = new JniController(C.appFolderPath+C.DEBUG_FOLDER+C.JNI_LOG_FOLER);
            tagDistanceNow = DEFAULT_TAG_DISTANCE;
            msc.startRecord(C.appFolderPath+C.DEBUG_FOLDER);
            txtTagInfo.setText(String.format("%d (m)", tagDistanceNow));
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

        //
        selectedIdx = -1;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);


        builder.setTitle("Please select vol");
        builder.setSingleChoiceItems(folderSuffixArray, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                selectedIdx = which;
            }
        });
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if(selectedIdx>=0) {
                    // only work in this option: CharSequence[] folderSuffixArray = {"5m","4m","3m","2m","1m","move","other"};
                    CharSequence meterAwayNow = folderSuffixArray[selectedIdx];
                    if(meterAwayNow.charAt(meterAwayNow.length()-1)=='m'){
                        int meterCode = Integer.parseInt(meterAwayNow.subSequence(0,meterAwayNow.length()-1).toString());
                        //lc.save(0,LogController.TAG_METER_FROM_OBJECT,0); // set init
                        //lc.save(ss.audioTotalRecordedSampleCnt,LogController.TAG_METER_FROM_OBJECT,1); // set init
                        lc.addLogAndOutputDirectly(0,LogController.TAG_METER_FROM_OBJECT, meterCode, 0, 0);
                        lc.addLogAndOutputDirectly(ss.audioTotalRecordedSampleCnt,LogController.TAG_METER_FROM_OBJECT, meterCode, 0, 0);
                    } else {
                        Toast.makeText(getApplicationContext(), "[WARN]: log is not saved, select unsupport name = "+meterAwayNow, Toast.LENGTH_LONG);
                    }


                    String newFolderPath = String.format("%s_%03d_%s", C.appFolderPath + C.DEBUG_FOLDER.substring(0,C.DEBUG_FOLDER.length()-1), (int)(volSelected*100), soundNameSelected);
                    if(tiltOptSelected != null){
                        newFolderPath = newFolderPath+"_"+tiltOptSelected+"tilt";
                    }

                    newFolderPath = newFolderPath+"_"+folderSuffixArray[selectedIdx];
                    moveResultFolder(newFolderPath);
                }

                // NOTE: need to release ss whenever it is possible
                ss = null;
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                ss = null;
            }
        });
        Dialog dialog = builder.create();
        dialog.show();

        txtResult.setText("Survey completes!");
    }

    @Override
    public void audioRecorded(byte[] data, long audioRecordedSampleCnt) {
        // update sensor data
        msc.outputMotion(ss.audioTotalRecordedSampleCnt);
        if(C.TRACE_REALTIME_PROCESSING){
            if(audioRecordedSampleCnt <= 48000){ // only for debug
            //if(true){
                int result = jc.addAudioSamples(data);
                if(result==-1){
                    Log.e(C.LOG_TAG, "[ERROR]: unable to find pilot (wrong pilot setting?)");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "[ERROR] pilot is not synced", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                if(result>20){
                    Log.d(C.LOG_TAG, "result of jni add = "+result);
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

//===================================================================================
//                            other callback functions
//===================================================================================

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
        txtTiltSensed.setText("Tilt(v) = "+Math.ceil(MySensorController.radiusToDegree(tiltZ)));
    }

    @Override
    public void onRecordedEnd() {

    }
}
