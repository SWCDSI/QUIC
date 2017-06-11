package edu.umich.cse.audioanalysis.EchoTag;

/**
 * Created by eddyxd on 8/1/16.
 */
public interface EchoTagControllerListener {
    public void predictUpdate(double pressure);
    public void updateDebugStatus(String stringToShow); // Note this function needs to be write run on UI thread
    public void showToast(String stringToShow);
    public void unexpectedEnd(int code, String reason);
}
