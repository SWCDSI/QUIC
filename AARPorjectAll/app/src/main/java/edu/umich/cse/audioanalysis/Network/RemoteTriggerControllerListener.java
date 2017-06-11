package edu.umich.cse.audioanalysis.Network;

/**
 * Created by eddyxd on 12/2/15.
 */
public interface RemoteTriggerControllerListener {
    void remoteTriggerIsConnected(boolean success, String resp);
    void remoteTriggerAskStart(int select);
    void remoteTriggerAskStop();
    void remoteTriggerAskSave(String suffix);
    void remoteTriggerAskRevoke();
    void remoteTriggerChangeScaleTo(int scale);
    void remoteTriggerChangeTarget(String target);
}
