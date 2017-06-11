package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import edu.umich.cse.audioanalysis.BigMoveDetector;
import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.JniController;
import edu.umich.cse.audioanalysis.LogController;
import edu.umich.cse.audioanalysis.MyApp;
import edu.umich.cse.audioanalysis.MySensorController;
import edu.umich.cse.audioanalysis.MySensorControllerListener;
import edu.umich.cse.audioanalysis.Network.NetworkController;
import edu.umich.cse.audioanalysis.Network.NetworkControllerListener;
import edu.umich.cse.audioanalysis.Network.RemoteTriggerControllerListener;
import edu.umich.cse.audioanalysis.SpectrumSurvey;
import edu.umich.cse.audioanalysis.SurveyEndListener;
import edu.umich.cse.audioanalysis.Ultraphone.UltraphoneControllerListener;


/**
 * Created by eddyxd on 11/19/15.
 * yctung: a delegate to controll ultraphone sensing
 * NOTE: everything in this controller is configured based on device-specific setting "D"
 * Need to update D before init this class!!
 */
public class UltraphoneController implements NetworkControllerListener, SurveyEndListener, MySensorControllerListener {
    public static final int ERROR_CODE_PILOT_NOT_FOUND = 1;
    public static final int ERROR_CODE_AUDIO_END = 2;

    // Android UI variables
    Context context;
    UltraphoneControllerListener caller;

    int deviceIdx = -1;

    // internal state
    boolean isSurvying;
    boolean isConnecting;
    boolean needToStartSensingAfterNetworkConnected;
    boolean needToRecordForce, needToRecordSqueeze, needToStartCheckSqueezeAfterNetworkConnected;
    boolean needToMoveTraceFolderWhenSensingEnded;
    boolean pilotNotSyncedHasBeenFound; // use this flag to avoid triggering the alert for pilot not synced multiple times
    String traceFolderSuffixToMove;

    // internal sensing controllers
    int RECORDER_SAMPLERATE = 48000;
    float volSelected = C.DEFAULT_VOL;
    String soundNameSelected = "default";
    String soundSettingSelected = "48000rate-5000repeat-2400period+chirp-18000Hz-24000Hz-1200samples+namereduced"; // 4min, 20Hz version
    SpectrumSurvey ss;
    MySensorController msc;
    LogController lc;
    NetworkController nc;
    JniController jc;
    public BigMoveDetector bmd;

    // performance measurement variables
    double anaDelaySum = 0;
    double anaDelayCnt = 0;

    final String SERVER_IP = C.SERVER_ADDR;
            //"10.0.0.12"; // Umich5566 in my office
    //String SERVER_IP = "10.0.0.12"; // Other

    final int SERVER_PORT = C.DETECTER_SERVER_PORT;

    Queue<Double> estimatedPressures;
    double estimatedPressureSum;

    int bigMoveStatus;

    public UltraphoneController(int detectMode, UltraphoneControllerListener callerIn, Context contextIn){
        context = contextIn;
        caller = callerIn;

        // init device config
        // update detectMode in D based on input
        D.configBasedOnSetting(detectMode, context);

        deviceIdx = D.code;


        // init necessary variables
        isConnecting = false;
        isSurvying = false;
        needToStartSensingAfterNetworkConnected = false;
        needToRecordForce = false;
        needToRecordSqueeze = false;
        needToStartCheckSqueezeAfterNetworkConnected = false;

        needToMoveTraceFolderWhenSensingEnded = false;
        pilotNotSyncedHasBeenFound = false;

        estimatedPressureSum = 0;
        estimatedPressures = new LinkedList<>();

        nc = new NetworkController(this);

        bigMoveStatus = 0;
        bmd = new BigMoveDetector();

        // connect sensor controller
        // NOTE: the folder to log will be updated latter when sensing starts
        msc = new MySensorController(this, (SensorManager) context.getSystemService(Context.SENSOR_SERVICE), C.DEBUG_FOLDER, 1000);

        // connect sensor controller
        //msc = new MySensorController(this, (SensorManager) context.getSystemService(Context.SENSOR_SERVICE), C.DEBUG_FOLDER, 1000);

        // create dummy log controller -> NOTE this logcontroller can't save trace since there is no folder is created yet
        lc = new LogController(C.appFolderPath+C.DEBUG_FOLDER, nc);
    }

//===================================================================================
// External functions
//===================================================================================
    public void startEverything(){
        if(C.TRACE_SEND_TO_NETWORK) {
            startNetwork();
            needToStartSensingAfterNetworkConnected = true;
        } else {
            startSensing();
        }
    }
    public void stopEverything(){
        stopSensing();
        stopNetwork();
    }
    public void startSensing(){
        ss = new SpectrumSurvey(RECORDER_SAMPLERATE, soundSettingSelected, context);
        boolean initSuccess = ss.initSurvey(this, C.SURVEY_MODE_TRAIN); // reuse the train as chirps mode
        if(initSuccess==false){
            caller.showToast("Please wait the previos sensing ends");
        } else {
            if (C.TRACE_REALTIME_PROCESSING) {
                jc = new JniController(C.appFolderPath + C.DEBUG_FOLDER + C.JNI_LOG_FOLER);
            }
            
            lc = new LogController(C.appFolderPath+C.DEBUG_FOLDER, nc); // NOTE the folder should be consistent to what data is saved to


            msc.startRecord(C.appFolderPath+C.DEBUG_FOLDER);


            isSurvying = true;
            caller.updateDebugStatus("Wait survey ends");
            ss.startSurvey();

            // start sending check code if need
            if (needToStartCheckSqueezeAfterNetworkConnected) {
                startCheckSqueezeRightNow();
                needToStartCheckSqueezeAfterNetworkConnected = false;
            }

        }
    }

