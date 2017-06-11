/*
* 2015/09/28: update to standalone app
* */
package edu.umich.cse.audioanalysis;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import edu.umich.cse.audioanalysis.AudioRelated.AudioSource;


public class SpectrumSurvey {
/////////////////////////////////////////////////////////////////////////////////////////////////
// 1. Constants and static configuration
/////////////////////////////////////////////////////////////////////////////////////////////////
	// *** begin control variables, condition based on these variables are evaluated in compile time ***
	private static final boolean DUMP_LOG = true;
	private static final boolean WRITE_RECORD_TO_FILE = true;
	private static final boolean WRITE_RECORD_TO_STREAM = true;
	private static final boolean IS_STEREO = true;
	private static final boolean SAVE_SURVEY_DATA_TO_FILE = false;
	private static final boolean REPEAT_SENSING = false; // choice of stoping sensing when a round or keep sensing
	private static final boolean SHOW_AUDIO_RECORDING_LOG = false;

	//private static final float DEFAULT_VOL = 0.05f;
	public int RECORDER_SAMPLERATE = -1; // determined by contructor
	private int RECODER_CHANNEL_CNT = 2; // this function is fixed to 2
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	// 7680 bytes is the min buffer value of note4/s5 for FS = 48000
	private static final int RECORDER_BYTE_PER_ELEMENT = 2; // 2 bytes in 16bit format
	//private static final int RECORDER_SINGLE_REPEAT_ELEMENTS = 4800;
	private static final int RECORDER_BUFFER_ELEMENTS = 4800*2; // this is the size of audio buffer elements *** WARN: this number must be a multiple of  ***
	private static final int RECORDER_TOTAL_BUFFER_SIZE = 480000; // total size of audio buffer (not necessary to read it all every thime)
	private static final int RECORDER_INIT_TRACK_CNT = 11;
	private static final int RECORDER_LATTER_TRACK_CNT = 5; // this size determines the number of audio buffer will be saved or proccessed
	private static final int RECORDER_REPEAT = 490; // WARN: this part should match the source file!

	private static final int TIME_TO_START_SURVEY = 200; // initial start delay // *** this is a test for online version ***
	private static final int TIME_TO_WAIT_PLAY = 200; // this could be set to 0, or other number to ensure get more data before audio is played
	private static final int TIME_TO_WAIT_STOP_RECORD = 1000;
	private static final int TIME_BETWEEN_SURVEY = 1000; // *** this is a test for online version ***
	private static final int TIME_DELAY_TO_SAVE_NEXT_SHOT = 200;

	private String audioSettingName; // assingned by constructor now


	private static final String logFolder = "log/";
	private static final String inputPrefix = "source_";
	private static final String outputPrefix = "record_";

	boolean USE_AUDIO_TRACK_TO_PLAY_BINARY_FILE = true;

	int LOCATION_SENSING_CNT = 4; // number of sensing is ncessary for each location


	// flag settings
	// -----|   isSurveying: can be controlled to interrupt survey (future design)
	// --------|  isPlayingAndRecording: controlled by mTimer for protect mStartAndKeepRecording
	// ----------|  mStartAndKeepRecording: can only be udpated by internal functions (ex: timers)


	// *** end of control variables ***
	private AudioRecord mAudioRecord = null;
	private String mSaveLocation = null;
	private String mSaveName = null;
	private String mServerName = null;
	private RecordTimer mRecordTimer = null;
	SimpleDateFormat mDateFormat;
	MediaPlayer.OnCompletionListener mAudioCompleteListener = null;

	// audio sense variables
	private boolean mStartAndKeepRecording = false;
	private Thread mAudioRecordingThread = null;
	private Object mAudioRecordingSyncToken = null;

	// control of timer
	private static final int MESSAGE_TO_START_PLAY_AND_RECORD = 1;
	private static final int MESSAGE_PLAY_IS_STOPPED = 3;
	private static final int MESSAGE_TO_START_SURVEY = 4;
	private static final int MESSAGE_RECORD_IS_STOPPED = 5;
	private static final int MESSAGE_STOP_PROGRESS_DIAG = 6;
	private static final int MESSAGE_TO_START_PLAY = 7;

	// my final setting for batch test
	AudioSource audioSourceTrain;
	AudioSource audioSourcePredict;


	private int mSurveyMode;
	private MediaPlayer mPlayerSelected = null;
	private AudioTrack mAudioTrack = null;
	private String mInputConfigSelected = null;
	SurveyEndListener mSurveyEndListener = null;

