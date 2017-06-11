package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.DelayAnalyzer;
import edu.umich.cse.audioanalysis.JniController;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.RealtimeSurvey;
import edu.umich.cse.audioanalysis.SurveyEndListener;

public class ExpRealtimeRecordActivity extends AppCompatActivity implements SurveyEndListener {
    WebView webView;
    Button btnStartTest;

    RealtimeSurvey realtimeSurvey;
    JniController jniController;
    DelayAnalyzer ndkDelayAnalyzer;

    boolean isTesting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exp_realtime_record);

        isTesting = false;

        boolean hasLowLatencyFeature =
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);
        boolean hasProFeature =
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO);
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        String framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        String PROPERTY_OUTPUT_FRAMES_PER_BUFFER = am.getProperty(AudioManager.PROPERTY_SUPPORT_MIC_NEAR_ULTRASOUND);
        String PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND = am.getProperty(AudioManager.PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND);
        int framesPerBufferInt = Integer.parseInt(framesPerBuffer); // Convert to int
        //if (framesPerBufferInt == 0) framesPerBufferInt = 256; // Use default


        webView = (WebView) findViewById(R.id.webView);
        webView.loadData("hasLowLatencyFeature = "+hasLowLatencyFeature+"<br>hasProFeature = "+hasProFeature+"<br>framesPerBuffer = "+framesPerBuffer+"<br>PROPERTY_OUTPUT_FRAMES_PER_BUFFER = "+PROPERTY_OUTPUT_FRAMES_PER_BUFFER+"<br>PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND = "+PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND, "text/html", "utf-8");

        btnStartTest = (Button) findViewById(R.id.btnStartTest);

        btnStartTest.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    Log.d(C.LOG_TAG, "button down");
                    startTest();
                    return true;
                }


                if(event.getAction() == MotionEvent.ACTION_UP){
                    Log.d(C.LOG_TAG, "button up");

                    stopTest();
                    // call stopTest a bit latter after the screen is touched
                    /*
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(150);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    stopTest();
                                }
                            });
                        }
                    }).start();
                    */


                    return true;
                }

                return false;
            }
        });

        ndkDelayAnalyzer = new DelayAnalyzer(1000);
        ndkDelayAnalyzer.reset();


        jniController = new JniController(C.appFolderPath);


        jniController.ndkAudioInit(C.appFolderPath);
        jniController.createNdkAudioEngine();
        jniController.createNdkAudioPlayer();
        jniController.createNdkAudioRecorder();



        realtimeSurvey = new RealtimeSurvey(48000, "48000rate-30repeat-24000period+chirp-18000Hz-24000Hz-1200samples+customhamming+pilotchirp.wav");
    }


    void startTest(){
        if(!isTesting){
            isTesting = true;
            //makeOnetimeNoticeIfNotNoticedBefore();

            ndkDelayAnalyzer.addTag("ndkAudioPlayerStartPlay: start");
            jniController.ndkAudioForcePhoneStartSensing();
            ndkDelayAnalyzer.addTag("ndkAudioPlayerStartPlay: end");

            // ndk play thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // do ndk test


                    // *** This part is only used for debugging ***
                    int prevPalyCnt = -1, prevRecordCnt = -1;
                    ndkDelayAnalyzer.addTag("Start check ndk debug result");
                    int playCntToShow = 10; // how many log will be recorded
                    int recordCntToShow = 10;

                    while(playCntToShow>0 || recordCntToShow>0){
                        // check play status
                        int playCnt = jniController.getTotalPlayCallbackCalledCnt();
                        if(playCntToShow>=0 && playCnt != prevPalyCnt){
                            //makeOnetimeNoticeIfNotNoticedBefore();
                            ndkDelayAnalyzer.addTag("playCnt changed to "+playCnt);
                            prevPalyCnt = playCnt;
                            playCntToShow--;
                        }

                        int recordCnt = jniController.getTotalRecordCallbackCalledCnt();
                        if(recordCntToShow>=0 && recordCnt != prevRecordCnt){
                            ndkDelayAnalyzer.addTag("recordCnt changed to "+recordCnt);
                            prevRecordCnt = recordCnt;
                            recordCntToShow--;
                        }
                    }



                    // end of polling -> ask to stop test
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ExpRealtimeRecordActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.loadData(ndkDelayAnalyzer.getResult(true), "text/html", "utf-8");
                            //stopTest();
                        }
                    });
                }
            }).start();



            /*
            boolean initSuccess = realtimeSurvey.initSurvey(this, false);
            if(initSuccess){
                realtimeSurvey.startSurvey();
            }
            */

        } else {
            Toast.makeText(this, "Test is started -> unable to start twice",Toast.LENGTH_LONG).show();
        }
    }

    void stopTest(){
        if(isTesting){
            isTesting = false;
            jniController.ndkAudioForcePhoneStopSensing();
            webView.loadData(ndkDelayAnalyzer.getResult(true), "text/html", "utf-8");

            //jniController.ndkAudioPlayerStopPlay();

            //btnStartTest.setEnabled(true);

            // show test result (java)
            //realtimeSurvey.stopSurvey();
            //webView.loadData(realtimeSurvey.delayAnalyzer.getResult(true), "text/html", "utf-8");

            // show test result (ndk)
            //webView.loadData(ndkDelayAnalyzer.getResult(true), "text/html", "utf-8");


            cleanNotice();
        } else {
            Toast.makeText(this, "Test is not started -> unable to stop",Toast.LENGTH_LONG).show();
        }
    }

    // This function produce a notification to users that something happends
    boolean isNoticed = false;
    void makeOnetimeNoticeIfNotNoticedBefore(){
        if(!isNoticed){
            isNoticed = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getWindow().getDecorView().setBackgroundColor(Color.YELLOW);
                }
            });
        }
    }

    void cleanNotice(){
        isNoticed = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getWindow().getDecorView().setBackgroundColor(Color.WHITE);
            }
        });
    }

    // callbacks
    @Override
    public void onSurveyEnd() {

    }

    @Override
    public void audioRecorded(byte[] data, long audioTotalRecordedSampleCnt) {
        //makeOnetimeNoticeIfNotNoticedBefore();
    }
}