    public void stopSensing(){
        if(isSurvying) {
            ss.stopSurvey();
        }
    }

    public void startNetwork(){
        caller.updateDebugStatus("Wait connection to server...");
        nc.connectServer(SERVER_IP, SERVER_PORT);
    }
    public void stopNetwork(){
        if (isConnecting) {
            nc.closeServerIfServerIsAlive();
            isConnecting = false;
        }
    }
    public void startCheckPressure(Point p){
        needToRecordForce = true;

        resetSmoothedPressure(); // remove all the old records

        lc.addLogAndOutputDirectly(ss.audioTotalRecordedSampleCnt, LogController.TAG_PRESSURE_SENSITIVE_ENABLE, 1, p.x, p.y);

        // enable realtime processing at phone when server is not connected
        if(!isConnecting && C.TRACE_REALTIME_PROCESSING && jc!=null){
            jc.enablePseReply();
        }

        caller.pressureUpdate(0);
    }

    public void stopCheckPressure(){
        needToRecordForce = false;

        lc.addLogAndOutputDirectly(ss.audioTotalRecordedSampleCnt, LogController.TAG_PRESSURE_SENSITIVE_ENABLE, 2, 0, 0);

        // disable realtime processing at phone when server is not connected
        if(!isConnecting && C.TRACE_REALTIME_PROCESSING && jc!=null){
            jc.disableReply();
        }

        caller.pressureUpdate(0);
    }

    public void startCheckSqueezeWhenPossible(){
        needToStartCheckSqueezeAfterNetworkConnected = true;
    }

    public void startCheckSqueezeRightNow(){
        if (needToRecordSqueeze) {
            Log.d(C.LOG_TAG,"[ERROR]: somehting wrong, actionSqueezeTestStart is triggered when needToRecordSqueeze = YES (remote server?)");
            caller.updateDebugStatus("Squeeze is ERROR");
        } else {
            needToRecordSqueeze = true;
            // write log to log file
            lc.addLogAndOutputDirectly(ss.audioTotalRecordedSampleCnt, LogController.TAG_SQUEEZE_SENSITIVE_ENALBE, 1, 0, 0);

            // enable realtime processing at phone when server is not connected
            if(!isConnecting && C.TRACE_REALTIME_PROCESSING && jc!=null){
                jc.enableSseReply();
            }

            caller.updateDebugStatus("Squeeze is ON");
            Log.d(C.LOG_TAG,"Squeeze is ON");
        }
    }

    public void stopCheckSqueeze(){
        if (!needToRecordSqueeze) {
            Log.d(C.LOG_TAG, "[ERROR]: somehting wrong, actionSqueezeTestEnd is triggered when needToRecordSqueeze = NO (remote server?)");
            caller.updateDebugStatus("Squeeze is ERROR");
        } else {
            needToRecordSqueeze = false;
            // write log to log file
            lc.addLogAndOutputDirectly(ss.audioTotalRecordedSampleCnt, LogController.TAG_SQUEEZE_SENSITIVE_ENALBE, 2, 0, 0);

            // disable realtime processing at phone when server is not connected
            if(!isConnecting && C.TRACE_REALTIME_PROCESSING && jc!=null){
                jc.disableReply();
            }

            caller.updateDebugStatus("Squeeze is OFF");
            Log.d(C.LOG_TAG, "Squeeze is OFF");
        }
    }

