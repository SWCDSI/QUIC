package edu.umich.cse.audioanalysis.Network;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.Queue;

import edu.umich.cse.audioanalysis.C;

/**
 * Created by eddyxd on 12/2/15.
 * 2015/12/02: update to be used for connecting remote trigger
 */
public class RemoteTriggerController {
    public final static char TRIGGER_ACTION_START 	 	= 1;
    public final static char TRIGGER_ACTION_STOP 	 	= 2;
    public final static char TRIGGER_ACTION_SET_SCALE 	= 3;
    public final static char TRIGGER_ACTION_SET_TESTER 	= 4;
    public final static char TRIGGER_ACTION_SET_POSITION = 5;
    public final static char TRIGGER_ACTION_SAVE    	= 6;
    public final static char TRIGGER_ACTION_GIVEUP       = 7;
    public final static char TRIGGER_ACTION_SET_TARGET = 8;
    // here is the check returned by phone
    public final static char TRIGGER_CHECK_OK = 1;
    public final static char TRIGGER_CHECK_NO = 2;
    public final static char TRIGGER_CHECK_TOUCHED = 3; // indicate that user has touch the phone screen
    public final static char TRIGGER_CHECK_STOPSERVER = (char) -2;

    // some trigger parameters
    int scale;
    String tester;
    String position;
    String target;



    private final static int SOCKET_TIMEOUT = 2000; // ms
    private final static int DATA_SENDING_THREAD_LOOP_DELAY = 100; //ms

    // animation scoket classes
    private final static int SERVER_STATUS_DISABLE = -1; // default status of server
    private final static int SERVER_STATUS_ENABLE = 1;
    private final static int SERVER_STATUS_CONNECTED = 2;

    // server actions
    private int status;
    public String serverIp;
    public int serverPort;
    public boolean keepSendingPacket; // control variable for thread to sending data

    private Socket sc = null;
    private DataOutputStream dataOut = null;
    private DataInputStream dataIn = null;

    Queue<NetworkRequest> requestQueue;


    RemoteTriggerControllerListener caller;
    public RemoteTriggerController(RemoteTriggerControllerListener callerIn) {
        caller = callerIn;
        serverIp = C.SERVER_ADDR;
        serverPort = C.TRIGGER_SERVER_PORT;
        status = SERVER_STATUS_DISABLE;
        requestQueue = new LinkedList<NetworkRequest>();


        scale = 2;
        tester = "none";
        position = "none";
        target = "none";

    }

    // netowkring can't be in the main thread -> crash
    public void connectServer(){
        new Thread(){
            public void run() {
                try {
                    InetAddress serverAddr = InetAddress.getByName(serverIp);
                    Log.d(C.LOG_TAG, "Is connecting animation server");
                    // create socket for the address
                    sc = new Socket(serverAddr, serverPort);
                    sc.setSoTimeout(SOCKET_TIMEOUT);
                    // create the output/input stream
                    dataOut = new DataOutputStream(sc.getOutputStream());
                    dataIn = new DataInputStream(sc.getInputStream());

                    createThreadToReceiveAndBlockArrivePacket();

                    status = SERVER_STATUS_CONNECTED;
                    caller.remoteTriggerIsConnected(true, "Connected!");
                } catch (UnknownHostException e) {
                    Log.e(C.LOG_TAG, "ERROR: can't connect socket : " + e.getMessage());
                    e.printStackTrace();
                    caller.remoteTriggerIsConnected(false, "UnknownHostException" + e.getMessage());
                } catch (IOException e) {
                    Log.e(C.LOG_TAG, "ERROR: can't connect socket : " + e.getMessage());
                    e.printStackTrace();
                    caller.remoteTriggerIsConnected(false, "IOException" + e.getMessage());
                }
            }
        }.start();
    }


    public void sendTriggerCheck(char triggerCheck){
        NetworkRequest r = new NetworkRequest(triggerCheck, null, null, -1);
        requestQueue.add(r);
        sendPacketByAnotherThread();
    }
    /*
    public void sendInitAction(){
        NetworkRequest r = new NetworkRequest(ACTION_INIT, null, null, -1);
        requestQueue.add(r);
        sendPacketByAnotherThread();
    }
    */

    boolean keepRecvThreadRunning = false; // control outside the thread to make it work or not
    private void createThreadToReceiveAndBlockArrivePacket (){
        if(!keepRecvThreadRunning){ // thread is not running twice
            keepRecvThreadRunning = true;
            class DataRecvingRunnable implements Runnable {
                public void run() {
                    // *** comment this line just for debugging ***
                    keepPacketRecving();
                }
            }
            new Thread(new DataRecvingRunnable(), "Data Recving Thread").start();
        }
    }

