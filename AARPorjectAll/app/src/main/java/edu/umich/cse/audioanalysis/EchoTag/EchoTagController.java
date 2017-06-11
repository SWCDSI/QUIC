package edu.umich.cse.audioanalysis.EchoTag;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.File;
import java.util.LinkedList;

import edu.umich.cse.audioanalysis.BigMoveDetector;
import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.JniController;
import edu.umich.cse.audioanalysis.LogController;
import edu.umich.cse.audioanalysis.MySensorController;
import edu.umich.cse.audioanalysis.MySensorControllerListener;
import edu.umich.cse.audioanalysis.Network.NetworkController;
import edu.umich.cse.audioanalysis.Network.NetworkControllerListener;
import edu.umich.cse.audioanalysis.SpectrumSurvey;
import edu.umich.cse.audioanalysis.SurveyEndListener;
import edu.umich.cse.audioanalysis.Ultraphone.UltraphoneControllerListener;

/**
 * Created by eddyxd on 8/1/16.
 * yctung: this is the new controller of echotag -> used to sensing environment and remember tags
 * Most of functions are copied from Ultraphone controller
 */
public class EchoTagController implements NetworkControllerListener, SurveyEndListener, MySensorControllerListener {
    public static final int ERROR_CODE_PILOT_NOT_FOUND = 1;
    public static final int ERROR_CODE_AUDIO_END = 2;

    // android UI variables
    Context context;
    EchoTagControllerListener caller;

    // internal sensing controllers
    SpectrumSurvey ss;
    MySensorController msc;
    LogController lc;
    NetworkController nc;
    JniController jc;

    // internal state
    boolean isSurvying;
    boolean isConnecting;
    boolean needToStartSensingAfterNetworkConnected;
    boolean needToMoveTraceFolderWhenSensingEnded;
    boolean pilotNotSyncedHasBeenFound; // use this flag to avoid triggering the alert for pilot not synced multiple times
    String traceFolderSuffixToMove;

    int RECORDER_SAMPLERATE = 48000;
    float volSelected = C.DEFAULT_VOL;
    String soundNameSelected = "default";
    String soundSettingSelected = "48000rate-5000repeat-2400period+chirp-18000Hz-24000Hz-1200samples+namereduced"; // 4min, 20Hz version
    final String SERVER_IP = C.SERVER_ADDR;
    final int SERVER_PORT = C.DETECTER_SERVER_PORT;
    int deviceIdx = -1;

    public EchoTagController(int detectMode, EchoTagControllerListener callerIn, Context contextIn){
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
        needToMoveTraceFolderWhenSensingEnded = false;
        pilotNotSyncedHasBeenFound = false;

        nc = new NetworkController(this);

        // connect sensor controller
        // NOTE: the folder to log will be updated latter when sensing starts
        msc = new MySensorController(this, (SensorManager) context.getSystemService(Context.SENSOR_SERVICE), C.DEBUG_FOLDER, 1000);

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


//===================================================================================
// Networking callback functions
//===================================================================================
    @Override
    public void isConnected(boolean success, String resp) {
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
        caller.updateDebugStatus(String.format("received data= %.3f", dataReceived));

        // TODO: update this recieved data to caller (like the predict tagged location...etc)
        /*
        if (needToRecordForce) {
            caller.pressureUpdate(smoothedData);
        } else if (needToRecordSqueeze) {
            int check = (int)Math.round(dataReceived); // convert it to integer by rounding

            //[labelStatus setText:[NSString stringWithFormat:@"(%d) data = %f (%d)", bigMoveStatus, f, check]];
            Log.d(C.LOG_TAG, String.format("data = %f (%d)", dataReceived, check));
            caller.updateDebugStatus(String.format("data = %f (%d)", dataReceived, check));
            caller.squeezeUpdate(check);
        }
        */

        return 0;
    }


//===================================================================================
// Survey end callback
//===================================================================================
    @Override
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

    @Override
    public void audioRecorded(byte[] data, long audioTotalRecordedSampleCnt) {
        msc.outputMotion((int)audioTotalRecordedSampleCnt);

        if(isConnecting){
            nc.sendDataRequest(data);
        } else if(C.TRACE_REALTIME_PROCESSING){ // data is processed by JNI only when networking is not connected
            // TODO: make it run on device
        }
    }

//===================================================================================
// Other callback
//===================================================================================
    @Override
    public void onTiltChanged(double tiltX, double tiltY, double tiltZ) {

    }

    @Override
    public void onRecordedEnd() {

    }
}