    public void setTriggerLog(int code, float arg0, float arg1){
        int stamp = 0;
        if (isSurvying){
            stamp = ss.audioTotalRecordedSampleCnt;
        }
        lc.addLogAndOutputDirectly(stamp,LogController.TAG_SET_TRIGGER,code,arg0,arg1);
    }

    public void moveTraceFolderWhenSensingEnded(String suffix){
        needToMoveTraceFolderWhenSensingEnded = true;
        traceFolderSuffixToMove = suffix;
    }


    // this function must be called once before using "getSmoothedPressure"
    void resetSmoothedPressure(){
        estimatedPressures.clear();
        estimatedPressureSum = 0;
    }

    double getSmoothedPressure(double dataNow){
        Double fDouble = new Double(dataNow);
        estimatedPressures.add(fDouble);
        estimatedPressureSum += fDouble.doubleValue();

        if (estimatedPressures.size() > C.UI_PRESSURE_SMOOTH_DATA_CNT) {
            Double d = estimatedPressures.peek(); // number to pop
            estimatedPressureSum -= d.doubleValue();
            estimatedPressures.poll(); // pop the oldest data
        }
        double smoothedPressure = estimatedPressureSum/(C.UI_PRESSURE_SMOOTH_DATA_CNT);
        return smoothedPressure;
    }


//===================================================================================
// Networking callback functions
//===================================================================================
    public void isConnected(boolean success, final String resp) {
        if(success){
            // send init set actions
            nc.sendSetAction(NetworkController.SET_TYPE_STRING, "matlabSourceMatName", ("source_"+soundSettingSelected).getBytes());
            nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, "traceChannelCnt", "2".getBytes());
            nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, "traceVol", "0.5".getBytes());
            nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, "deviceIdx", String.format("%d", deviceIdx).getBytes());
            nc.sendInitAction();
            //nc.sendSetAction(NetworkController.SET_TYPE_INT, "traceChannelCnt", ByteBuffer.allocate(4).putInt(2).array());
            //nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING,"vol","0.5".getBytes());

            // update UI
            isConnecting = true;
            caller.updateDebugStatus("Connect successfully");

            if (needToStartSensingAfterNetworkConnected){
                Activity a = (Activity)caller;
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startSensing();
                    }
                });
            }

        } else {
            caller.showToast("[ERROR]: unable to connect server : " + resp);
        }
    }

    @Override
    public int consumeReceivedData(double dataReceived) {
        double smoothedData = getSmoothedPressure(dataReceived);

        caller.updateDebugStatus(String.format("received data= %.3f, smoothed data = %.3f", dataReceived, smoothedData));

        if (needToRecordForce) {
            caller.pressureUpdate(smoothedData);
        } else if (needToRecordSqueeze) {
            int check = (int)Math.round(dataReceived); // convert it to integer by rounding

            //[labelStatus setText:[NSString stringWithFormat:@"(%d) data = %f (%d)", bigMoveStatus, f, check]];
            Log.d(C.LOG_TAG, String.format("data = %f (%d)", dataReceived, check));
            caller.updateDebugStatus(String.format("data = %f (%d)", dataReceived, check));
            caller.squeezeUpdate(check);
        }

        return 0;
    }