    private void keepPacketRecving(){
        while(keepRecvThreadRunning) {
            try {
                /*
                int BYTE_TO_READ = 8;
                byte[] b = new byte[BYTE_TO_READ];

                int byteIsRead = dataIn.read(b, 0, BYTE_TO_READ);
                ByteBuffer byteBuffer = ByteBuffer.allocate(BYTE_TO_READ).order(ByteOrder.LITTLE_ENDIAN); // use netowrk byte order
                byteBuffer.put(b);
                byteBuffer.position(0);
                //float f = byteBuffer.getFloat();
                double f = byteBuffer.getDouble();

                //double f = dataIn.readFloat();
                //caller.consumeReceivedData(f);
                */

                byte[] buffer = new byte[1024];
                int len = dataIn.read(buffer, 0, 3);
                if (len > 0) {
                    if(len == 3){
                        Log.d(C.LOG_TAG, "socket received data with len = %d"+len);

                        byte action = buffer[0];
                        byte dataByte = buffer[1];
                        byte dataSize = buffer[2];
                        String dataString = null;

                        // read further data if need
                        ;
                        if (dataSize >0) {
                            byte[] stringBuffer = new byte[dataSize];
                            len = dataIn.read(stringBuffer, 0, dataSize);
                            if (dataSize == len) {
                                dataString = new String(stringBuffer);
                                Log.d(C.LOG_TAG, "Get dataString = "+ dataString);
                            } else {
                                Log.e(C.LOG_TAG, "[ERROR]: Unable to get full data because size is wrong");
                            }
                        }

                        // do some logic here
                        switch (action) {
                            case TRIGGER_ACTION_SET_TESTER:
                                tester = dataString;
                                break;
                            case TRIGGER_ACTION_SET_POSITION:
                                position = dataString;
                                break;
                            case TRIGGER_ACTION_SET_TARGET:
                                target = dataString;
                                caller.remoteTriggerChangeTarget(dataString);
                                break;
                            case TRIGGER_ACTION_SET_SCALE:
                                scale = dataByte;
                                caller.remoteTriggerChangeScaleTo(scale);
                                break;
                            case TRIGGER_ACTION_START:
                                caller.remoteTriggerAskStart(dataByte);
                                break;
                            case TRIGGER_ACTION_STOP:
                                caller.remoteTriggerAskStop();
                                break;
                            case TRIGGER_ACTION_GIVEUP:
                                caller.remoteTriggerAskRevoke();
                            case TRIGGER_ACTION_SAVE:
                                caller.remoteTriggerAskSave(dataString);
                            default:
                                break;
                        }

                    } else {
                        Log.e(C.LOG_TAG, "[ERROR]: Unable to read enough bytes -> format errors!!!");
                    }
                } else {
                    Log.e(C.LOG_TAG, "[ERROR]: unable to read socket");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    boolean keepSendThreadRunning = false; // control outside the thread to make it work or not
    boolean sendThreadIsStopped = true; // control inside the thread to make sure it run before the previous one has been stopped
    private void sendPacketByAnotherThread(){
        if(!keepSendThreadRunning){ // thread is not running anymore(or yet) -> need to run a new thread
            keepSendThreadRunning = true;
            class DataSendingRunnable implements Runnable {
                public void run() {
                    // *** comment this line just for debugging ***
                    keepPacketSending();
                }
            }
            new Thread(new DataSendingRunnable(), "Data Sending Thread").start();
        }
    }

    private void keepPacketSending(){
        boolean threadNeedInit = true;
        while(keepSendThreadRunning){
            if(threadNeedInit) {
                if (!sendThreadIsStopped) {
                    Log.d(C.LOG_TAG, "WARN: previous thread is not stopped yet");
                } else {
                    // indicate this thread is working
                    sendThreadIsStopped = false;
                    threadNeedInit = false;
                }
            } else {
                // send message to remote server
                try{
                    while(!requestQueue.isEmpty()){
                        NetworkRequest r = requestQueue.peek();
                        requestQueue.poll();

                        // reuse the action filed in request
                        dataOut.write((byte)r.action);
                    }
                    // flish data out -> NOTE: try only when all data has been writtened
                    dataOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // TODO: wait/sleep some times
            try {
                Thread.sleep(DATA_SENDING_THREAD_LOOP_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // end of sending thread (could be terminated by close socket)
        sendThreadIsStopped = true;
    }

    // this function open a new thread to close server if need
    public void closeServerIfServerIsAlive(){
        new Thread() {
            public void run() {
                // end the packet sending thread
                keepSendThreadRunning = false;
                keepRecvThreadRunning = false;

                // close server directly since the check alive function doesn't work
                closeServer();
				/*
				boolean serverIsAlive = checkIfServerIsAlive();
				if(serverIsAlive){
					closeServer();
				}*/
            }
        }.start();
    }


    private void closeServer(){
        if(status != SERVER_STATUS_DISABLE){
            try {
                if(status == SERVER_STATUS_CONNECTED) {
                    Log.d(C.LOG_TAG, "send disconnection messgae to socket for closing socket");
                    dataOut.write((byte)TRIGGER_CHECK_STOPSERVER);
                    dataOut.flush();
                }

                sc.close();
            } catch (IOException e) {
                Log.e(C.LOG_TAG, "ERROR: can't being closed : " + e.getMessage());
                e.printStackTrace();
            }
            status = SERVER_STATUS_DISABLE;
        } else {
            Log.w(C.LOG_TAG, "ERROR: server is not connected -> no need to close it");
        }
    }

    public boolean isConnected(){
        return status == SERVER_STATUS_CONNECTED;
    }

}
