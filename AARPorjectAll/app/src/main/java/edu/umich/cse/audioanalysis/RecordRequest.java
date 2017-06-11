package edu.umich.cse.audioanalysis;

import java.io.ByteArrayOutputStream;

//import org.opencv.core.Mat;

public class RecordRequest {
	public int recordIdx;
	public String filePath;
	public String createTime;
	public String saveTime;
	public ByteArrayOutputStream byteStreamToSave;
	
	public RecordRequest(int recordIdxIn, String filePathIn, String timeNow) {
		// TODO Auto-generated constructor stub
		recordIdx = recordIdxIn;
		filePath = filePathIn;
		createTime = timeNow;
	}
	
	public String dump(){
		return "recordIdx = "+recordIdx+", time ="+createTime+",filePath = "+filePath+", size of buffer"+byteStreamToSave.size();
	}
}