//===================================================================================
// Survey end callback
//===================================================================================
    public void onSurveyEnd() {
        Log.d(C.LOG_TAG, "onSurveyEnd");
        isSurvying = false;

        msc.stopRecord();

        // move the folder to target folder if necessary
        if(needToMoveTraceFolderWhenSensingEnded){
            File folderOld = new File(C.appFolderPath + C.DEBUG_FOLDER); // TODO: load from survey class
            File folderNew = new File(C.appFolderPath + C.DEBUG_FOLDER.substring(0,C.DEBUG_FOLDER.lastIndexOf("/")) + traceFolderSuffixToMove);
            if(folderOld.exists()){
                if (folderNew.exists()){
                    Log.w(C.LOG_TAG, "[WARN]: new folder path is already existed (forget to clean?)");
                }
                folderOld.renameTo(folderNew);
            } else {
                Log.e(C.LOG_TAG, "[ERROR]: no sensing trace folder to move");
            }

            needToMoveTraceFolderWhenSensingEnded = false;
        }
        // neet to realease this resource whenever is possible
        ss = null;

        //caller.showToast("[ERROR]: audio play end (might need to use a longer audio?)");
        caller.unexpectedEnd(ERROR_CODE_AUDIO_END, "[ERROR]: audio play end (might need to use a longer audio?)");
    }

    //public void audioRecorded(ByteArrayOutputStream data) {
    public long currentAudioTotalRecordedSampleCnt = 0;
    public void audioRecorded(byte[] data, long audioTotalRecordedSampleCnt) {
        currentAudioTotalRecordedSampleCnt = audioTotalRecordedSampleCnt;
        msc.outputMotion((int)audioTotalRecordedSampleCnt);
        bigMoveStatus = bmd.update(ss.audioTotalRecordedSampleCnt, msc.accMag, msc.gyroMag);

        if(isConnecting){
            //nc.sendDataRequest(data.toByteArray());
            nc.sendDataRequest(data);
        } else if(C.TRACE_REALTIME_PROCESSING){ // data is processed by JNI only when networking is not connected

            long t1=0, t2=0;
            if(C.ANA_NEED_TO_ESTIMATE_JNI_DELAY){
                t1 = System.currentTimeMillis();
            }
            int result = jc.addAudioSamples(data);
            if(C.ANA_NEED_TO_ESTIMATE_JNI_DELAY){
                t2 = System.currentTimeMillis();
            }

            if(result==-1){
                Log.e(C.LOG_TAG, "[ERROR]: unable to find pilot (wrong pilot setting?)");
                if(!pilotNotSyncedHasBeenFound) {
                    pilotNotSyncedHasBeenFound = true; // avoid trigger it multiple times
                    caller.showToast("[ERROR] pilot is not synced");
                    caller.unexpectedEnd(ERROR_CODE_PILOT_NOT_FOUND, "[ERROR] pilot is not synced");
                }
            }

            if(C.ANA_NEED_TO_ESTIMATE_JNI_DELAY && jc.isReplyReadyToFetch() && result>10){ // ignore first few samples for getting delay estimation
                double delayNow = (double)(t2-t1)/1000.0;
                anaDelaySum += delayNow;
                anaDelayCnt += 1;
                caller.updateDebugStatus(String.format("delayNow = %f, avg = %f", delayNow, anaDelaySum / anaDelayCnt));
            }

            while(jc.isReplyReadyToFetch()) {
                float reply = jc.fetchReply();

                // put reply for pressure sensing
                if(needToRecordForce) {
                    double smoothedReply = getSmoothedPressure(reply);
                    if (!C.ANA_NEED_TO_ESTIMATE_JNI_DELAY) { // avoid overwrite the delay information
                        caller.updateDebugStatus(String.format("JNI reply = %.3f, smoothed reply = %.3f", reply, smoothedReply));
                    }
                    caller.pressureUpdate(smoothedReply);
                }

                // put reply for squeese sensing
                if(needToRecordSqueeze) {
                    /*
                    int squeezeStatus = Math.round(reply);
                    if (!C.ANA_NEED_TO_ESTIMATE_JNI_DELAY) { // avoid overwrite the delay information
                        caller.updateDebugStatus(String.format("JNI reply = %d", squeezeStatus));
                    }*/
                    // yctung: skip the detection, just report the energy of windowed area
                    caller.squeezeUpdate((double) reply);
                }
            }
        }
    }


//===================================================================================
// Other callback
//===================================================================================
    public void onTiltChanged(double tiltX, double tiltY, double tiltZ) {
        /*
        if(nc!=null && isConnecting) {
            nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, "sensorDataNow", ("[" + msc.getDataString() + "]").getBytes());
        }
        */
    }

    public void onRecordedEnd() {

    }

    /*
    String getTimeString(){
        Calendar c = Calendar.getInstance();
        int mseconds = c.get(Calendar.MILLISECOND);
        String currentDateandTime = mDateFormat.format(new Date()) + String.format("-%04d", mseconds);

        return currentDateandTime;
    }*/

}
