package edu.umich.cse.audioanalysis;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import edu.umich.cse.audioanalysis.Ultraphone.CalibrationControllerSetting;


public class AppSetting {
    @SerializedName("id")
    public String name;
    public String other;
    public int tagNextId;
    public boolean enableTagApplication; // control parameter to determine if application selected is performed
    public int predictShowMode;
    public boolean recordAllSensors; // control for comparing exisitng scheme only
    public boolean enableFalseSvm; // train location of "false locatons"
    public boolean enableAutoRetrain; // retrain classifer whenever prediction is wrong
    public boolean enableGeomagneticFilter; // filter out tags with different geomagnetic field
    public boolean showSensorDebugInfo;

    boolean modelIsLocked;
    String modelLockHolder;

    public List<CalibrationControllerSetting> calibrationControllerSettings;
    public String selectedCalibrationSettingName;

    public AppSetting() {
        tagNextId = 0;
        enableTagApplication = false;
        recordAllSensors = true;
        enableFalseSvm = true;
        enableAutoRetrain = true;
        enableGeomagneticFilter = true;
        showSensorDebugInfo = false;
        modelLockHolder = "NULL";
        modelIsLocked = false;

        calibrationControllerSettings = new ArrayList<>();
        selectedCalibrationSettingName = "NULL";
    }
}



