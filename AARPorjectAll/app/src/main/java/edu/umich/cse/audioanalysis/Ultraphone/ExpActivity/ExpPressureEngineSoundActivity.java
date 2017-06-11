package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.Ultraphone.Graphic.GaugeView;
import edu.umich.cse.audioanalysis.Ultraphone.UltraphoneControllerListener;
import edu.umich.cse.audioanalysis.Utils;

public class ExpPressureEngineSoundActivity extends AppCompatActivity implements UltraphoneControllerListener {

    static final boolean DEBUG_MODE = true; // shows the UI for start/stop sound and hide the UI for gas pedal

    AudioTrack mAudioTrack;
    public final static int FS = 48000;
    public final static int BYTE_PER_ELEMENT = 2;
    public final static int PLAYER_TOTAL_BUFFER_SIZE = 960*4*BYTE_PER_ELEMENT;

    boolean isAudioKeepPlaying = false;


    ImageButton btnStartOrStop, btnGasPadel;
    TextView txtDebugStatus, txtDebugFreqIdx;
    SeekBar seekBar;
    GaugeView gaugeView;

    int freqIdxToPlay = 0; // This value shoud be smaller than DATA_FREQ_CNT

    //double PRESSURE_SCALE_MAX = 0.5; // max amount of pressure to trigger the max engine sound

    Spinner spinnerThres;

    public static double ENGINE_MAX_VALES[] = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};

    UltraphoneController uc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exp_pressure_engine_sound);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, FS, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, PLAYER_TOTAL_BUFFER_SIZE, AudioTrack.MODE_STREAM);
        prepareAudioDateFromEngineSound();

        txtDebugStatus = (TextView) findViewById(R.id.txtDebugStatus);
        txtDebugFreqIdx = (TextView) findViewById(R.id.txtDebugFreqIdx);

        btnStartOrStop = (ImageButton) findViewById(R.id.buttonStartOrStop);
        btnStartOrStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isAudioKeepPlaying){ // need to start
                    startAudioPlaying();
                    //btnStartOrStop.setText("Stop");
                    btnStartOrStop.setImageResource(R.drawable.start_button_pressed);
                } else { // need to stop
                    stopAudioPlaying();
                    //btnStartOrStop.setText("Start");
                    btnStartOrStop.setImageResource(R.drawable.start_button);
                }
            }
        });

        gaugeView = (GaugeView) findViewById(R.id.gaugeView);

        seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setMax(DATA_FREQ_CNT-1);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateFreqIdx(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        btnGasPadel = (ImageButton) findViewById(R.id.buttonGasPadel);
        btnGasPadel.setImageResource(R.drawable.gas_padel);
        btnGasPadel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: // need to move the ball
                        uc.startCheckPressure(new Point(0, 0));
                        btnGasPadel.setImageResource(R.drawable.gas_padel_pressed);
                        return true;
                    case MotionEvent.ACTION_UP:
                        uc.stopCheckPressure();
                        updateFreqIdx(0);
                        btnGasPadel.setImageResource(R.drawable.gas_padel);
                        return true;
                }
                return false;
            }
        });

        spinnerThres = (Spinner)findViewById(R.id.spinnerThres);
        String [] spinnerTitles = Utils.doubleArrayToStringArray(ENGINE_MAX_VALES);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this,   android.R.layout.simple_spinner_item, spinnerTitles);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
        spinnerThres.setAdapter(spinnerArrayAdapter);
        spinnerThres.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                double selectedThres = ENGINE_MAX_VALES[position];
                D.appPressureEngineSoundScaleMax = selectedThres;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // set the default value
        int spinnerPosition = 0;
        for(int i =0;i<ENGINE_MAX_VALES.length; i++){
            if(ENGINE_MAX_VALES[i]==D.appPressureEngineSoundScaleMax){
                spinnerPosition = i;
                break;
            }
        }
        if (spinnerPosition>0) { // otherwise -> use the first value by default
            spinnerThres.setSelection(spinnerPosition);
        }


        // init ultraphone controller
        uc = new UltraphoneController(D.DETECT_PSE, this, getApplicationContext());
    }

    @Override
    protected void onResume (){
        super.onResume();
        uc.startEverything();
    }

    @Override
    protected void onPause (){
        if(uc!=null) {
            uc.stopEverything();
        }
        if(isAudioKeepPlaying){
            stopAudioPlaying();
        }
        super.onPause();
    }

