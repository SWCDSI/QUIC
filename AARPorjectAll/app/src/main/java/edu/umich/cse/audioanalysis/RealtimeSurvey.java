/*
* 2015/09/28: update to standalone app
* 2016/05/16: update to reduce the jitter needed to get traces
* */

package edu.umich.cse.audioanalysis;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import edu.umich.cse.audioanalysis.AudioRelated.AudioSource;

public class RealtimeSurvey {
/////////////////////////////////////////////////////////////////////////////////////////////////
// 1. Constants and static configuration
/////////////////////////////////////////////////////////////////////////////////////////////////



	public boolean needToSaveTrace = false;

	//private static final float DEFAULT_VOL = 0.05f;
	public int RECORDER_SAMPLERATE = -1; // determined by contructor
	private int RECODER_CHANNEL_CNT = 2; // this function is fixed to 2
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	// 7680 bytes is the min buffer value of note4/s5 for FS = 48000
	private static final int RECORDER_BYTE_PER_ELEMENT = 2; // 2 bytes in 16bit format
	//private static final int RECORDER_SINGLE_REPEAT_ELEMENTS = 4800;
	//private static final int RECORDER_BUFFER_ELEMENTS = 4800*2; // this is the size of audio buffer elements *** WARN: this number must be a multiple of  ***
	private static final int RECORDER_BUFFER_ELEMENTS = 960; // this is the size of audio buffer elements *** WARN: this number must be a multiple of  ***
	private static final int RECORDER_TOTAL_BUFFER_SIZE = 480000; // total size of audio buffer (not necessary to read it all every thime)
	private static final int PLAYER_TOTAL_BUFFER_SIZE = 48000; // used for auido track class
	private static final int RECORDER_INIT_TRACK_CNT = 11;
	private static final int RECORDER_LATTER_TRACK_CNT = 5; // this size determines the number of audio buffer will be saved or proccessed
	private static final int RECORDER_REPEAT = 490; // WARN: this part should match the source file!

	private static final int TIME_TO_START_SURVEY = 0; // initial start delay // *** this is a test for online version ***
	private static final int TIME_TO_WAIT_PLAY = 0; // this could be set to 0, or other number to ensure get more data before audio is played
	private static final int TIME_TO_WAIT_STOP_RECORD = 1000;
	private static final int TIME_BETWEEN_SURVEY = 1000; // *** this is a test for online version ***

	private String audioSettingName; // assingned by constructor now

	private static final String logFolder = "log/";
	private static final String inputPrefix = "source_";
	private static final String outputPrefix = "record_";


	// flag settings
	// -----|   isSurveying: can be controlled to interrupt survey (future design)
	// --------|  isPlayingAndRecording: controlled by mTimer for protect mStartAndKeepRecording
	// ----------|  mStartAndKeepRecording: can only be udpated by internal functions (ex: timers)

	// *** end of control variables ***
	private AudioRecord mAudioRecord = null;
	private AudioTrack mAudioTrack = null;
	private String mSaveLocation = null;
	private String mSaveName = null;
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
	AudioSource audioSource;


	private MediaPlayer mPlayerSelected = null;
	private String mInputConfigSelected = null;

	SurveyEndListener mSurveyEndListener = null;

	ByteArrayOutputStream recordByteStream = null;

	private static int recordIdx;
	private static boolean isPlayingAndRecording = false;
	private boolean isKeepRecording = false;
	public static boolean isSurveying;

	public ByteArrayOutputStream mSurveyStereoByteData;

	public int audioTotalRecordedSampleCnt;

	public DelayAnalyzer delayAnalyzer;

