package edu.umich.cse.audioanalysis;

import java.io.ByteArrayOutputStream;

public interface SurveyEndListener {
	public void onSurveyEnd();
	//public void audioRecorded(ByteArrayOutputStream data);
	public void audioRecorded(byte[] data, long audioTotalRecordedSampleCnt);
}
