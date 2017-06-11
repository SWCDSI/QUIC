package edu.umich.cse.audioanalysis;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by eddyxd on 4/26/16.
 * yctung: created based on iPhone's prototype to detect if people move hardly
 */
public class BigMoveDetector {
    double inAccMetricSumTemp; // store the internal state for optimization
    double inGyroMetricSumTemp;

    // internal state to delay the state change for a while after it is over the threshold
    boolean inAccMetricWasOver;
    boolean inGyroMetricWasOver;

    LinkedList<Double> inAccMags, inGyroMags;
    LinkedList<Integer> inStamps;
    double inAccMetric,inGyroMetric,inAccMax,inGyroMax;

    public BigMoveDetectorListener caller;

    public void setCaller(BigMoveDetectorListener callerIn) {
        caller = callerIn;
    }

    public BigMoveDetector(){
        inAccMags = new LinkedList<>();
        inGyroMags = new LinkedList<>();
        inStamps = new LinkedList<>();
        inAccMetricSumTemp = 0;
        inAccMetric = 0;
        inGyroMetricSumTemp = 0;
        inGyroMetric = 0;
        inAccMax = 0;
        inGyroMax = 0;
        inAccMetricWasOver = false;
        inGyroMetricWasOver = false;
        caller = null;
    }


    public int getStatus(){
            int statusCode = 0;
            if (inAccMetric > D.BIG_MOTION_DETECT_IN_ACC_METRIC_THRE || inAccMetricWasOver) {
                statusCode = 1; // just over threshold now
            }
            if (inAccMax > D.BIG_MOTION_DETECT_IN_ACC_MAX_THRE) {
                statusCode = 2;
            }
            if (inGyroMetric > D.BIG_MOTION_DETECT_IN_GYRO_METRIC_THRE || inGyroMetricWasOver) {
                statusCode = 3;
            }
            if (inGyroMax > D.BIG_MOTION_DETECT_IN_GYRO_MAX_THRE) {
                statusCode = 4;
            }

            if (caller != null) {
                caller.statusUpdate(statusCode);
            }
            return statusCode; // means normal
    }

    public int update(int stamp, double accMag ,double gyroMag){
        // 1. remove out-date data
        while (inStamps.size()>0) {
            int oldestStamp = inStamps.getFirst().intValue();
            if (stamp - oldestStamp < D.BIG_MOTION_DETECT_IN_RANGE_SAMPLE_SIZE) {
                break;
            } else { // find outdated object
                // update statistics
                inAccMetricSumTemp -= inAccMags.getFirst().doubleValue();
                inGyroMetricSumTemp -= inGyroMags.getFirst().doubleValue();
                // remove data
                inAccMags.pollFirst();
                inGyroMags.pollFirst();
                inStamps.pollFirst();
            }
        }

        // 2. add new value
        inAccMetricSumTemp += accMag;
        inGyroMetricSumTemp += gyroMag;

        inAccMags.add(accMag);
        inGyroMags.add(gyroMag);
        inStamps.add(stamp);


        // 3. update statistics (matlab algorithm?)
        inAccMetric = inAccMetricSumTemp/(inAccMags.size());
        inGyroMetric = inGyroMetricSumTemp/(inGyroMags.size());

        // 4. update max values
        if (accMag > inAccMax) {
            inAccMax = accMag;
            if (accMag > D.BIG_MOTION_DETECT_IN_ACC_MAX_THRE) {
                resetAccMaxIfNeedWithDelay(D.BIG_MOTION_DETECT_MAX_RESET_DELAY);
            }
        }

        if (gyroMag > inGyroMax) {
            inGyroMax = gyroMag;
            if (gyroMag > D.BIG_MOTION_DETECT_IN_GYRO_MAX_THRE) {
                resetGyroMaxIfNeedWithDelay(D.BIG_MOTION_DETECT_MAX_RESET_DELAY);
            }
        }

        if (!inAccMetricWasOver && inAccMetric > D.BIG_MOTION_DETECT_IN_ACC_METRIC_THRE) {
            inAccMetricWasOver = true;
            resetAccMetricWasOverWithDelay(D.BIG_MOTION_DETECT_MAX_RESET_DELAY);
        }

        if (!inGyroMetricWasOver && inGyroMetric > D.BIG_MOTION_DETECT_IN_GYRO_METRIC_THRE) {
            inGyroMetricWasOver = true;
            resetGyroMetricWasOverWithDelay(D.BIG_MOTION_DETECT_MAX_RESET_DELAY);
        }

        //Log.d(C.LOG_TAG, String.format("inAcc/gyro = (%.2f, %.2f), [inAccMags count] = %d", inAccMetricSumTemp, inGyroMetricSumTemp,inAccMags.size()));
        return getStatus();
    }

    void resetAccMetricWasOverWithDelay(int delay){
        inAccMetricWasOver = false;
    }

    void resetGyroMetricWasOverWithDelay(int delay){
        inGyroMetricWasOver = false;
    }

    void resetAccMaxIfNeedWithDelay(int delay){
        inAccMax = 0;
    }

    void resetGyroMaxIfNeedWithDelay(int delay){
        inGyroMax = 0;
    }

}
