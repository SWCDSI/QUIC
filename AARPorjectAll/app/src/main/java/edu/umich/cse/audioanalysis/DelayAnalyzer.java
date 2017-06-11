package edu.umich.cse.audioanalysis;

import android.util.Log;

import java.util.ArrayList;
/**
 * Created by eddyxd on 5/16/16.
 * This class help to remember the time of system delay
 */
public class DelayAnalyzer {
    String[] tags;
    long[] times;

    int currentIdx;

    // use pre-allocated size to avoid delay in this class
    public DelayAnalyzer(int allocatedSize){
        tags = new String[allocatedSize];
        times = new long[allocatedSize];
        currentIdx = 0;
    }

    public void reset(){
        currentIdx = 0;
    }

    // add tag
    public void addTag(String tag){
        if(currentIdx == tags.length){
            Log.e(C.LOG_TAG, "Out of space for DelayAnalyzer, (you need more space when you init it?)");
        } else {
            tags[currentIdx] = tag;
            times[currentIdx] = System.currentTimeMillis();
            currentIdx++;
        }
    }


    // output result for showing
    public String getResult(boolean usingHtmlFormat){
        String newline = "\n";
        if(usingHtmlFormat){
            newline = "<br>";
        }

        String result = "Result ("+currentIdx+"): "+newline;
        for(int i=0;i<currentIdx;i++){
            String tagTimeNow = tags[i]+":"+(times[i]-times[0]);
            result += (tagTimeNow+newline);
        }

        return result;
    }







}
