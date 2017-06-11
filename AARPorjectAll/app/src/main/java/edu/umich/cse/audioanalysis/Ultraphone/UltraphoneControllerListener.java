package edu.umich.cse.audioanalysis.Ultraphone;

/**
 * Created by eddyxd on 11/19/15.
 */
public interface UltraphoneControllerListener {
    public void pressureUpdate(double pressure);
    public void squeezeUpdate(double check);
    public void updateDebugStatus(String stringToShow); // Note this function needs to be write run on UI thread
    public void showToast(String stringToShow);
    public void unexpectedEnd(int code, String reason);
}
