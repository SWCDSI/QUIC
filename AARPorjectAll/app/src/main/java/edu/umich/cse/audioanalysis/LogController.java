package edu.umich.cse.audioanalysis;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import edu.umich.cse.audioanalysis.Network.NetworkController;

/**
 * Created by eddyxd on 10/27/15.
 * 2015/10/27: used to control log output -> dump to files or dump to network
 */
public class LogController {
    // *** start of category of log options ***
    static public final String TAG_METER_FROM_OBJECT = "mfo";
    static public final String TAG_PRESSURE_SENSITIVE_ENABLE = "pse";
    static public final String TAG_SQUEEZE_SENSITIVE_ENALBE = "sse";
    static public final String TAG_OBJECT_DETECTION_EANBLE = "ode";
    static public final String TAG_SET_TRIGGER = "trg";
    static public final String TAG_NOISE_TEST = "nos";

    // *** end of category of log options ***

    //ArrayList<LogReqeust> savedRequests;

    //static final String TRACE_FILE_NAME = "truthlog.txt";

    //String logPath;
    //File logFile;
    //PrintWriter logFileWriter;
    //FileOutputStream motionOutputStream;

    NetworkController logNetworkController;

    String traceFolderPath;
    public LogController(String traceFolderPathIn, NetworkController networkController){
        traceFolderPath = traceFolderPathIn;
        File pathFolder = new File(traceFolderPath);
        if(!pathFolder.exists()){
            Log.e(C.LOG_TAG, "[ERROR]: log tracePath("+traceFolderPath+") is not existed (not created yet?)");
            //android.os.Process.killProcess(android.os.Process.myPid());
        }

        logNetworkController = networkController;


        /*
        logPath = traceFolderPath+TRACE_FILE_NAME;
        try {
            logFile = new File(logPath);
            if(logFile.exists()){
                Log.e(C.LOG_TAG, "[ERROR]: log file existed (forget to remove old trace folder?)");
            } else {
                logFile.createNewFile();
            }
            motionOutputStream = new FileOutputStream(new File(logPath), true);
            logFileWriter = new PrintWriter(motionOutputStream);
        } catch (FileNotFoundException e) {
            Log.e(C.LOG_TAG, "[ERROR]: unable to open files");
            //android.os.Process.killProcess(android.os.Process.myPid());
        } catch (IOException e){
            Log.e(C.LOG_TAG, "[ERROR]: unable to create a new file");
            //android.os.Process.killProcess(android.os.Process.myPid());
        }

        savedRequests = new ArrayList<LogReqeust>();
        */
    }

    // NOTE: in 48000Hz sample rate -> it can only keep about 10hr data
    public void addLogAndOutputDirectly(int stamp, String tag, int code, float arg0, float arg1){
        Log.d(C.LOG_TAG, String.format("LogController: add log as (stamp, tag, code) = (%d, %s, code)", stamp, tag, code));
        String pathToSave = traceFolderPath+tag+".dat";
        // data to bytes
        // ref: http://stackoverflow.com/questions/14619653/converting-a-float-to-a-byte-array-and-vice-versa-in-java
        int byteCnt = 4*4; // 4 varaibleas each 4 bytes
        ByteBuffer b = ByteBuffer.allocate(byteCnt).order(ByteOrder.LITTLE_ENDIAN); // use linux little endian format
        b.putInt(stamp);
        b.putInt(code);
        b.putFloat(arg0);
        b.putFloat(arg1);
        // ByteBuffer to byte[]
        // ref: http://stackoverflow.com/questions/679298/gets-byte-array-from-a-bytebuffer-in-java
        //byte[] byteToSave = new byte[byteCnt];
        //b.get(byteToSave);
        byte[] byteToSave = b.array();
        // NOTE: tag is not using now
        if(C.TRACE_SAVE_TO_FILE){
            try {
                File logFile = new File(pathToSave);
                if(!logFile.exists()){
                    logFile.createNewFile();
                }
                FileOutputStream outputStream = new FileOutputStream(new File(pathToSave), true);
                outputStream.write(byteToSave, 0, byteCnt);

            } catch (FileNotFoundException e) {
                Log.e(C.LOG_TAG, "[ERROR]: unable to open files");
                //android.os.Process.killProcess(android.os.Process.myPid());
            } catch (IOException e){
                Log.e(C.LOG_TAG, "[ERROR]: unable to create a new file");
                //android.os.Process.killProcess(android.os.Process.myPid());
            }
        }

        if(C.TRACE_SEND_TO_NETWORK && logNetworkController != null && logNetworkController.isConnected()){
            logNetworkController.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, tag, String.format("%d", code).getBytes());
        }
    }


    /*
    private void save(long stamp, String tag, int code){
        savedRequests.add(new LogReqeust(stamp, tag, code));
        String stringToSave = String.format("%d %s %d\n", stamp, tag, code);
        if(C.TRACE_SAVE_TO_FILE && logFileWriter!=null){
            logFileWriter.write(stringToSave);
            logFileWriter.flush();
        }
        if(logNetworkController!=null && logNetworkController.isConnected()){
            logNetworkController.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, tag, String.format("%d; stamp = %d;", code, stamp).getBytes());
        }
    }

    // logic to help prevent making wrong log
    public boolean noThisTagSavedBefore(String tag){
        for(int i = 0; i< savedRequests.size();i++){
            if(tag.equals(savedRequests.get(i).tag)){
                return false;
            }
        }
        return true;
    }

    // class used to remember saved logs
    class LogReqeust{
        long stamp;
        String tag;
        int code;
        public LogReqeust(long stamp, String tag, int code){
            this.stamp = stamp;
            this.tag = tag;
            this.code = code;
        }
    }

    */

}