	// my final setting for batch test


/////////////////////////////////////////////////////////////////////////////////////////////////
//  Setting functions (ex: constructoer and reset)
/////////////////////////////////////////////////////////////////////////////////////////////////
	public RealtimeSurvey(int RECORDER_SAMPLERATE, String audioSettingName) {
		// set config defaut values
		isSurveying = false;
		isPlayingAndRecording = false;

		this.RECORDER_SAMPLERATE = RECORDER_SAMPLERATE;
		this.audioSettingName = audioSettingName;

		// create necessary classes
		mDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		mRecordTimer = new RecordTimer();

		delayAnalyzer = new DelayAnalyzer(1000);

		// Set up audio sources and recorder and their corresponding complete handler
		mAudioCompleteListener = new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				Log.d(C.LOG_TAG, "Audio Play Complete," + getTime());
				mp.seekTo(0); // roll back to the begining
				mRecordTimer.sendMessageDelayed(Message.obtain(null, MESSAGE_PLAY_IS_STOPPED), TIME_TO_WAIT_STOP_RECORD);
			}
		};
		audioSource = new AudioSource(audioSettingName, C.DEFAULT_VOL, mAudioCompleteListener);
		
		// Set up audio recorder
		Log.d(C.LOG_TAG, "AndroidRecord: min buffer size = " + String.valueOf(AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_IN_STEREO, RECORDER_AUDIO_ENCODING)));

		// Use device-specific setting
		mAudioRecord = new AudioRecord(D.RECORD_SOURCE, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_IN_STEREO, RECORDER_AUDIO_ENCODING, RECORDER_TOTAL_BUFFER_SIZE);

		// Set up audio track (player)
		Log.d(C.LOG_TAG, "AndroidRecord: min buffer size = " + String.valueOf(AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, RECORDER_AUDIO_ENCODING)));
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, PLAYER_TOTAL_BUFFER_SIZE, AudioTrack.MODE_STREAM);
		//mAudioTrack.play();
	}


	// this functions must be called only if isIdle is set to true

	public boolean initSurvey(SurveyEndListener listener, boolean needToSaveTrace){
		if (isSurveying == true){ // can't init twice
			return false;
		}

		delayAnalyzer.reset();
		delayAnalyzer.addTag("initSurvey: begin");

		this.needToSaveTrace = needToSaveTrace;

		audioTotalRecordedSampleCnt = 0;

		mSurveyEndListener = listener;

		//mPlayerSelected = audioSource.mPlayer;
		//mInputConfigSelected = audioSource.config;
		//mPlayerSelected.seekTo(0);

		isSurveying = true;
		isPlayingAndRecording = false;
		recordIdx = 0;
		
		// this functions create necessary folders and files *** any creation of files should be after this command ***
		//createOutputFiles();

		// init necessary threads *** proper flag has not been set -> recording data will not be saved to files ***
		startInitAudioRecording();
        startInitAudioPlayingThread();

		delayAnalyzer.addTag("initSurvey: end");

		return true;
	}
	
	public void startSurvey(){
		delayAnalyzer.addTag("startSurvey: begin");
		mSurveyStereoByteData = null;
		mSurveyStereoByteData = new ByteArrayOutputStream();
		mRecordTimer.sendMessageDelayed(Message.obtain(null, MESSAGE_TO_START_SURVEY), TIME_TO_START_SURVEY);
		delayAnalyzer.addTag("startSurvey: end");
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
		mAudioRecord = new AudioRecord(D.RECORD_SOURCE, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_IN_STEREO, RECORDER_AUDIO_ENCODING, RECORDER_TOTAL_BUFFER_SIZE);
			
		if(needToSaveTrace) {
			String oldFolderPath = C.appFolderPath + C.DEBUG_FOLDER;
			String newFolderPrefix = C.appFolderPath + C.DEBUG_FOLDER.substring(0, C.DEBUG_FOLDER.length() - 1) + "_" + mSaveLocation + "_" + mSaveName;
			File oldFolder = new File(oldFolderPath);
		}
		
		// this variable must be set in the end of this function
		isSurveying = false;
		mSurveyEndListener.onSurveyEnd();
	}


/////////////////////////////////////////////////////////////////////////////////////////////////
// 3. Audio playing (audio track) related functions
/////////////////////////////////////////////////////////////////////////////////////////////////
	private void startInitAudioPlayingThread() {
		// code help by Arun
		// ref: https://github.com/arunganesan/TWatch.P/blob/master/app/src/main/java/edu/umich/eecs/twatchp/Player.java
		new Thread(new Runnable() {
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                delayAnalyzer.addTag("startInitAudioPlayingThread: wait to play");
                mAudioTrack.play();
                delayAnalyzer.addTag("startInitAudioPlayingThread: play finished");
				keepAudioPlaying();
			}
		}).start();
	}

    //int playCnt = 0;
	private void keepAudioPlaying() {
        int PLAY_WRITE_SIZE = 48;

        int currentIdx = 0;
        while(currentIdx<CHIRP_DEBUG.length){
            if(currentIdx == 0){
                delayAnalyzer.addTag("keepAudioPlaying: wait first write end");
            }
            int writeCnt = mAudioTrack.write(CHIRP_DEBUG, currentIdx, PLAY_WRITE_SIZE);
            if(writeCnt != PLAY_WRITE_SIZE){
                Log.e(C.LOG_TAG, "Audio play write size in consistent, writeCnt = "+writeCnt+", when currentIdx = "+currentIdx);
            }

            if(currentIdx == 0){
                delayAnalyzer.addTag("keepAudioPlaying: first write end");
            }

            if(writeCnt < 0 || currentIdx > CHIRP_DEBUG.length){
                Log.d(C.LOG_TAG,"Nothing to write -> end of playing when currentIdx = "+currentIdx);
                break;
            } else {
                delayAnalyzer.addTag("keepAudioPlaying: write finished, currentIdx = "+currentIdx+", writeCnt = "+writeCnt);
                currentIdx += writeCnt;
            }
        }

	}

	