//==================================================================================================
// Public interface
//==================================================================================================
    void startAudioPlaying(){
        if(isAudioKeepPlaying){
            Log.w(C.LOG_TAG, "Audio can't be played twice, something is wrong");
        } else {
            isAudioKeepPlaying = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                    mAudioTrack.play();
                    keepAudioPlaying();
                }
            }).start();
        }
    }

    void stopAudioPlaying(){
        if(!isAudioKeepPlaying){
            Log.w(C.LOG_TAG, "Audio can't be stopped when it is not played, something is wrong");
        } else {
            isAudioKeepPlaying = false; // NOTE: set it false will end the audio playing loop automatically
        }
    }


//==================================================================================================
// Internal audio processing functions
//==================================================================================================
    public short[][] dataTable;
    public static final int DATA_FREQ_CNT = 50, DATA_TIME_CNT = 960*4;
    // NOTE: this engine sound setting must be consistent with matlab setting (for engineSound.dat)
    private void prepareAudioDateFromEngineSound(){
        // read 16-bit data from asset

        // loop to create short array
        // NOTE: it is a in-efficient way, but keep the flexibility of modifying code in java
        dataTable = new short[DATA_FREQ_CNT][DATA_TIME_CNT];

        try {
            InputStream is = getAssets().open("engineSoundShortAmp20000.dat");
            byte[] fileBytes=new byte[DATA_TIME_CNT*BYTE_PER_ELEMENT];

            if(is.available()!=DATA_FREQ_CNT*DATA_TIME_CNT*BYTE_PER_ELEMENT){
                Log.e(C.LOG_TAG, "[ERROR]: engineSound.data file size inconsistent");
            }

            for(int fIdx = 0; fIdx < DATA_FREQ_CNT; fIdx++){
                is.read(fileBytes, 0, DATA_TIME_CNT*BYTE_PER_ELEMENT);
                ByteBuffer.wrap(fileBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(dataTable[fIdx]);

                // *** this is only for debug ***
                /*
                String debugString = "dataDebug=[";
                for (int i=0;i<DATA_TIME_CNT;i++){
                    debugString = debugString+dataTable[fIdx][i]+",";
                }
                debugString += "];";
                */

                Log.d(C.LOG_TAG, "Load fIdx = "+fIdx+" ends");
            }
            is.close();
        } catch (IOException e) {
            Utils.showSimpleAlertDialog(this, "Error", "Unable to read engine sound from asset, error = "+e.getMessage());
            e.printStackTrace();
        }
    }

    private void keepAudioPlaying() {
        int PLAY_WRITE_SIZE = DATA_TIME_CNT;
        while(isAudioKeepPlaying){
            int writeCnt = mAudioTrack.write(dataTable[freqIdxToPlay], 0, PLAY_WRITE_SIZE);
            if(writeCnt != PLAY_WRITE_SIZE){
                Log.e(C.LOG_TAG, "Audio play write size in consistent, writeCnt = "+writeCnt);
            }
        }
    }

    // end of survey
    void actionEnd(){
        if(uc!=null) {
            uc.stopEverything();
            uc = null; // release the previous used controller
        }
        finish();
    }

    void updateFreqIdx(final int freqIdx){
        freqIdxToPlay = freqIdx;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtDebugFreqIdx.setText("FreqIdx = "+freqIdxToPlay);
                updateGaugeValue(freqIdx);
            }
        });
    }

    void updateGaugeValue(int freqIdx){
        gaugeView.setTargetValue(freqIdx);
    }

//==================================================================================================
// Ultraphone callbacks
//==================================================================================================
    @Override
    public void pressureUpdate(double pressure) {
        double PRESSURE_SCALE_MAX = D.appPressureEngineSoundScaleMax;
        if(pressure >= PRESSURE_SCALE_MAX){
            updateFreqIdx(DATA_FREQ_CNT-1);
        } else {
            updateFreqIdx((int)Math.floor(DATA_FREQ_CNT*pressure/PRESSURE_SCALE_MAX));
        }
    }

    @Override
    public void squeezeUpdate(double check) {

    }

    @Override
    public void updateDebugStatus(final String stringToShow) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtDebugStatus.setText(stringToShow);
            }
        });
    }

    @Override
    public void showToast(final String stringToShow) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), stringToShow, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void unexpectedEnd(int code, String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                actionEnd();
            }
        });
    }
}
