package edu.umich.cse.audioanalysis.Ultraphone;

import android.graphics.Point;

/**
 * Created by eddyxd on 7/31/16.
 */
public class CalibrationControllerSetting {
    public String name;
    public double targetValue;
    Point calibPointStarts[];
    double calibPointRatios[];
    public CalibrationControllerSetting(String settingName){
        name = settingName;
    }
}
