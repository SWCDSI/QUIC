package edu.umich.cse.audioanalysis;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by eddyxd on 11/11/15.
 * 2015/11/11: JNI controller to connect jni functions
 * 2016/04/25: add device-specific initialization
 */
public class JniController {
    static {
        System.loadLibrary("detection"); // load jni libarary
    }
    public native void deviceSettingGeneralInit(int detectChIdxIn);
    public native void detectionInit(String logFolderPath);
    public native void detectionClean();
    public native void detectionCheckStatus();
    public native int addAudioSamples(byte[] audioToAdd);

    // UltraPhone JNI functions
    public native void enablePseReply(); // pressure sensing enable
    public native void enableSseReply(); // squeeze sensing enable
    public native void disableReply();
    public native boolean isReplyReadyToFetch();
    public native float fetchReply();

    // NDK audio play/record related function
    public native void testAssert();
    public native void debugTest();
    public native void debugTestInNdkAudio();

    public native void ndkAudioInit(String logPath);
    public native void createNdkAudioEngine();
    public native void createNdkAudioPlayer(); // remove the argument -> you need to tune it in c directly
    //public native void ndkAudioPlayerStartPlay();
    //public native void ndkAudioPlayerStopPlay();
    public native void createNdkAudioRecorder();
    //public native void ndkAudioRecrderStartRecord();
    public native void ndkAudioForcePhoneStartSensing();
    public native void ndkAudioForcePhoneStopSensing();



    public native int getTotalPlayCallbackCalledCnt();
    public native int getTotalRecordCallbackCalledCnt();

    public JniController(String logFolderPath){
        // reconfig the setting based on device-specific setting
        // NOTE: everything in the setting starts by 1 (matlab format) -> need to -1 to fit c++ format

        if(D.detectMode == D.DETECT_PSE) {
            deviceSettingGeneralInit(D.PSE_DETECT_CH_IDX-1);
        } else if(D.detectMode == D.DETECT_SSE) {
            deviceSettingGeneralInit(D.SSE_DETECT_CH_IDX-1);
        } else {
            Log.e(C.LOG_TAG, "Undefined detect mode in D = "+D.detectMode);
        }

        detectionInit(logFolderPath);
        detectionCheckStatus();

        /*
        int byteCnt = 4;
        ByteBuffer b = ByteBuffer.allocate(byteCnt).order(ByteOrder.LITTLE_ENDIAN); // use linux little endian format
        for (int i=0;i<byteCnt/2;i++){
            b.putShort((short)i);
        }
        byte[] byteToAdd = b.array();
        addAudioSamples(byteToAdd);
        addAudioSamples(byteToAdd);
        */

    }



}