/////////////////////////////////////////////////////////////////////////////////////////////////
// 3. Audio recording related functions
/////////////////////////////////////////////////////////////////////////////////////////////////
	// function to start recording thread
	private void startInitAudioRecording() {
		// jsut a function to start audio recording thread
        delayAnalyzer.addTag("startInitAudioRecording: begin");
		mAudioRecord.startRecording();
        delayAnalyzer.addTag("startInitAudioRecording: startRecording done");
		new Thread(new Runnable() {
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				keepAudioRecording();
			}
		}).start();


        delayAnalyzer.addTag("startInitAudioRecording: end (create the thread)");
		// *** NOTE: this is the old trying to maintian thread is synced and high priority -> no use ***
		// *** keep for only reference ***
		/*
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
		Log.d(C.LOG_TAG, "Thread priority before change = " + mAudioRecordingThread.getPriority());
		mAudioRecordingThread.setPriority(Thread.MAX_PRIORITY);
		Log.d(C.LOG_TAG, "Thread priority after change = " + mAudioRecordingThread.getPriority());
		mAudioRecordingThread.start();
		*/
	}

	private void keepAudioRecording() {
		byte[] byteBuffer = new byte[RECORDER_BUFFER_ELEMENTS* RECORDER_BYTE_PER_ELEMENT];
		audioTotalRecordedSampleCnt = 0; // init this log value to 0 for every recording
		// trigger recording
		//mAudioRecord.startRecording();

		Log.w(C.LOG_TAG, "WARN: TODO: find a nother way to wait audio resource released");
		delayAnalyzer.addTag("keepAudioRecording thread: begin");
		// start recording infinite loop (stop when recording is done)
		while(true){
			// start to save data only if mStartAndKeepRecording is triggered
			if(mStartAndKeepRecording){
				//mSensorController.fileToSave = mSensorOutFile;
				// start recording
				int trackCnt = 0;
				
				for (int trackIdx = 0; trackIdx < RECORDER_REPEAT; trackIdx++) {
					// 1. reset a new recordRequest
					Log.d(C.LOG_TAG, "recordIdx = " + recordIdx);
					String recordSuffix = String.format("%04d", recordIdx);
					String outputFileName = C.appFolderPath + C.DEBUG_FOLDER + outputPrefix + mInputConfigSelected + "-" + recordSuffix+ ".txt";
					Log.d(C.LOG_TAG, outputFileName);
					RecordRequest r = new RecordRequest(recordIdx,outputFileName, getTime());
					r.byteStreamToSave = new ByteArrayOutputStream();

					// 2. just read period of data
					for(trackCnt=0;trackCnt<RECORDER_LATTER_TRACK_CNT;trackCnt++){
						if(audioTotalRecordedSampleCnt==0){
							delayAnalyzer.addTag("first audio chunk wait to read");
						}
						int byteRead = mAudioRecord.read(byteBuffer, 0, RECORDER_BUFFER_ELEMENTS * RECORDER_BYTE_PER_ELEMENT);
						if(audioTotalRecordedSampleCnt==0){
							delayAnalyzer.addTag("first audio chunk read finishes");
						}
						Log.d(C.LOG_TAG, "byteRead = " +byteRead);
						if(byteRead != RECORDER_BUFFER_ELEMENTS * RECORDER_BYTE_PER_ELEMENT){
							Log.e(C.LOG_TAG, "byteRead not matched! = "+byteRead);
						}

						try {
							// update sample cnt for logging purpose
							if(audioTotalRecordedSampleCnt==0){
								delayAnalyzer.addTag("first audio chunk ("+byteRead / (RECODER_CHANNEL_CNT*RECORDER_BYTE_PER_ELEMENT)+")");
							} else {
								delayAnalyzer.addTag("audio chunk is read ("+audioTotalRecordedSampleCnt+byteRead / (RECODER_CHANNEL_CNT*RECORDER_BYTE_PER_ELEMENT)+")");
							}
							audioTotalRecordedSampleCnt += byteRead / (RECODER_CHANNEL_CNT*RECORDER_BYTE_PER_ELEMENT); // only recorded the number of "sample" in each channel as a reference point

							mSurveyEndListener.audioRecorded(byteBuffer.clone(), audioTotalRecordedSampleCnt);
							r.byteStreamToSave.write(byteBuffer);
							mSurveyStereoByteData.write(byteBuffer);
	    				} catch (IOException e) {
	    					Log.e(C.LOG_TAG, "ERROR: IOError in writing stream");
	    					e.printStackTrace();
	    				}
					}
    				Log.d(C.LOG_TAG, "trackIdx=" + trackIdx + ", read " + trackCnt + " times");
    				Log.d(C.LOG_TAG, r.dump());

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
					Log.d(C.LOG_TAG, "end of one track");

					
					// 5. check if need to stop
					if (!mStartAndKeepRecording || !isSurveying) {
						Log.d(C.LOG_TAG, "stop recording");
						if (!isSurveying && mStartAndKeepRecording) {
							Log.d(C.LOG_TAG, "recorder is forced to stop, so as the player!");
							//mPlayerSelected.stop();
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
				//mAudioRecord.read(byteBuffer, 0, RECORDER_BUFFER_ELEMENTS * RECORDER_BYTE_PER_ELEMENT);
                delayAnalyzer.addTag("Dummy record: wait to read");
				mAudioRecord.read(byteBuffer, 0, RECORDER_BUFFER_ELEMENTS * RECORDER_BYTE_PER_ELEMENT);
                delayAnalyzer.addTag("Dummy record: read finish");
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
	
	private void startPlaying() {
		Log.d(C.LOG_TAG, "Play audio" + getTime());
		//delayAnalyzer.addTag("startPlaying is called, isPlaying = "+mPlayerSelected.isPlaying());
		//mPlayerSelected.start();

		// record the start of time when audio is turned in into
		/*
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(!mPlayerSelected.isPlaying()){
					Log.d(C.LOG_TAG, "Wait audio is played");
				}
				delayAnalyzer.addTag("startPlaying is called, isPlaying = "+mPlayerSelected.isPlaying());
			}
		}).start();
		*/
	}

	private void startPlayAndRecord() {
		Log.d(C.LOG_TAG, "startRecord");
		delayAnalyzer.addTag("startPlayAndRecord: begin");

		if (isPlayingAndRecording) {
			Log.e(C.LOG_TAG, "It is recording, can't record again");
		} else {
			//set the flag to protect this section
			isPlayingAndRecording = true;
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
				delayAnalyzer.addTag("RecordTimer MESSAGE_TO_START_SURVEY: begin");

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

			} else if (msg.what == MESSAGE_RECORD_IS_STOPPED) {
				Log.d(C.LOG_TAG, "record is stopped," + getTime());
				//Log.d(C.LOG_TAG,"mAudioRecord recording state = "+mAudioRecord.getRecordingState());

				// end of one whole survey (one round)
				isPlayingAndRecording = false;
					
						
				// make up next survey if need
				endSurvey();

			} else if (msg.what == MESSAGE_STOP_PROGRESS_DIAG){
				//progress.dismiss();
				// TODO: add new logic to handle end of progress message
			} else {
				Log.e(C.LOG_TAG, "undefined message type");
			}

		}

	}


/////////////////////////////////////////////////////////////////////////////////////////////////
// 4. Other Functions Calls
/////////////////////////////////////////////////////////////////////////////////////////////////
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
			Log.e(C.LOG_TAG, "ERROR: can't open sensor file to write");
			e.printStackTrace();
		}
	}


	//========================== some common used, not so important functions ============================
	String getTime(){
		Calendar c = Calendar.getInstance();
        int mseconds = c.get(Calendar.MILLISECOND);
        String currentDateandTime = mDateFormat.format(new Date()) + String.format("-%04d", mseconds);
        
        return currentDateandTime;
	}
		
	void DeleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            DeleteRecursive(child);
	    fileOrDirectory.delete();
	}

    // *** just for debug ***
    public static final short [] CHIRP_DEBUG = {32767, 31444, 27579, 21477, 13627, 4663, -4687, -13665, -21534, -27647, -31498, -32765, -31337, -27325, -21052, -13032, -3925, 5515, 14505, 22295, 28230, 31810, 32728, 30901, 26474, 19814, 11477, 2163, -7341, -16232, -23752, -29256, -32272, -32534, -30012, -24916, -17677, -8911, 629, 10123, 18751, 25762, 30544, 32676, 31965, 28466, 22477, 14516, 5276, -4434, -13763, -21887, -28087, -31808, -32715, -30720, -25993, -18949, -10208, -549, 9167, 18071, 25359, 30372, 32653, 31989, 28432, 22298, 14136, 4683, -5204, -14625, -22718, -28739, -32132, -32579, -30030, -24713, -17112, -7923, 2006, 11758, 20425, 27200, 31445, 32759, 31010, 26356, 19225, 10282, 364, -9596, -18658, -25963, -30814, -32743, -31562, -27374, -20571, -11798, -1888, 8210, 17528, 25168, 30390, 32683, 31820, 27875, 21224, 12506, 2564, -7634, -17096, -24895, -30261, -32664, -31860, -27920, -21224, -12428, -2396, 7880, 17384, 25168, 30452, 32706, 31695, 27513, 20572, 11562, 1382, -8944, -18377, -25962, -30926, -32762, -31274, -26607, -19228, -9882, 479, 10800, 20023, 27198, 31583, 32721, 30486, 25100, 17115, 7350, -3184, -13396, -22222, -28737, -32257, -32406, -29161, -22854, -14140, -3932, 6698, 16629, 24810, 30370, 32712, 31581, 27088, 19706, 10214, -376, -10935, -20329, -27546, -31806, -32642, -29957, -24032, -15501, -5283, 5515, 15723, 24225, 30092, 32675, 31685, 27222, 19767, 10134, -621, -11316, -20770, -27933, -32008, -32535, -29446, -23079, -14135, -3606, 7333, 17459, 25636, 30939, 32767, 30904, 25554, 17312, 7105, -3913, -14497, -23440, -29721, -32618, -31793, -27330, -19735, -9870, 1135, 12019, 21525, 28555, 32289, 32288, 28545, 21485, 11922, 964, -10114, -20019, -27590, -31935, -32536, -29316, -22644, -13298, -2377, 8833, 19005, 26933, 31668, 32643, 29731, 23273, 14030, 3103, -8202, -18533, -26645, -31557, -32671, -29844, -23410, -14137, -3146, 8235, 18623, 26752, 31627, 32644, 29671, 23064, 13624, 2505, -8929, -19272, -27246, -31861, -32539, -29187, -22213, -12475, -1178, 10272, 20450, 28080, 32203, 32293, 28332, 20809, 10661, -835, -12236, -22098, -29170, -32550, -31802, -27011, -18779, -8148, 3530, 14765, 24118, 30386, 32758, 30920, 25100, 16041, 4905, -6872, -17769, -26369, -31551, -32635, -29471, -22463, -12516, -926, 10791, 21103, 28651, 32437, 31955, 27259, 18960, 8149, -3747, -15156, -24559, -30704, -32765, -30458, -24083, -14484, -2939, 9006, 19752, 27849, 32202, 32217, 27882, 19774, 8984, -3030, -14642, -24271, -30602, -32766, -30459, -23986, -14226, -2510, 9558, 20321, 28294, 32373, 31987, 27180, 18608, 7452, -4744, -16290, -25577, -31306, -32671, -29473, -22148, -11716, 365, 12404, 22706, 29816, 32726, 31015, 24917, 15286, 3481, -8825, -19885, -28121, -32353, -31969, -27014, -18189, -6751, 5662, 17271, 26401, 31733, 32488, 28550, 20478, 9434, -2985, -14979, -24801, -31018, -32715, -29635, -22218, -11544, 829, 13089, 23432, 30333, 32767, 30365, 23475, 13106, 788, -11653, -22372, -29770, -32737, -30823, -24304, -14146, -1864, 10705, 21676, 29394, 32693, 31065, 24748, 14687, 2396, -10263, -21375, -29247, -32674, -31127, -24831, -14740, -2389, 10336, 21484, 29341, 32694, 31017, 24559, 14308, 1840, -10920, -21997, -29669, -32738, -30719, -23915, -13380, -749, 12006, 22891, 30195, 32767, 30192, 22867, 11935, -884, -13573, -24123, -30861, -32711, -29371, -21362, -9949, 3056, 15581, 25626, 31578, 32478, 28171, 19339, 7393, -5748, -17973, -27305, -32231, -31948, -26490, -16733, -4253, 8924, 20658, 29032, 32674, 30979, 24216, 13482, 531, -12514, -23511, -30644, -32730, -29417, -21241, -9546, 3736, 16407, 26362, 31939, 32203, 27100, 17471, 4917, -8465, -20439, -28992, -32682, -30879, -23878, -12848, 355, 13507, 24382, 31134, 32610, 28550, 19634, 7372, -6153, -18637, -27945, -32478, -31453, -25033, -14313, -1126, 12261, 23544, 30773, 32690, 28956, 20207, 7949, -5694, -18357, -27833, -32467, -31441, -24924, -14047, -704, 12768, 24010, 31040, 32614, 28446, 19261, 6671, -7105, -19633, -28689, -32662, -30838, -23529, -12028, 1622, 14991, 25690, 31801, 32219, 26862, 16680, 3494, -10327, -22296, -30251, -32748, -29325, -20593, -8124, 5825, 18725, 28227, 32594, 31023, 23788, 12203, -1620, -15156, -25919, -31930, -32075, -26319, -15709, -2196, 11729, 23494, 30917, 32614, 28261, 18656, 5576, -8549, -21089, -29700, -32767, -29705, -21079, -8495, 5689, 18813, 28405, 32650, 30740, 23025, 10953, -3198, -16751, -27135, -32370, -31454, -24550, -12964, 1102, 14965, 25974, 32016, 31925, 25708, 14547, 583, -13500, -24990, -31662, -32220, -26546, -15727, -1853, 12387, 24229, 31363, 32392, 27105, 16524, 2707, -11646, -23726, -31160, -32479, -27415, -16953, -3146, 11289, 23503, 31078, 32506, 27494, 17027, 3172, -11320, -23567, -31126, -32478, -27346, -16744, -2784, 11740, 23917, 31298, 32390, 26963, 16099, 1982, -12541, -24538, -31574, -32216, -26322, -15080, -765, 13712, 25404, 31919, 31918, 25392, 13665, -868, -15232, -26474, -32280, -31444, -24128, -11832, 2913, 17065, 27693, 32587, 30725, 22481, 9560, -5357, -19167, -28986, -32757, -29683, -20396, -6831, 8173, 21469, 30260, 32688, 28231, 17819, 3639, -11316, -23886, -31403, -32265, -26279, -14705, 2, 14717, 26302, 32279, 31362, 23737, 11025, -4056, -18275, -28576, -32736, -29850, -20530, -6775, 8449, 21855, 30537, 32606, 27603, 16603, 1988, -13067, -25283, -31991, -31718, -24512, -11938, 3255, 17743, 28344, 32722, 29903, 20498, 6569, -8815, -22261, -30790, -32507, -27019, -15534, -593, 14486, 26350, 32348, 31134, 22967, 9662, -5811, -19991, -29695, -32738, -28425, -17716, -3012, 12378, 24982, 31950, 31695, 24264, 11331, -4178, -18747, -29061, -32767, -29008, -18633, -3998, 11557, 24476, 31796, 31830, 24559, 11642, -3958, -18656, -29061, -32766, -28906, -18361, -3562, 12069, 24910, 31974, 31610, 23893, 10607, -5156, -19726, -29693, -32722, -28093, -16877, -1698, 13887, 26216, 32382, 30923, 22172, 8183, -7745, -21850, -30789, -32434, -26384, -14066, 1602, 16897, 28174, 32737, 29487, 19189, 4296, -11632, -24781, -31989, -31517, -23467, -9765, 6296, 20847, 30372, 32561, 26874, 14676, -1084, -16590, -28073, -32735, -29432, -18957, -3852, 12199, 25277, 32178, 31202, 22577, 8408, -7833, -22156, -31032, -32267, -25545, -12513, 3616, 18859, 29440, 32728, 27897, 16135, 356, -15518, -27534, -32690, -29689, -19268, -4022, 12236, 25437, 32262, 30989, 21926, 7341, -9099, -23253, -31545, -31872, -24139, -10293, 6167, 21072, 30633, 32414, 25950, 12875, -3487, -18968, -29611, -32689, -27403, -15095, 1088, 17000, 28552, 32767, 28548, 16971, 1012, -15216, -27519, -32709, -29431, -18523, -2804, 13649, 26563, 32570, 30096, 19774, 4286, -12327, -25726, -32396, -30582, -20746, -5459, 11268, 25040, 32225, 30921, 21459, 6326, -10485, -24530, -32085, -31139, -21929, -6891, 9985, 24213, 31996, 31253, 22167, 7156, -9775, -24098, -31971, -31273, -22180, -7124, 9855, 24189, 32011, 31202, 21968, 6793, -10225, -24484, -32112, -31032, -21525, -6163, 10882, 24972, 32261, 30750, 20841, 5229, -11819, -25639, -32435, -30334, -19899, -3989, 13025, 26460, 32605, 29753, 18681, 2439, -14485, -27405, -32730, -28973, -17165, -578, 16176, 28431, 32763, 27951, 15328, -1588, -18067, -29490, -32648, -26642, -13147, 4050, 20116, 30520, 32321, 24997, 10607, -6788, -22271, -31449, -31710, -22970, -7698, 9769, 24463, 32193, 30743, 20515, 4421, -12944, -26612, -32659, -29341, -17598, -793, 16247, 28617, 32747, 27432, 14196, -3147, -19588, -30366, -32348, -24950, -10305, 7340, 22858, 31729, 31358, 21841, 5945, -11695, -25922, -32568, -29674, -18076, -1168, 16090, 28624, 32739, 27209, 13654, -3936, -20372, -30790, -32100, -23902, -8612, 9238, 24353, 32234, 30525, 19723, 3037, -14562, -27820, -32767, -27910, -14694, 2932, 19687, 30536, 32211, 24196, 8891, -9099, -24352, -32259, -30420, -19378, -2465, 15202, 28266, 32754, 27291, 13526, -4361, -20928, -31120, -31816, -22793, -6797, 11286, 25920, 32612, 29299, 16986, -554, -17931, -29793, -32476, -25140, -10037, 8173, 23863, 32170, 30511, 19387, 2239, -15611, -28613, -32714, -26624, -12228, 5989, 22344, 31718, 31170, 20858, 4002, -14116, -27808, -32765, -27414, -13429, 4794, 21513, 31445, 31445, 21498, 4742, -13523, -27508, -32766, -27617, -13684, 4609, 21442, 31445, 31419, 21358, 4466, -13861, -27757, -32763, -27261, -13005, 5436, 22136, 31718, 31084, 20426, 3172, -15113, -28520, -32703, -26296, -11363, 7262, 23538, 32169, 30340, 18632, 849, -17218, -29673, -32438, -24598, -8704, 10046, 25511, 32611, 29005, 15864, -2500, -20049, -30998, -31727, -21983, -4973, 13689, 27827, 32754, 26824, 11990, -6830, -23388, -32172, -30248, -18243, -149, 18001, 30145, 32213, 23502, 6914, -11997, -26891, -32760, -27620, -13188, 5689, 22658, 31991, 30530, 18754, 633, -17709, -30061, -32227, -23461, -6725, 12302, 27153, 32767, 27218, 12386, -6677, -23469, -32243, -29989, -17464, 1050, 19211, 30785, 31785, 21855, 4399, -14578, -28537, -32655, -25497, -9524, 9749, 25655, 32677, 28370, 14214, -4883, -22292, -31950, -30485, -18395, 116, 18594, 30582, 31879, 22020, 4443, -14697, -28689, -32611, -25073, -8713, 10721, 26384, 32750, 27560, 12633, -6765, -23777, -32376, -29505, -16167, 2913, 20967, 31572, 30945, 19297, 766, -18044, -30420, -31930, -22022, -4227, 15087, 29001, 32514, 24352, 7435, -12162, -27389, -32753, -26309, -10368, 9323, 25651, 32707, 27922, 13018, -6612, -23850, -32431, -29224, -15383, 4064, 22038, 31980, 30252, 17471, -1702, -20260, -31405, -31043, -19293, -457, 18554, 30750, 31633, 20866, 2403, -16952, -30057, -32059, -22206, -4130, 15481, 29361, 32353, 23333, 5638, -14158, -28694, -32545, -24264, -6927, 13002, 28080, 32661, 25018, 8000, -12023, -27543, -32725, -25610, -8859, 11231, 27098, 32754, 26053, 9510, -10632, -26761, -32765, -26358, -9955, 10230, 26538, 32767, 26534, 10198, -10028, -26439, -32766, -26583, -10239, 10028, 26463, 32767, 26509, 10078, -10228, -26612, -32766, -26309, -9715, 10629, 26881, 32758, 25978, 9148, -11228, -27262, -32733, -25506, -8373, 12019, 27745, 32678, 24884, 7387, -12997, -28314, -32574, -24097, -6187, 14153, 28952, 32400, 23129, 4768, -15474, -29634, -32129, -21962, -3129, 16945, 30333, 31733, 20578, 1271, -18546, -31015, -31179, -18958, 802, 20251, 31644, 30432, 17085, -3084, -22029, -32176, -29456, -14944, 5559, 23841, 32564, 28213, 12523, -8207, -25643, -32755, -26667, -9818, 10999, 27380, 32696, 24782, 6830, -13896, -28993, -32328, -22531, -3571, 16847, 30414, 31592, 19889, 64, -19792, -31567, -30433, -16842, 3653, 22658, 32373, 28799, 13390, -7530, -25359, -32749, -26644, -9546, 11497, 27799, 32613, 23935, 5344, -15467, -29872, -31885, -20656, -838, 19335, 31467, 30494, 16808, -3894, -22978, -32467, -28383, -12419, 8750, 26259, 32761, 25513, 7546, -13598, -29030, -32245, -21875, -2278, 18286, 31136, 30832, 17488, -3261, -22636, -32423, -28459, -12413, 8911, 26454, 32748, 25100, 6755, -14477, -29538, -31990, -20771, -664, 19733, 31685, 30059, 15538, -5658, -24430, -32705, -26911, -9528, 11966, 28307, 32439, 22562, 2931, -17973, -31106, -30769, -17096, 3998, 23363, 32585, 27644, 10673, -10949, -27808, -32547, -23086, -3540, 17559, 30986, 30856, 17212, -3979, -23433, -32607, -27461, -10241, 11488, 28168, 32444, 22417, 2496, -18533, -31380, -30356, -15898, 5603, 24629, 32739, 26321, 8212, -13552, -29296, -32006, -20460, 209, 20792, 32100, 29071, 13045, -8822, -26747, -32693, -23984, -4511, 16994, 30870, 30869, 16977, -4563, -24055, -32705, -26598, -8477, 13480, 29348, 31938, 20062, -910, -21476, -32301, -28456, -11675, 10422, 27782, 32491, 22390, 2070, -19200, -31706, -29719, -14135, 7926, 26362, 32717, 24062, 4355, -17358, -31097, -30530, -15904, 6055, 25229, 32767, 25174, 5945, -16039, -30607, -31005, -17032, 4843, 24478, 32750, 25802, 6851, -15294, -30323, -31223, -17558, 4305, 24165, 32735, 25995, 7084, -15151, -30291, -31225, -17502, 4447, 24313, 32747, 25767, 6646, -15614, -30514, -31014, -16861, 5267, 24910, 32766, 25100, 5532, -16666, -30959, -30548, -15612, 6756, 25914, 32731, 23945, 3731, -18270, -31549, -29747, -13712, 8892, 27244, 32538, 22222, 1233, -20354, -32162, -28499, -11112, 11633, 28777, 32041, 19836, -1957, -22810, -32633, -26659, -7766, 14897, 30340, 31056, 16683, -5805, -25476, -32749, -24068, -3650, 18554, 31707, 29373, 12677, -10225, -28129, -32257, -20571, 1209, 22403, 32597, 26768, 7768, -15061, -30478, -30874, -16040, 6716, 26162, 32681, 23034, 1982, -20057, -32161, -28316, -10413, 12666, 29457, 31601, 18016, -4546, -24849, -32764, -24328, -3739, 18725, 31830, 29009, 11659, -11539, -28958, -31849, -18746, 3780, 24413, 32766, 24622, 4068, -18542, -31804, -29010, -11557, 11744, 29110, 31743, 18293, -4434, -24919, -32759, -23958, -2975, 19527, 32095, 28318, 10104, -13268, -29875, -31227, -16618, 6495, 26290, 32627, 22243, 449, -21581, -32535, -26773, -7240, 16023, 31043, 30071, 13593, -9902, -28292, -32070, -19268, 3506, 24471, 32765, 24076, 2894, -19795, -32209, -27885, -9055, 14492, 30499, 30614, 14763, -8796, -27769, -32236, -19847, 2930, 24181, 32767, 24175, 2897, -19907, -32260, -27656, -8502, 15133, 30803, 30241, 13728, -10035, -28505, -31915, -18447, 4786, 25493, 32695, 22563, 455, -21900, -32626, -26009, -5552, 17865, 31773, 28747, 10386, -13523, -30217, -30764, -14858, 9004, 28051, 32071, 18895, -4423, -25374, -32696, -22442, -110, 22286, 32683, 25465, 4509, -18886, -32088, -27950, -8695, 15270, 30974, 29897, 12609, -11525, -29409, -31319, -16203, 7731, 27464, 32242, 19447, -3958, -25210, -32699, -22320, 267, 22714, 32731, 24816, 3293, -20042, -32381, -26935, -6682, 17252, 31697, 28689, 9868, -14399, -30727, -30093, -12830, 11530, 29519, 31170, 15554, -8687, -28117, -31946, -18033, 5905, 26566, 32450, 20265, -3213, -24907, -32711, -22255, 635, 23177, 32761, 24011, 1811, -21409, -32631, -25545, -4114, 19633, 32350, 26870, 6263, -17877, -31948, -28003, -8254, 16162, 31452, 28961, 10083, -14508, -30887, -29761, -11752, 12931, 30277, 30420, 13263, -11445, -29643, -30956, -14620, 10061, 29006, 31386, 15828, -8787, -28380, -31725, -16892, 7631, 27782, 31988, 17819, -6597, -27225, -32188, -18615, 5689, 26720, 32338, 19285, -4912, -26276, -32446, -19836, 4265, 25902, 32522, 20272, -3752, -25603, -32574, -20597, 3373, 25385, 32607, 20813, -3128, -25250, -32624, -20925, 3018, 25201, 32628, 20932, -3043, -25238, -32619, -20834, 3202, 25362, 32596, 20632, -3497, -25569, -32557, -20321, 3925, 25858, 32497, 19901, -4487, -26222, -32410, -19365, 5182, 26657, 32288, 18711, -6008, -27154, -32122, -17932, 6962, 27705, 31900, 17023, -8041, -28299, -31610, -15977, 9241, 28921, 31240, 14789, -10557, -29559, -30773, -13452, 11979, 30194, 30193, 11962, -13500, -30808, -29484, -10314, 15107, 31380, 28629, 8506, -16786, -31887, -27609, -6537, 18518, 32303, 26407, 4408, -20285, -32601, -25007, -2126, 22061, 32753, 23393, -302, -23818, -32729, -21552, 2864, 25527, 32497, 19474, -5542, -27151, -32028, -17151, 8313, 28651, 31290, 14582, -11150, -29987, -30254, -11770, 14018, 31114, 28895, 8723, -16875, -31984, -27189, -5459, 19677, 32551, 25119, 2004, -22368, -32766, -22674, 1609, 24891, 32583, 19850, -5337, -27181, -31959, -16655, 9125, 29171, 30857, 13105, -12910, -30790, -29244, -9232, 16618, 31969, 27101, 5079, -20165, -32636, -24417, -704, 23461, 32730, 21199, -3817, -26410, -32193, -17467, 8396, 28911, 30978, 13263, -12931, -30864, -29056, -8648, 17303, 32172, 26414, 3705, -21387, -32747, -23060, 1459, 25046, 32511, 19030, -6721, -28144, -31406, -14387, 11933, 30544, 29398, 9221, -16933, -32119, -26480, -3658, 21544, 32757, 22680, -2150, -25587, -32369, -18062, 8022, 28880, 30894, 12731, -13753, -31253, -28310, -6835, 19121, 32553, 24642, 560, -23895, -32659, -19960, 5869, 27842, 31488, 14391, -12196, -30746, -29008, -8115, 18145, 32416, 25247, 1366, -23424, -32699, -20295, 5575, 27751, 31500, 14316, -12391, -30861, -28790, -7541, 18738, 32528, 24616, 268, -24265, -32585, -19113, 7151, 28634, 30938, 12503, -14322, -31541, -27585, -5093, 20828, 32744, 22625, -2733, -26255, -32081, -16266, 10531, 30223, 29496, 8824, -17821, -32410, -25050, -712, 24114, 32585, 18937, -7577, -28947, -30638, -11482, 15495, 31923, 26597, 3131, -22476, -32744, -20645, 5567, 27979, 31249, 13123, -13996, -31529, -27440, -4520, 21509};


}
