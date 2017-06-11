package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.Network.RemoteTriggerController;
import edu.umich.cse.audioanalysis.Network.RemoteTriggerControllerListener;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.Ultraphone.UltraphoneControllerListener;

public class ExpPressureCorrectActivity extends AppCompatActivity implements UltraphoneControllerListener, RemoteTriggerControllerListener {

    TextView txtDebugStatus;
    Button btnStartOrStopCorrect;


    boolean needToCorrect;

    UltraphoneController uc;
    RemoteTriggerController rtc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        needToCorrect = false;

        setContentView(R.layout.activity_exp_pressure_correct);

        txtDebugStatus = (TextView) findViewById(R.id.txtDebugStatus);

        btnStartOrStopCorrect = (Button) findViewById(R.id.btnStartOrStopCorrect);
        btnStartOrStopCorrect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOrStopCorrect();
            }
        });
    }

    void startOrStopCorrect(){
        if(!needToCorrect){ // need to start
            needToCorrect = true;
            btnStartOrStopCorrect.setText("Stop");

            // init ultraphone controller
            uc = new UltraphoneController(D.DETECT_PSE, this, getApplicationContext());
            uc.startEverything();

            // connect remote trigger
            if(C.TRIGGERED_BY_NETWORK) {
                rtc = new RemoteTriggerController(this);
                rtc.connectServer();
            }

        } else { // need to stop
            needToCorrect = false;
            btnStartOrStopCorrect.setText("Start");

            // stop all connection
            uc.stopEverything();
            if (rtc!=null && rtc.isConnected()){
                rtc.closeServerIfServerIsAlive();
            }

            // release objects
            uc = null;
            rtc = null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_exp_pressure_correct, menu);



        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//===================================================================================
// Ultraphone callbacks
//===================================================================================
    @Override
    public void pressureUpdate(final double pressure) {

    }

    @Override
    public void squeezeUpdate(double check) {
        // never use this callback here
    }

    // This callback is consistent for all caller
    @Override
    public void updateDebugStatus(final String stringToShow) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtDebugStatus.setText(stringToShow);
            }
        });
    }

    // This callback is consistent for all caller
    @Override
    public void showToast(final String stringToShow){
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), stringToShow, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void unexpectedEnd(int code, String reason){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                uc.stopEverything();
                finish();
            }
        });
    }

//===================================================================================
// Start of RemoteServer callbacks
//===================================================================================
    @Override
    public void remoteTriggerIsConnected(final boolean success, String resp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtDebugStatus.setText("remote trigger: "+success);
            }
        });
    }

    @Override
    public void remoteTriggerAskStart(int select) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View root = txtDebugStatus.getRootView();
                root.setBackgroundColor(Color.parseColor("#AAAA00"));
            }
        });
    }

    @Override
    public void remoteTriggerAskStop() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View root = txtDebugStatus.getRootView();
                root.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        });
    }

    @Override
    public void remoteTriggerAskSave(final String suffix) {

    }

    @Override
    public void remoteTriggerAskRevoke() {

    }

    @Override
    public void remoteTriggerChangeScaleTo(final int scale) {

    }

    @Override
    public void remoteTriggerChangeTarget(String target) {

    }
}
