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
 * 2015/08/25: add this to manage socket events used to connect matlab server
 * 2015/08/26: update
 * 2015/10/15: update to a genernal class for all EchoTag/BumpFree/MicroButton projects
 * */


public class NetworkControllerForVibrationMeasurement {
	private final static String DEFAULT_SERVER_ADDR = "10.0.0.12"; // MAC air at lab
	//private final static String DEFAULT_SERVER_ADDR = "192.168.1.143"; // MAC air at home
	//private final static String DEFAULT_SERVER_ADDR = "35.2.125.126"; // MWireless at lab
	//private final static String DEFAULT_SERVER_ADDR =  "192.168.0.33"; // NQ Mwireless
	private final static int DEFAULT_SERVER_PORT = 50009;
	private final static int DELAY_WHEN_REQUEST_QUEUE_IS_EMPTY = 5000; // delay when there is nothig to transmit (ms)

	private final static int SOCKET_TIMEOUT = 2000; // ms

	private final static int DATA_SENDING_THREAD_LOOP_DELAY = 10; //ms

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
	private PrintWriter writer = null;

	Queue<NetworkRequest> requestQueue;

	// ACTION_CONNECT format: | ACTION_CONNECT | xxx parater setting
	private final static int ACTION_CONNECT = 1;

	// ACTION_SEND format: | ACTION_DATA | # of bytes to send | byte[] | -1
	private final static int ACTION_DATA = 2;

	// ACTION_CLOSE format: | ACTION_CLOSE |
	private final static int ACTION_CLOSE = -1;

	private final static int ACTION_SET = 3;
	private final static int ACTION_INIT = 4;


	// set action types
	public final static int SET_TYPE_BYTE_ARRAY = 1;
	public final static int SET_TYPE_STRING = 2;
	public final static int SET_TYPE_DOUBLE = 3;
	public final static int SET_TYPE_INT = 4;
	public final static int SET_TYPE_VALUE_STRING = 5; // sent value by string


	NetworkControllerListener caller;
	public NetworkControllerForVibrationMeasurement(NetworkControllerListener callerIn) {
		caller = callerIn;
		serverIp = DEFAULT_SERVER_ADDR;
		serverPort = DEFAULT_SERVER_PORT;
		status = SERVER_STATUS_DISABLE;
		requestQueue = new LinkedList<NetworkRequest>();
	}


	// netowkring can't be in the main thread -> crash
	public void connectServer(String ip, int port){
		serverIp = ip;
		serverPort = port;
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

					dataOut.write(ACTION_CONNECT);
					dataOut.flush();


					createThreadToReceiveAndBlockArrivePacket();


					status = SERVER_STATUS_CONNECTED;
					caller.isConnected(true, "Connected!");
				} catch (UnknownHostException e) {
					Log.e(C.LOG_TAG, "ERROR: can't connect socket : " + e.getMessage());
					e.printStackTrace();
					caller.isConnected(false, "UnknownHostException" + e.getMessage());
				} catch (IOException e) {
					Log.e(C.LOG_TAG, "ERROR: can't connect socket : " + e.getMessage());
					e.printStackTrace();
					caller.isConnected(false, "IOException" + e.getMessage());
				}
			}
		}.start();
	}
	public void sendInitAction(){
		NetworkRequest r = new NetworkRequest(ACTION_INIT, null, null, -1);
		requestQueue.add(r);
		sendPacketByAnotherThread();
	}

	public void sendDataRequest(byte[] data){
		NetworkRequest r = new NetworkRequest(ACTION_DATA, null, data, -1);
		requestQueue.add(r);
		sendPacketByAnotherThread();
	}

	public void sendSetAction(int setType, String name, byte[] data){
		NetworkRequest r = new NetworkRequest(ACTION_SET, name, data, setType);
		requestQueue.add(r);
		sendPacketByAnotherThread();
	}


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
				int BYTE_TO_READ = 1;
				byte[] b = new byte[BYTE_TO_READ];
				int byteIsRead = dataIn.read(b, 0, BYTE_TO_READ);
				int cmd = b[0];
				caller.consumeReceivedData((double)cmd);
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

						switch (r.action){
							case ACTION_INIT:
								dataOut.write(ACTION_INIT);
								break;
							case ACTION_DATA:
								dataOut.write(ACTION_DATA);
								dataOut.writeInt(r.data.length);
								dataOut.write(r.data);
								dataOut.write(-1); // use as the sanity check of send message
								break;
							case ACTION_SET:
								dataOut.write(ACTION_SET);
								dataOut.writeInt(r.type);
								byte[] nameBytes = r.name.getBytes();
								dataOut.writeInt(nameBytes.length);
								dataOut.write(nameBytes);
								dataOut.write(-1); // use as the sanity check of send message
								dataOut.writeInt(r.data.length);
								dataOut.write(r.data);
								dataOut.write(-1); // use as the sanity check of send message
								break;
							default:
								Log.e(C.LOG_TAG, "[ERROR]: undefined action = "+r.action);
						}
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

	// *** NOTE: all those method is deprecated and changed to new methd with request queue
	// sent init action to help server start parsing and init necessary buffers
	/*
	public void sendInitAction(){
		try {
			dataOut.write(ACTION_INIT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// TODO: making data sent process in the other thread
	public void sendDataRequest(byte[] data){
		try {
			dataOut.write(ACTION_DATA);

			//String a = "XXXX555";
			//byte[] b = new byte[10];
			//b = a.getBytes();
			Log.d(C.LOG_TAG, "sendDataRequest: byte length = " + data.length);

			dataOut.writeInt(data.length);
			dataOut.write(data);

			dataOut.write(-1); // use as the sanity check of send message
			dataOut.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// TODO: making data sent process in the other thread
	public void sendSetAction(int setType, String name, byte[] data){
		try {
			dataOut.write(ACTION_SET);

			dataOut.writeInt(setType);

			byte[] nameBytes = name.getBytes();
			dataOut.writeInt(nameBytes.length);
			dataOut.write(nameBytes);
			dataOut.write(-1); // use as the sanity check of send message

			dataOut.writeInt(data.length);
			dataOut.write(data);
			dataOut.write(-1); // use as the sanity check of send message

			dataOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	*/

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

	// TODO: this function is wrong -> always return true -> no -1 when there is nothing to receive
	/*
	private boolean checkIfServerIsAlive(){
		if(status != SERVER_STATUS_DISABLE){
			try {
				if(status == SERVER_STATUS_CONNECTED) {
					// 1. try to read for knowing if server is dead
					byte[] temp = new byte[1];
					int result = dataIn.read(temp);
					if(result == -1){
						return false;
					} else {
						return true;
					}
				}
				sc.close();
			} catch (IOException e) {
				Log.e(C.LOG_TAG, "get time out exception -> need to close socket : " + e.getMessage());
				e.printStackTrace();
				return true;
			}
			status = SERVER_STATUS_DISABLE;
		} else {
			Log.w(C.LOG_TAG, "ERROR: server is not connected -> no need to check it");
			return false;
		}
		// return when Exception happens
		return true;
	}
	*/

	private void closeServer(){
		if(status != SERVER_STATUS_DISABLE){
			try {
				if(status == SERVER_STATUS_CONNECTED) {
					Log.d(C.LOG_TAG, "send disconnection messgae to socket for closing socket");
					dataOut.write(ACTION_CLOSE);
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
