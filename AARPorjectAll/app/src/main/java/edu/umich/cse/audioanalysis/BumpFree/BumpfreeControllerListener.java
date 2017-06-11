package edu.umich.cse.audioanalysis.BumpFree;

/**
 * Created by eddyxd on 11/20/15.
 */
public interface BumpfreeControllerListener {
    public void detectionUpdate(double value);
    public void updateDebugStatus(String stringToShow); // Note this function needs to be write run on UI thread
    public void showToast(String stringToShow); // Note this function needs to be write run on UI thread
}
