package edu.umich.cse.audioanalysis.Ultraphone;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.umich.cse.audioanalysis.AppSetting;
import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.MyApp;
import edu.umich.cse.audioanalysis.Ultraphone.Graphic.TouchCalibrationCircleView;

/**
 * Created by eddyxd on 5/19/16.
 * 2016/05/19: This is used
 */
public class CalibrationController {
    double TARGET_VALUE_TOLRANCE = 0.05; // this is the tolerance range
    double TARGET_VALUE_DEFAULT = 0.5; // target value to train

    final static int CALIB_MODE_SHORTEST_POINT = 0;
    final static int CALIB_MODE_AVERAGE_ALL = 1;

    static int CALIB_MODE = CALIB_MODE_AVERAGE_ALL;

    public static final String SETTING_NAME_DEFAULT = "calibSetting.json";

    // init the setting
    public double targetValue = TARGET_VALUE_DEFAULT;
    public double targetValueRangeMin = TARGET_VALUE_DEFAULT - TARGET_VALUE_TOLRANCE;
    public double targetValueRangeMax = TARGET_VALUE_DEFAULT + TARGET_VALUE_TOLRANCE;

    public List<TouchCalibrationCircleView> calibPointList;

    private Context context;
    private MyApp myApp;

    public boolean calibrationIsReady = false;

    // constructor for using/loading the activity
    public CalibrationController(String settingName, Context context, MyApp myApp) {
        this.context = context;
        this.myApp = myApp;
        // load the primary calibration setting
        String settingNameToLoad = null;
        if(settingName == null || settingName.equals("")){
            settingNameToLoad = myApp.appSetting.selectedCalibrationSettingName;
        } else {
            settingNameToLoad = settingName;
        }
        loadSetting(settingNameToLoad);
    }

    // constructor for building the activity
    public CalibrationController(List calibPointListIn, Context context, MyApp myApp) {
        this.context = context;
        this.myApp = myApp;

        calibPointList = calibPointListIn;
        calibrationIsReady = true;
    }

    // overloadd constructors
    public CalibrationController(List calibPointListIn, Activity activity){
        this(calibPointListIn, (Context) activity, (MyApp) activity.getApplicationContext());
    }
    public CalibrationController(String settingName, Activity activity) {
        this(settingName, (Context) activity, (MyApp) activity.getApplicationContext());
    }

    public void updateTargetValue(double targetValueIn){
        targetValue = targetValueIn;
        targetValueRangeMin = targetValue - TARGET_VALUE_TOLRANCE;
        targetValueRangeMax = targetValue + TARGET_VALUE_TOLRANCE;
    }


    public void updateEstForceAndExtForce(double estForce, double extForce, int pointIdx){
        if(inTargetRange(extForce)) {
            if(estForce > 0.03) { // there is a hard threshold for the estForce to avoid outliner
                double ratio = extForce / estForce;
                calibPointList.get(pointIdx).addCalibRatio(ratio);
            }
        }
    }

    public boolean inTargetRange(double value){
        return value > targetValueRangeMin && value < targetValueRangeMax;
    }

    // function to retrive calibration result
    // 2016/06/23: update to estiamte calibration weight for being averaged latter on
    int calibPointIdxLocked = -1;
    public void lockTouchLocation(int x, int y){
        if(calibPointIdxLocked >= 0){
            Log.e(C.LOG_TAG, "[ERROR]: lockTouchLocation is called when calibPointIdxLocked >= 0 (forget to unlock?)");
        }

        // find the point with the shortest distance
        int minDistIdx = -1;
        int minDistSqure = -1;
        for(int i=0;i<calibPointList.size();i++){
            TouchCalibrationCircleView c = calibPointList.get(i);
            c.distSquare = (c.start.x-x)*(c.start.x-x) + (c.start.y-y)*(c.start.y-y);
            if(minDistIdx == -1 || c.distSquare < minDistSqure){
                minDistIdx = i;
                minDistSqure = c.distSquare;
            }
        }
        calibPointIdxLocked = minDistIdx;
        calibPointList.get(calibPointIdxLocked).pressed = true;
        calibPointList.get(calibPointIdxLocked).invalidate();

        // update calib weight
        int MIN_DIS_TO_FIX_ONE_POINT = 10;
        if(minDistSqure < MIN_DIS_TO_FIX_ONE_POINT){ // too close to the point -> just use one point as the single reference
            for(int i=0;i<calibPointList.size();i++){
                if(i==minDistIdx) calibPointList.get(i).calibWeight = 1.0;
                else calibPointList.get(i).calibWeight = 0.0;
            }
        } else { // estimate weight based on the distance to the touch point
            double weightSum = 0.0;
            for(int i=0;i<calibPointList.size();i++){
                TouchCalibrationCircleView c = calibPointList.get(i);
                c.calibWeight = 1.0/((double)c.distSquare);
                weightSum += c.calibWeight;
            }
            // ensure the sum of all weight is zero
            for(int i=0;i<calibPointList.size();i++){
                TouchCalibrationCircleView c = calibPointList.get(i);
                c.calibWeight /= weightSum;
            }
        }
    }

