/*
 * 2014/02/06: file to contain audio file
 * */

package edu.umich.cse.audioanalysis.AudioRelated;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import edu.umich.cse.audioanalysis.C;



public class AudioSource {
	// some static setting
	public String config;
	
	public MediaPlayer mPlayer = null;
	
	private final static String AUDIO_FILE_END = ".wav";
	private final static String MAT_FILE_END = ".mat";
	
	public AudioSource(String configIn, float vol, MediaPlayer.OnCompletionListener completeListener) {
		config = configIn;
		
		// initialize data
		
		String audioFolderPath = C.systemPath+C.INPUT_FOLDER;
		String audioFilePath =  audioFolderPath + C.INPUT_PREFIX + config + AUDIO_FILE_END;
		String matFilePath = audioFolderPath + C.INPUT_PREFIX + config + MAT_FILE_END;
		
		
		
		// init audio players
		//String inputFileName = C.systemPath + C.INPUT_FOLDER + inputPrefix + config + ".wav"; // "/11025Hz-10waves.wav";
		Log.d(C.LOG_TAG, audioFilePath);
		boolean exists = (new File(audioFilePath)).exists();
		if (exists) {
			Log.d(C.LOG_TAG, "file is existed");
		} else {
			Log.e(C.LOG_TAG, "This audio source is not exist!");
		}
		try {
			mPlayer = new MediaPlayer();

			mPlayer.setDataSource(audioFilePath);
			//ref: http://stackoverflow.com/questions/2119060/android-getting-audio-to-play-through-earpiece
			if(C.FORCE_TO_USE_TOP_SPEAKER){
				mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
				//mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
			}
			mPlayer.prepare();
			mPlayer.setVolume(vol, vol);

			mPlayer.setOnCompletionListener(completeListener);
		} catch (IOException e) {
			Log.e(C.LOG_TAG, "fail to prepare");
			e.printStackTrace();
		}
	}
}

