package edu.umich.cse.audioanalysis.Network;

/**
 * Created by eddyxd on 10/15/15.
 * yctung: this is a request queue used for network communication
 */
public class NetworkRequest {
    public int action;
    public String name;
    public byte[] data;
    public int type;


    NetworkRequest(int actionIn, String nameIn, byte[] dataIn, int typeIn){
        action = actionIn;
        name = nameIn;
        data = dataIn;
        type = typeIn;
    }

}