	short pilot[];
	short signal[];
	ByteArrayOutputStream recordByteStream = null;

	private static int recordIdx;
	private static boolean isPlayingAndRecording = false;
	private boolean isKeepRecording = false;
	public static boolean isSurveying;

	//public ByteArrayOutputStream mSurveyStereoByteData;

	public int audioTotalRecordedSampleCnt;


	Context context;

/////////////////////////////////////////////////////////////////////////////////////////////////
//  Setting functions (ex: constructoer and reset)
/////////////////////////////////////////////////////////////////////////////////////////////////
	public SpectrumSurvey(int RECORDER_SAMPLERATE, String audioSettingName, Context context) {
		// set config defaut values
		isSurveying = false;
		isPlayingAndRecording = false;
		isSurveying = false;
		mSurveyMode = C.SURVEY_MODE_TRAIN;
		this.RECORDER_SAMPLERATE = RECORDER_SAMPLERATE;
		this.audioSettingName = audioSettingName;
		this.context = context;

		// create necessary classes
		mDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		mRecordTimer = new RecordTimer();
		//recordRequestQueue = new LinkedList<RecordRequest>();
		
		// Set up audio sources and recorder and their corresponding complete handler
		mAudioCompleteListener = new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
				Log.d(C.LOG_TAG, "Audio Play Complete," + getTime());
				mp.seekTo(0); // roll back to the begining
				mRecordTimer.sendMessageDelayed(Message.obtain(null, MESSAGE_PLAY_IS_STOPPED), TIME_TO_WAIT_STOP_RECORD);
			}
		};
		
		
		if(!USE_AUDIO_TRACK_TO_PLAY_BINARY_FILE) {
			audioSourceTrain = new AudioSource(audioSettingName, C.DEFAULT_VOL, mAudioCompleteListener);
			audioSourcePredict = new AudioSource(audioSettingName, C.DEFAULT_VOL, mAudioCompleteListener);
		}
		
		// Set up audio recorder
		Log.d(C.LOG_TAG, "min buffer size = " + String.valueOf(AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_IN_STEREO, RECORDER_AUDIO_ENCODING)));
		//mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_IN_STEREO, RECORDER_AUDIO_ENCODING, RECORDER_TOTAL_BUFFER_SIZE);
		//mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_IN_STEREO, RECORDER_AUDIO_ENCODING, RECORDER_TOTAL_BUFFER_SIZE);
		// Use device-specific setting
		mAudioRecord = new AudioRecord(D.RECORD_SOURCE, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_IN_STEREO, RECORDER_AUDIO_ENCODING, RECORDER_TOTAL_BUFFER_SIZE);
	}


	// this functions must be called only if isIdle is set to true
	public boolean initSurvey(SurveyEndListener listener, int surveyMode) {
		if (isSurveying == true) {
			return false;
		}

		audioTotalRecordedSampleCnt = 0;

		mSurveyEndListener = listener;
		mSurveyMode = surveyMode;

		if (!USE_AUDIO_TRACK_TO_PLAY_BINARY_FILE) {
			if (surveyMode == C.SURVEY_MODE_TRAIN) {
				mPlayerSelected = audioSourceTrain.mPlayer;
				mInputConfigSelected = audioSourceTrain.config;
			} else if (surveyMode == C.SURVEY_MODE_PREDICT) {
				mPlayerSelected = audioSourcePredict.mPlayer;
				mInputConfigSelected = audioSourcePredict.config;
			} else {
				Log.e(C.LOG_TAG, "ERROR: undefined survey mode");
			}
			mPlayerSelected.seekTo(0);
		} else {
			// load audio signals from binary dat file
			int PLAYER_TOTAL_BUFFER_SIZE = 960*4*2; // just for debug
			pilot = Utils.readBinaryAudioDataFromAsset(context, "source_48000rate-5000repeat-2400period+chirp-18000Hz-24000Hz-1200samples+namereduced_pilot.dat");
			signal = Utils.readBinaryAudioDataFromAsset(context, "source_48000rate-5000repeat-2400period+chirp-18000Hz-24000Hz-1200samples+namereduced_signal.dat");
			mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, PLAYER_TOTAL_BUFFER_SIZE, AudioTrack.MODE_STREAM);

			/*
			try {
				// load pilot array
				//InputStream is = context.getAssets().open("source_48000rate-5000repeat-2400period+chirp-18000Hz-24000Hz-1200samples+namereduced_pilot.dat");
				InputStream is = context.getAssets().open("engineSoundShortAmp20000.dat");

				byte[] fileBytes=new byte[is.available()];
				//PLAYER_TOTAL_BUFFER_SIZE = is.available(); // use pilot size as the total buffer size
				int arraySize = is.available()/2; // short take 2 bytes
				pilot = new short[arraySize];
				is.read(fileBytes, 0, is.available()); // read all buffer into bytebuffer
				ByteBuffer.wrap(fileBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pilot);

				// TODO: load signal
				Log.d(C.LOG_TAG, "pilot has been loaded");
			} catch (IOException e) {
				e.printStackTrace();
			}
			*/

			//PLAYER_TOTAL_BUFFER_SIZE =

		}
		
		isSurveying = true;
		isPlayingAndRecording = false;
		recordIdx = 0;
		
		// this functions create necessary folders and files *** any creation of files should be after this command ***
		createOutputFiles();

		// init necessary threads *** proper flag has not been set -> recording data will not be saved to files ***
		// start audio recording
		startInitAudioRecording();
		// start tilt recording
		//startInitSensorRecording();
		
		return true;
	}
	
	public void startSurvey(){
		//mSurveyStereoByteData = null;
		//mSurveyStereoByteData = new ByteArrayOutputStream();
		mRecordTimer.sendMessageDelayed(Message.obtain(null, MESSAGE_TO_START_SURVEY), TIME_TO_START_SURVEY);
	}

	// public interace for caller to stop this survey
	public void stopSurvey(){
		isSurveying = false;
		// NOTE: the other flags will be set automatically when isSurveying is set to false
	}

	// this function must be called in the end of survey
	private void endSurvey(){
		Log.d(C.LOG_TAG, "endSurvey: finalize folder names");

		// release audio record
		mAudioRecord.release();
		mAudioRecord = null;
		//mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_IN_STEREO, RECORDER_AUDIO_ENCODING, RECORDER_TOTAL_BUFFER_SIZE);
			
		
		String oldFolderPath = C.appFolderPath + C.DEBUG_FOLDER;
		String newFolderPrefix = C.appFolderPath + C.DEBUG_FOLDER.substring(0, C.DEBUG_FOLDER.length()-1) + "_"+mSaveLocation+"_"+mSaveName;
		File oldFolder = new File(oldFolderPath);
		
		// TODO: release mAudioRecord
		
		// this variable must be set in the end of this function
		
		isSurveying = false;
		mSurveyEndListener.onSurveyEnd();
		
	}


	
	// this function only use for debug raw data output
	PrintWriter mMatlabOutFile;
	private void createOutputFiles(){
		// erase the the output folder if it existed
		File folder = new File(C.appFolderPath + C.DEBUG_FOLDER);
		boolean success = true;
		if(folder.exists()){
			// erase the folder
			DeleteRecursive(folder);
		}
		
		// create necessary folders
		if (!folder.exists()) {
		    success = folder.mkdir();
		    Log.d(C.LOG_TAG, "folder is not existed, create one");
		} else {
			Log.e(C.LOG_TAG, "ERROR: folder has not been deleted");
		}
		File monitorFolder = new File(C.appFolderPath + C.DEBUG_FOLDER + logFolder);
		monitorFolder.mkdir();

		try {
			mMatlabOutFile = new PrintWriter(new FileOutputStream(new File(C.appFolderPath+ C.DEBUG_FOLDER +"matlab.txt"),true));
			mMatlabOutFile.println(inputPrefix+audioSettingName);
			mMatlabOutFile.println(RECODER_CHANNEL_CNT);
			mMatlabOutFile.println(C.DEFAULT_VOL);
			mMatlabOutFile.close();
			mMatlabOutFile = null;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.e(C.LOG_TAG, "ERROR: can't open sensor file to write");
			e.printStackTrace();
		}
	}
	
	
