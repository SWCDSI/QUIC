package edu.umich.cse.audioanalysis.Network;

/**
 * Created by eddyxd on 10/15/15.
 * yctung: a callback function for activty using NetworkingController
 */
public interface NetworkControllerListener {
    void isConnected(boolean success, String resp);
    int consumeReceivedData(double dataReceived);
}
