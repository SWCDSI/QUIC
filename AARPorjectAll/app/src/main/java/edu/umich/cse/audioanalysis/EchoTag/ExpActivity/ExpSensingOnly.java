package edu.umich.cse.audioanalysis.EchoTag.ExpActivity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.EchoTag.EchoTagController;
import edu.umich.cse.audioanalysis.EchoTag.EchoTagControllerListener;
import edu.umich.cse.audioanalysis.R;

// yctung: this is the activity only used for debugging -> mostly use to send environment data to remote server
public class ExpSensingOnly extends AppCompatActivity implements EchoTagControllerListener{

    TextView txtDebugStatus;
    EchoTagController etc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exp_sensing_only);

        txtDebugStatus = (TextView) findViewById(R.id.txtDebugStatus);

        etc = new EchoTagController(D.DETECT_LOC, this, this);
    }


    public void actionStartSensing(View view){
        etc.startEverything();
    }

    public void actionStopSensing(View view){
        etc.stopEverything();
    }

    // end of survey
    void actionEnd(){
        if(etc!=null) {
            etc.stopEverything();
            etc = null; // release the previous used controller
        }
        finish();
    }

//===================================================================================
// Sensing callbacks
//===================================================================================
    @Override
    public void predictUpdate(double pressure) {

    }

    @Override
    public void updateDebugStatus(final String stringToShow) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtDebugStatus.setText(stringToShow);
            }
        });
    }

    @Override
    public void showToast(final String stringToShow) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), stringToShow, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void unexpectedEnd(int code, String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                actionEnd();
            }
        });
    }
}
