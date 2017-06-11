package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.Ultraphone.ForcePhoneService;

public class ExpServiceActivity extends AppCompatActivity {
    TextView txtServiceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exp_service);
        txtServiceInfo = (TextView)findViewById(R.id.txtServiceInfo);
    }


    public void onClickBtnStartService(View v) {
        Intent intent = new Intent(this, ForcePhoneService.class);
        startService(intent);

        if(isMyServiceRunning(ForcePhoneService.class)){
            txtServiceInfo.setText("Server starts successfully");
            //terminate();
        } else {
            txtServiceInfo.setText("Server starts failed");
        }
    }

    public void onClickBtnStopService(View v) {
        Intent intent = new Intent(this, ForcePhoneService.class);
        stopService(intent);
        if(isMyServiceRunning(ForcePhoneService.class)){
            txtServiceInfo.setText("Server stops failed");
        } else {
            txtServiceInfo.setText("Server stops successfully");
        }
    }

    public void onClickBtnCheckService(View v) {
        Log.v(C.LOG_TAG, "onClickBtnCheckService(View v)");
        boolean serviceIsRunning = isMyServiceRunning(ForcePhoneService.class);
        txtServiceInfo.setText("Service is running = " + serviceIsRunning);
    }

    // return if the service is run or not
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            String serviceCheck = service.service.getClassName();
            String serviceTarget = serviceClass.getName();
            //Log.d(C.LOG_TAG, "("+serviceTarget+","+serviceCheck+")");
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
