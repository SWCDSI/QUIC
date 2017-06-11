package edu.umich.cse.audioanalysis.BumpFree;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.LogController;
import edu.umich.cse.audioanalysis.MySensorController;
import edu.umich.cse.audioanalysis.MySensorControllerListener;
import edu.umich.cse.audioanalysis.Network.NetworkController;
import edu.umich.cse.audioanalysis.Network.NetworkControllerListener;
import edu.umich.cse.audioanalysis.SpectrumSurvey;
import edu.umich.cse.audioanalysis.SurveyEndListener;

/**
 * Created by eddyxd on 11/20/15.
 *  yctung: similar to ultraphone controller -> manage the bumpfree calls
 */


public class BumpfreeController implements NetworkControllerListener, SurveyEndListener, MySensorControllerListener {
    // Android UI variables
    Context context;
    BumpfreeControllerListener caller;

    int deviceIdx = 1; // TODO: name this index based on phone model if need, now 1 means android

    // internal state
    boolean isSurvying;
    boolean isConnecting;
    boolean needToStartSensingAfterNetworkConnected;
    boolean needToDetect;

    // internal sensing controllers
    int RECORDER_SAMPLERATE = 48000;
    float volSelected = C.DEFAULT_VOL;
    String soundNameSelected = "default";
    String soundSettingSelected = "48000rate-5000repeat-2400period+chirp-18000Hz-24000Hz-1200samples+namereduced"; // 4min, 20Hz version
    SpectrumSurvey ss;
    MySensorController msc;
    LogController lc;

    NetworkController nc;

    //final String SERVER_IP = "10.0.0.12"; // Umich5566 in my office
    //String SERVER_IP = "192.168.1.143"; // Home
    //String SERVER_IP = "35.2.78.141"; // Mwireless
    //final int SERVER_PORT = 50009;

    Queue<Double> estimatedPressures;
    double estimatedPressureSum;

    public BumpfreeController(BumpfreeControllerListener callerIn, Context contextIn) {
        context = contextIn;
        caller = callerIn;

        // init necessary variables
        isConnecting = false;
        isSurvying = false;
        needToStartSensingAfterNetworkConnected = false;
        needToDetect = false;

        estimatedPressureSum = 0;
        estimatedPressures = new LinkedList<>();

        nc = new NetworkController(this);

        // connect sensor controller
        msc = new MySensorController(this, (SensorManager) context.getSystemService(Context.SENSOR_SERVICE), C.DEBUG_FOLDER, 1000);

        // create dummy log controller -> NOTE this logcontroller can't save trace since there is no folder is created yet
        lc = new LogController(C.appFolderPath + C.DEBUG_FOLDER, nc);
    }

//===================================================================================
// External functions
//===================================================================================
    public void startEverything() {
        if(C.TRACE_SEND_TO_NETWORK) {
            startNetwork();
            needToStartSensingAfterNetworkConnected = true;
        } else {
            startSensing();
        }
    }

    public void stopEverything() {
        stopSensing();
        stopNetwork();
    }

    public void startSensing() {
        ss = new SpectrumSurvey(RECORDER_SAMPLERATE, soundSettingSelected, context);
        boolean initSuccess = ss.initSurvey(this, C.SURVEY_MODE_TRAIN); // reuse the train as chirps mode
        if (initSuccess == false) {
            caller.showToast("Please wait the previos sensing ends");
        } else {
            isSurvying = true;
            caller.updateDebugStatus("Wait survey ends");
            ss.startSurvey();
        }
    }

    public void stopSensing() {
        ss.stopSurvey();
    }

    public void startNetwork() {
        caller.updateDebugStatus("Wait connection to server...");
        nc.connectServer(C.SERVER_ADDR, C.DETECTER_SERVER_PORT);
    }

    public void stopNetwork() {
        nc.closeServerIfServerIsAlive();
        isConnecting = false;
    }

    public void startDetect() {
        needToDetect = true;
        lc.addLogAndOutputDirectly(ss.audioTotalRecordedSampleCnt, LogController.TAG_OBJECT_DETECTION_EANBLE, 1, 0, 0);

    }

    public void stopDetect() {
        needToDetect = false;
        lc.addLogAndOutputDirectly(ss.audioTotalRecordedSampleCnt, LogController.TAG_OBJECT_DETECTION_EANBLE, 2, 0, 0);
    }

    public void startCheckSqueezeWhenPossible() {

    }

    public void stopCheckSqueeze() {

    }

//===================================================================================
// Networking callback functions
//===================================================================================
    public void isConnected(boolean success, final String resp) {
        if (success) {
            // send init set actions
            nc.sendSetAction(NetworkController.SET_TYPE_STRING, "matlabSourceMatName", ("source_" + soundSettingSelected).getBytes());
            nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, "traceChannelCnt", "2".getBytes());
            nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, "traceVol", "0.5".getBytes());
            nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, "deviceIdx", String.format("%d", deviceIdx).getBytes());
            nc.sendInitAction();
            //nc.sendSetAction(NetworkController.SET_TYPE_INT, "traceChannelCnt", ByteBuffer.allocate(4).putInt(2).array());
            //nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING,"vol","0.5".getBytes());

            // update UI
            isConnecting = true;
            caller.updateDebugStatus("Connect successfully");

            if (needToStartSensingAfterNetworkConnected) {
                Activity a = (Activity) caller;
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
        caller.updateDebugStatus(String.format("received data = %f", dataReceived));

        double SACLE = 2.0; // used for better visulization and easy detection setting

        if (needToDetect) {
            caller.detectionUpdate(dataReceived*SACLE);
            /*
            Double fDouble = new Double(dataReceived);
            estimatedPressures.add(fDouble);
            estimatedPressureSum += fDouble.doubleValue();

            if (estimatedPressures.size() > C.UI_PRESSURE_SMOOTH_DATA_CNT) {
                Double d = estimatedPressures.peek(); // number to pop
                estimatedPressureSum -= d.doubleValue();
                estimatedPressures.poll(); // pop the oldest data
            }

            double smoothedPressure = estimatedPressureSum / ((double) estimatedPressures.size());
            caller.pressureUpdate(smoothedPressure * 5);*/
        }

        return 0;
    }


//===================================================================================
// Survey end callback
//===================================================================================
    public void onSurveyEnd() {
        Log.d(C.LOG_TAG, "onSurveyEnd");
        isSurvying = false;

        // neet to realease this resource whenever is possible
        ss = null;
    }

    //public void audioRecorded(ByteArrayOutputStream data) {
    public void audioRecorded(byte[] data, long audioTotalRecordedSampleCnt) {
        if (isConnecting) {
            //nc.sendDataRequest(data.toByteArray());
            nc.sendDataRequest(data);
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
}