    public double getLockedCalibResult(double estForce){
        if(calibPointIdxLocked>=0) {
            if(CALIB_MODE == CALIB_MODE_SHORTEST_POINT) {
                return calibPointList.get(calibPointIdxLocked).calib(estForce);
            } else if(CALIB_MODE == CALIB_MODE_AVERAGE_ALL){
                double result = 0.0;
                for(int i=0;i<calibPointList.size();i++) {
                    TouchCalibrationCircleView c = calibPointList.get(i);
                    result += c.calib(estForce)*c.calibWeight;
                }
                return result;
            } else {
                Log.e(C.LOG_TAG, "[ERROR]: undefined CALIB_MODE = "+CALIB_MODE);
                return 0;
            }
        } else {
            Log.w(C.LOG_TAG, "[WARN]: getLockedCalibResult is called when calibPointIdxLocked is not ready (forget to lock?)");
            return 0;
        }
    }

    public void unlockTouchLocation(){
        if(calibPointIdxLocked < 0){
            Log.e(C.LOG_TAG, "[ERROR]: unlockTouchLocation is called when calibPointIdxLocked < 0 (forget to lock?)");
        }

        calibPointList.get(calibPointIdxLocked).pressed = false;
        calibPointList.get(calibPointIdxLocked).invalidate();

        calibPointIdxLocked = -1;
    }

    public String currentSettingName = null;
    public void loadSetting(String settingName){
        if(!myApp.isCalibrationSettingExistedInAppSetting(settingName)){
            Toast.makeText(context, "[ERROR]: not existed setting name = "+settingName, Toast.LENGTH_LONG).show();
            return;
        }
        // else
        calibrationIsReady = true;

        currentSettingName = settingName;


        // load old setting
        int settingIdx = myApp.findCalibrationSettingIdxByName(settingName);
        CalibrationControllerSetting setting = myApp.appSetting.calibrationControllerSettings.get(settingIdx);

        // load setting
        this.updateTargetValue(setting.targetValue);
        calibPointList = new ArrayList<>();
        for(int i=0;i<setting.calibPointStarts.length;i++){
            TouchCalibrationCircleView c = new TouchCalibrationCircleView(context);
            c.updateStartPoint(setting.calibPointStarts[i]);
            c.addCalibRatio(setting.calibPointRatios[i]);
            // TODO: add ratio by other way to implement other calib method
            c.estimateAndUpdateCalibRatio();
            c.invalidate();
            calibPointList.add(c);
        }
    }

    public void saveSetting(String settingName){
        currentSettingName = settingName;

        CalibrationControllerSetting setting = new CalibrationControllerSetting(settingName);
        setting.targetValue = this.targetValue;

        setting.calibPointStarts = new Point[calibPointList.size()];
        setting.calibPointRatios = new double[calibPointList.size()];

        for(int i=0;i<calibPointList.size();i++){
            setting.calibPointStarts[i] = calibPointList.get(i).start;
            setting.calibPointRatios[i] = calibPointList.get(i).calibRatioUsed;
        }

        myApp.addCalibrationSettingToAppSetting(setting);

    }


    // This is the internal class created for waiting to save
    /*
    public class CalibrationControllerSetting{
        public String name;
        public double targetValue;
        Point calibPointStarts[];
        double calibPointRatios[];
        public CalibrationControllerSetting(String settingName){
            name = settingName;
        }
    }
    */


}
