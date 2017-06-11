package edu.umich.cse.audioanalysis.UsbRelated;

/**
 * Created by eddyxd on 5/14/16.
 */
public interface ExtForceReaderListener {
    void updateExtForce(String dataString);
    void updateExtForce(int val0, int val1);
}
