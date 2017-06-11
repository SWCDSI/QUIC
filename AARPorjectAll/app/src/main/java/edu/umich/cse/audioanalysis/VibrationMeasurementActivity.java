package edu.umich.cse.audioanalysis;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import edu.umich.cse.audioanalysis.Network.NetworkController;
import edu.umich.cse.audioanalysis.Network.NetworkControllerForVibrationMeasurement;
import edu.umich.cse.audioanalysis.Network.NetworkControllerListener;

/*
* 2015/10/05: This activity is used to survey BumpFree audio response
* 2015/10/27: Add log controller logic to remember sensed data
* 2015/11/22: This is changed to used for vibration measurement at ME
* */

public class VibrationMeasurementActivity extends AppCompatActivity  implements SurveyEndListener, MySensorControllerListener, NetworkControllerListener {

    Button btnSelectSound;
    Button btnStartSurvey;
    Button btnConnectNetwork;
    TextView txtSound;
    TextView txtResult;

    // here is the test parameter setting
    int RECORDER_SAMPLERATE = 48000;

    CharSequence[] TILT_OPTS = {"0", "30", "45", "60", "90", "change"};
    String tiltOptSelected = null;
    float volSelected = C.DEFAULT_VOL;

    SpectrumSurvey ss;
    MySensorController msc;
    LogController lc;

    NetworkControllerForVibrationMeasurement nc;


    //final String SERVER_IP = "10.0.0.12"; // Umich5566 in my office
    //String SERVER_IP = "192.168.1.143"; // Home
    String SERVER_IP = "35.2.107.98"; // ME Mwireless

    final int SERVER_PORT = 50009;

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
        setContentView(R.layout.activity_vibration_measurement);

        if(C.FORCE_TO_USE_TOP_SPEAKER){
            Log.w(C.LOG_TAG, "Top speaker is using");
            // ref: http://stackoverflow.com/questions/2119060/android-getting-audio-to-play-through-earpiece
            AudioManager m_amAudioManager;
            m_amAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            m_amAudioManager.setMode(AudioManager.MODE_IN_CALL);
            m_amAudioManager.setSpeakerphoneOn(false);
        }


        btnSelectSound = (Button) findViewById(R.id.btnSelectSound);
        btnSelectSound.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectSound();
            }
        });

        btnConnectNetwork = (Button) findViewById(R.id.btnConntectNetwork);
        btnConnectNetwork.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                actionConnectNetwork();
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

        txtSound = (TextView) findViewById(R.id.txtSound);
        txtResult = (TextView) findViewById(R.id.txtResult);


        // set sound to the first posiible value
        soundNameSelected = (String)SOUND_NAMES[0];
        soundSettingSelected = SOUND_SETTINGS[0];
        txtSound.setText("Sound = " + soundNameSelected + " (default)");

        // connect sensor controller
        msc = new MySensorController(this, (SensorManager) getSystemService(Context.SENSOR_SERVICE), C.DEBUG_FOLDER, 1000);

        nc = new NetworkControllerForVibrationMeasurement(this);
    }

//===================================================================================
//                                UI actions
//===================================================================================


    void actionConnectNetwork(){
        txtResult.setText("Wait connection to server...");
        nc.connectServer(SERVER_IP, SERVER_PORT);
    }

    int selectedIdx = -1;

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
            msc.startRecord(C.appFolderPath+C.DEBUG_FOLDER);
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


                ss = null;
    }

    @Override
    public void audioRecorded(byte[] data, long audioRecordedSampleCnt) {
        // update sensor data

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
        //txtTiltSensed.setText("Tilt(v) = "+Math.ceil(MySensorController.radiusToDegree(tiltZ)));
    }

    @Override
    public void onRecordedEnd() {

    }

    public void isConnected(boolean success, final String resp) {
        if(success){
            // send init set actions
            nc.sendSetAction(NetworkController.SET_TYPE_STRING, "matlabSourceMatName", ("source_"+soundSettingSelected).getBytes());
            nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, "traceChannelCnt", "2".getBytes());
            nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, "traceVol", "0.5".getBytes());
            nc.sendInitAction();
            //nc.sendSetAction(NetworkController.SET_TYPE_INT, "traceChannelCnt", ByteBuffer.allocate(4).putInt(2).array());
            //nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING,"vol","0.5".getBytes());

            // update UI
            //isConnecting = true;
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtResult.setText("Unable to connect server");
                }
            });

            //Toast.makeText(getApplicationContext(),"[ERROR]: unable to connect server : " + resp,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int consumeReceivedData(final double dataReceived) {
        final int cmd = (int)Math.round(dataReceived);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtResult.setText("Recevied = " + dataReceived + "(" + cmd + ")");

                if(cmd >= 0) {
                    selectedIdx = cmd;
                    soundNameSelected = (String) SOUND_NAMES[selectedIdx];
                    soundSettingSelected = SOUND_SETTINGS[selectedIdx];
                    txtSound.setText("Sound = " + soundNameSelected);
                    startSurvey();
                } else if (cmd == -1) {
                    stopSurvey();
                } else if (cmd == -3) { // set to front microphone
                    C.FORCE_TO_USE_TOP_SPEAKER = true;
                    txtResult.setText("Force to use front speaker");
                } else if (cmd == -4) {
                    C.FORCE_TO_USE_TOP_SPEAKER = false;
                    txtResult.setText("Use bottom speaker");
                }
            }
        });

        return 0;
    }
}
