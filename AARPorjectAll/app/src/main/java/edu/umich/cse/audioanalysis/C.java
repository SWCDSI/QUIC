package edu.umich.cse.audioanalysis;

import java.util.Arrays;

/**
 * Created by eddyxd on 9/28/15.
 * basic global variable class
 * )
 */
public class C {
    public final static String LOG_TAG = "AudioAnalysis";
    public static float DEFAULT_VOL = 1.0f;
    public final static String SETTING_JSON_FILE_NAME = "AudioAnaSetting.json";

    public static boolean USE_REAL_TIME_SURVEY = false;
    public static boolean USE_AUDIO_QUEUE_IN_REAL_TIME_SRUVEY = false;

    public static boolean TRACE_SAVE_TO_FILE = false;
    public static boolean TRACE_REALTIME_PROCESSING = true; // NOTE: Can only select one of TRACE_REALTIME_PROCESSING/TRACE_SEND_TO_NETWORK
    public static boolean TRACE_SEND_TO_NETWORK = false;
    public static boolean TRIGGERED_BY_NETWORK = false;
    public static boolean TRIGGERED_BY_LOCAL = true;
    public static boolean DISABLE_SQUEEZE_APPS = true;
    public static boolean SHOW_CALIBRATION_LAYOUT = false; // this shows the "dot" of cliabration locations
    public static boolean FORCE_TO_USE_TOP_SPEAKER = false;
    public static boolean ANA_NEED_TO_ESTIMATE_JNI_DELAY = false;

    public static final int UI_PRESSURE_SMOOTH_DATA_CNT = 5; // use multiple pressure estiamtion to get smooth results

    // Networking setting
    //public static String SERVER_ADDR = "192.168.1.114"; // Home
    //public static String SERVER_ADDR = "10.0.0.12"; // Office
    public static String SERVER_ADDR = "35.2.209.110"; // MWireless
    public static int DETECTER_SERVER_PORT = 50009;
    public static int TRIGGER_SERVER_PORT = 50010;

    public final static String INPUT_FOLDER = "AudioInput/";
    public final static String INPUT_PREFIX = "source_";
    public static final String OUTPUT_FOLDER = "DataOutput/";
    public static final String DEBUG_FOLDER = "DebugOutput/";
    public static final String JNI_LOG_FOLER = "log/";

    // detailed control variables -> most used for debug
    public static boolean TERMINATE_AFTER_EACH_LOCATION_SENSING = false; // used for making many expeirments manually
    public static boolean SURVEY_SEND_DATA_TO_SOCKET = false; // only used for remote python server
    public static boolean SURVEY_DUMP_RAW_BYTE_DATA_TO_FILE = true; // data dumpted for matlab parser

    // contorl of survey mode (predict or train)
    public final static int SURVEY_MODE_TRAIN = 1;
    public final static int SURVEY_MODE_PREDICT = 2;

    public static String systemPath;
    public static String appFolderName;
    public static String appFolderPath;


    // check if the set varaible is ok
    public static String isValidSetting(){
        if(USE_REAL_TIME_SURVEY){
            if(TRACE_SEND_TO_NETWORK) {
                return("Should not enable networking function when USE_REAL_TIME_SURVEY is on");
            }
        }

        if(TRACE_REALTIME_PROCESSING){
            if(TRACE_SEND_TO_NETWORK){
                return("Should not enable both TRACE_REALTIME_PROCESSING and TRACE_SEND_TO_NETWORK");
            }
        }

        return null; // return null means everything is ok
    }
}
