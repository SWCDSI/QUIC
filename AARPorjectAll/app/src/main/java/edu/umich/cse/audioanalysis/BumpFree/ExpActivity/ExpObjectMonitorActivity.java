package edu.umich.cse.audioanalysis.BumpFree.ExpActivity;

import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import edu.umich.cse.audioanalysis.BumpFree.BumpfreeController;
import edu.umich.cse.audioanalysis.BumpFree.BumpfreeControllerListener;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.Ultraphone.Graphic.MonitorView;

public class ExpObjectMonitorActivity extends AppCompatActivity implements BumpfreeControllerListener{

    MonitorView monitorView;
    Button btnStartOrStopBumpfree;
    Button btnStartOrStopDetect;
    TextView txtDebugStatus, txtDetectResult;

    boolean needToBumpfree; // means start sound sync and sending data to network
    boolean needToDetect; // means start report the detection results

    BumpfreeController bc;

    boolean isDetectResultTriggered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exp_object_monitor);

        txtDebugStatus = (TextView) findViewById(R.id.txtDebugStatus);

        monitorView = (MonitorView) findViewById(R.id.viewMonitor);
        monitorView.showLines(1, 0, 200, 0, 1); // TODO: set to proper level


        btnStartOrStopBumpfree = (Button) findViewById(R.id.btnStartOrStopBumpfree);
        btnStartOrStopBumpfree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionStartOrStopBumpfree();
            }
        });

        btnStartOrStopDetect = (Button) findViewById(R.id.btnStartOrStopDetect);
        btnStartOrStopDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionStartOrStopDetect();
            }
        });

        txtDetectResult = (TextView) findViewById(R.id.txtDetectResult);

        needToBumpfree = false;
        needToDetect = false;
        isDetectResultTriggered = false;

        bc = new BumpfreeController(this, getApplicationContext());
    }

    public void updateDetectResultIfNeed(double value){
        if(!isDetectResultTriggered && value > 0.25){
            isDetectResultTriggered = true;
            txtDetectResult.setText("WARN!!!");
            txtDetectResult.setTextColor(Color.RED);


            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    resetDetectResult();
                }
            }, 4000);

        }
    }

    public void resetDetectResult(){
        txtDetectResult.setText("No thing");
        txtDetectResult.setTextColor(Color.BLACK);
        isDetectResultTriggered = false;
    }


    public void actionStartOrStopBumpfree(){
        if (!needToBumpfree) { // need to start bumpfree
            needToBumpfree = true;
            btnStartOrStopBumpfree.setText("Stop Bumpfree");
            bc.startEverything();
        } else { // need to stop bumpfree
            needToBumpfree = false;
            btnStartOrStopBumpfree.setText("Start Bumpfree");
            // TODO: need to clean everything after callback from bumpfree
            bc.stopEverything();
        }
    }

    // this function trigger the real "detection" algorithm at BumpFree
    // NOTE this function can only be called when bumpfree has aloready started
    public void actionStartOrStopDetect(){
        if (!needToDetect) {
            needToDetect = true;
            btnStartOrStopDetect.setText("Stop Detect");
            bc.startDetect();
        } else {
            needToDetect = false;
            btnStartOrStopDetect.setText("Start Detect");
            bc.stopDetect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_exp_object_monitor, menu);
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
// BumpFree callbacks
//===================================================================================
    @Override
    public void detectionUpdate(final double value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateDetectResultIfNeed(value);
                monitorView.addPoint(0, value);
                monitorView.invalidate();
            }
        });
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

}