/////////////////////////////////////////////////////////////////////////////////////////////////
// 2. Audio recording related functions	
/////////////////////////////////////////////////////////////////////////////////////////////////
	// function to start recording thread
	private void startInitAudioRecording() {
		// jsut a function to start audio recording thread
		class AudioRecordingRunnable implements Runnable {
			Object syncToken;
			AudioRecordingRunnable(Object mAudioRecordingSyncToken) {
				syncToken = mAudioRecordingSyncToken;
			}
			@Override
			public void run() {
				// *** comment this line just for debugging ***

				// chage thread priority again
				// ref:  http://stackoverflow.com/questions/5198518/whats-the-difference-between-thread-setpriority-and-android-os-process-setthre


				int tid=android.os.Process.myTid();
				Log.d(C.LOG_TAG,"Process priority before change = " + android.os.Process.getThreadPriority(tid));
				Log.d(C.LOG_TAG, "Process priority before change = " + Thread.currentThread().getPriority());
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
				Log.d(C.LOG_TAG,"Process priority after change = " + android.os.Process.getThreadPriority(tid));
				Log.d(C.LOG_TAG,"Process priority after change = " + Thread.currentThread().getPriority());


				keepAudioRecording(syncToken);
			}
		}
		mAudioRecordingSyncToken = new Object();
		mAudioRecordingThread = new Thread(new AudioRecordingRunnable(mAudioRecordingSyncToken), "Audio Recording Thread");

		// change the thread setting
		// ref:  http://stackoverflow.com/questions/9019137/changing-thread-priority-doesnt-have-an-effect
		Log.d(C.LOG_TAG, "Thread priority before change = "+mAudioRecordingThread.getPriority());
		mAudioRecordingThread.setPriority(Thread.MAX_PRIORITY);
		Log.d(C.LOG_TAG, "Thread priority after change = " + mAudioRecordingThread.getPriority());
		mAudioRecordingThread.start();
	}

	private void keepAudioRecording(Object syncToken) {
		byte[] byteBuffer = new byte[RECORDER_BUFFER_ELEMENTS* RECORDER_BYTE_PER_ELEMENT];
		audioTotalRecordedSampleCnt = 0; // init this log value to 0 for every recording
		// trigger recording
		mAudioRecord.startRecording();
		
		/*
		while(mAudioRecord.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING){
			Log.e(C.LOG_TAG,"ERROR: audio has not prepared");
		}
		*/
		Log.w(C.LOG_TAG, "WARN: TODO: find a nother way to wait audio resource released");
		
		// start recording infinite loop (stop when recording is done)
		while(true){
			// start to save data only if mStartAndKeepRecording is triggered
			if(mStartAndKeepRecording){
				//mSensorController.fileToSave = mSensorOutFile;
				// start recording
				int trackCnt = 0;
				
				for (int trackIdx = 0; trackIdx < RECORDER_REPEAT; trackIdx++) {
					// 1. reset a new recordRequest
					if(SHOW_AUDIO_RECORDING_LOG) Log.d(C.LOG_TAG, "recordIdx = " + recordIdx);

					String recordSuffix = String.format("%04d", recordIdx);
					String outputFileName = C.appFolderPath + C.DEBUG_FOLDER + outputPrefix + mInputConfigSelected + "-" + recordSuffix+ ".txt";

					if(SHOW_AUDIO_RECORDING_LOG) Log.d(C.LOG_TAG, outputFileName);


					RecordRequest r = new RecordRequest(recordIdx,outputFileName, getTime());
					r.byteStreamToSave = new ByteArrayOutputStream();

					// 2. just read period of data
					for(trackCnt=0;trackCnt<RECORDER_LATTER_TRACK_CNT;trackCnt++){
						int byteRead = mAudioRecord.read(byteBuffer, 0, RECORDER_BUFFER_ELEMENTS * RECORDER_BYTE_PER_ELEMENT);
						if(SHOW_AUDIO_RECORDING_LOG) {
							Log.d(C.LOG_TAG, "byteRead = " + byteRead);
						}
						if(byteRead != RECORDER_BUFFER_ELEMENTS * RECORDER_BYTE_PER_ELEMENT){
							Log.e(C.LOG_TAG, "byteRead not matched! = "+byteRead);
						}

						try {
							// update sample cnt for logging purpose
							audioTotalRecordedSampleCnt += byteRead / (RECODER_CHANNEL_CNT*RECORDER_BYTE_PER_ELEMENT); // only recorded the number of "sample" in each channel as a reference point
							mSurveyEndListener.audioRecorded(byteBuffer.clone(), audioTotalRecordedSampleCnt);

							// save memory
							if(C.TRACE_SAVE_TO_FILE && C.SURVEY_DUMP_RAW_BYTE_DATA_TO_FILE) {
								r.byteStreamToSave.write(byteBuffer);
							}
							//mSurveyStereoByteData.write(byteBuffer);
	    				} catch (IOException e) {
	    					Log.e(C.LOG_TAG, "ERROR: IOError in writing stream");
	    					e.printStackTrace();
	    				}
					}
    				//Log.d(C.LOG_TAG, "trackIdx=" + trackIdx + ", read " + trackCnt + " times");
    				//Log.d(C.LOG_TAG, r.dump());

					// 3. call survey end audio recorded callback // TODO: remove this part since it is moved forward
					//mSurveyEndListener.audioRecorded(r.byteStreamToSave);

					// 4. open a new thread to save raw record (if need)
					if(C.TRACE_SAVE_TO_FILE && C.SURVEY_DUMP_RAW_BYTE_DATA_TO_FILE) {
						class RecordSavingRunnable implements Runnable {
							RecordRequest recordRequest;
							RecordSavingRunnable(RecordRequest recordIn) {
								recordRequest = recordIn;
							}
							@Override
							public void run() {
								keepRecordSaving(recordRequest);
							}
						}
						Thread recordSavingThread = new Thread( new RecordSavingRunnable(r), "Record Saving " + r.recordIdx + " Thread");
						recordSavingThread.start();
					}

					
					// 4. update recordIdx
					recordIdx++;
					if(SHOW_AUDIO_RECORDING_LOG) {
						Log.d(C.LOG_TAG, "end of one track");
					}

					
					// 5. check if need to stop
					if (!mStartAndKeepRecording || !isSurveying) {
						Log.d(C.LOG_TAG, "stop recording");
						if (!isSurveying && mStartAndKeepRecording) {
							Log.d(C.LOG_TAG, "recorder is forced to stop, so as the player!");
							if(USE_AUDIO_TRACK_TO_PLAY_BINARY_FILE){
								mAudioTrack.stop();
							} else {
								mPlayerSelected.stop();
							}
						}
						mStartAndKeepRecording = false;
						break;
					} 
				}

				Log.d(C.LOG_TAG, "end of inner while loop in recording, repeat is over");
				mRecordTimer.sendMessage(Message.obtain(null, MESSAGE_RECORD_IS_STOPPED));
				isKeepRecording = false;
				
				break;
			} else {
				// do nothing, just read out the buffer
				mAudioRecord.read(byteBuffer, 0, RECORDER_BUFFER_ELEMENTS * RECORDER_BYTE_PER_ELEMENT);
			}	
		}
	}

	// used to dump data for matlab
    private void keepRecordSaving(RecordRequest recordRequest) {
    	Log.d(C.LOG_TAG, "start of keepRecordSaving thread " + recordRequest.recordIdx);
    	Log.d(C.LOG_TAG, "record = " + recordRequest.dump());
    	
    	//Boolean writeResult = Highgui.imwrite(shotRequest.filePath, shotRequest.imageToSave);
    	try {
			FileOutputStream fos = new FileOutputStream(recordRequest.filePath);
			recordRequest.byteStreamToSave.writeTo(fos);
			fos.close();
		} catch (FileNotFoundException e) {
			Log.e(C.LOG_TAG, "ERROR: can't open file when saving record");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(C.LOG_TAG, "IOERROR: can't open file when saving record");
			e.printStackTrace();
		}
    }

	private void keepAudioPlaying() {
		// write pilot (just once)
		int writeCnt = mAudioTrack.write(pilot, 0, pilot.length);
		if(writeCnt != pilot.length){
			Log.e(C.LOG_TAG, "Audio pilot play write size in consistent, writeCnt = "+writeCnt);
		}


		// write signals (forever)
		while(isPlayingAndRecording){
		//while(true){
			writeCnt = mAudioTrack.write(signal, 0, signal.length);

			if(writeCnt != signal.length){
				Log.e(C.LOG_TAG, "Audio signal play write size in consistent, writeCnt = "+writeCnt);
			}
		}
	}

	private void startPlaying() {
		Log.d(C.LOG_TAG, "Play audio" + getTime());
		if(!USE_AUDIO_TRACK_TO_PLAY_BINARY_FILE) {
			mPlayerSelected.start();
		} else {
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

	private void startPlayAndRecord() {
		Log.d(C.LOG_TAG, "startRecord");

		if (isPlayingAndRecording) {
			Log.e(C.LOG_TAG, "It is recording, can't record again");
		} else {
			//set the flag to protect this section
			isPlayingAndRecording = true;
			
			// *** moved this part of code to startPlayAndRecord for adding delay before recording ***
			//RecordRequest r = new RecordRequest(recordIdx, outputFileName, getTime());
			//recordRequestQueue.add(r);
			mStartAndKeepRecording = true; // trigger background recorder to save data
			mRecordTimer.sendMessage(Message.obtain(null, MESSAGE_TO_START_PLAY_AND_RECORD));
		}
	}
	
	private class RecordTimer extends Handler {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Log.d(C.LOG_TAG, "RecordTimer: get a message (what,arg1,arg2) = (" + msg.what + "," + msg.arg1 + "," + msg.arg2 + ")" + getTime());
			String configName = mInputConfigSelected;
			String recordSuffix = String.format("%04d", recordIdx);

			if (msg.what == MESSAGE_TO_START_SURVEY) {
				String shotName =  outputPrefix + configName + "-" + recordSuffix + ".jpg";
				String shotPath = C.appFolderPath + C.DEBUG_FOLDER + shotName;
				
				
				
				// acoustic part
				if (isPlayingAndRecording) {
					Log.e(C.LOG_TAG, "ERROR: it is still recording!!");
				} else {
					startPlayAndRecord();
				}
				
			} else if (msg.what == MESSAGE_TO_START_PLAY_AND_RECORD) {
				Log.d(C.LOG_TAG, "start to prepare play" + getTime());

				// set up recording request
				String outputFileName = C.appFolderPath + C.DEBUG_FOLDER + outputPrefix + configName + "-" + recordSuffix + ".txt";
				Log.d(C.LOG_TAG, outputFileName);


				// make a small delay before recording
				mRecordTimer.sendMessageDelayed(Message.obtain(null, MESSAGE_TO_START_PLAY), TIME_TO_WAIT_PLAY);
			} else if (msg.what == MESSAGE_TO_START_PLAY) {

				// start playing
				startPlaying();
			} else if (msg.what == MESSAGE_PLAY_IS_STOPPED) {
				Log.d(C.LOG_TAG, "stop play");
				
				// force recorder to stop
				if(mStartAndKeepRecording == false){
					Log.e(C.LOG_TAG, "ERROR: try to stop a recorder not runing!");
				}
				
				mStartAndKeepRecording = false;
				
			/*	*** keep it for the future test of energy, CPU...etc ***
			} else if (msg.what == MESSAGE_TO_START_SAVE_SHOT) {
				// begin to take shot
				
				String shotPath = systemPath+outputFolder+String.format("%04d.jpg", shotIdx);
				
				if(mSaveShot){
					Log.w(C.LOG_TAG,"WARN: shot is saving too closely!!");
				} else {
					// change the flage, frame capture will do its job
					shotRequestQueue.add(new ShotRequest(shotIdx, shotPath, getTime()));
					mSaveShot = true;
					shotIdx++;
				}
				
				mRecordTimer.sendMessageDelayed(Message.obtain(null, MESSAGE_TO_START_SAVE_SHOT), TIME_DELAY_TO_SAVE_NEXT_SHOT);
				*/
			} else if (msg.what == MESSAGE_RECORD_IS_STOPPED) {
				Log.d(C.LOG_TAG, "record is stopped," + getTime());
				//Log.d(C.LOG_TAG,"mAudioRecord recording state = "+mAudioRecord.getRecordingState());

				// end of one whole survey (one round)
				isPlayingAndRecording = false;
					
						
				// make up next survey if need
				if (isSurveying && REPEAT_SENSING) {
					mRecordTimer.sendMessageDelayed(Message.obtain(null, MESSAGE_TO_START_SURVEY), TIME_BETWEEN_SURVEY);
				} else {
					// end of survey
					//isSurveying = false; // make the survey thread stop automatically
					
					endSurvey();
					//showSaveDiag();
				}
			} else if (msg.what == MESSAGE_STOP_PROGRESS_DIAG){
				//progress.dismiss();
				// TODO: add new logic to handle end of progress message
			} else {
				Log.e(C.LOG_TAG, "undefined message type");
			}

		}

	}	

	
/////////////////////////////////////////////////////////////////////////////////////////////////
// 3. Other Functions Calls	
/////////////////////////////////////////////////////////////////////////////////////////////////
	
	//========================== some common used, not so important functions ============================
	String getTime(){
		Calendar c = Calendar.getInstance();
        int mseconds = c.get(Calendar.MILLISECOND);
        String currentDateandTime = mDateFormat.format(new Date()) + String.format("-%04d", mseconds);
        
        return currentDateandTime;
	}
		
	void DeleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory()) {
			File[] subFiles = fileOrDirectory.listFiles();
			for(int i=0;i<subFiles.length;i++){
				DeleteRecursive(subFiles[i]);
			}
			//for (File child : fileOrDirectory.listFiles())
			//	DeleteRecursive(child);
		}
	    fileOrDirectory.delete();
	}
	
}
