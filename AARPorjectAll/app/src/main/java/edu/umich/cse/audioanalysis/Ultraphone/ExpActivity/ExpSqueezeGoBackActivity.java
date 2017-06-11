package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

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

import java.util.Random;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.Network.RemoteTriggerController;
import edu.umich.cse.audioanalysis.Network.RemoteTriggerControllerListener;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.Ultraphone.UltraphoneControllerListener;

public class ExpSqueezeGoBackActivity extends AppCompatActivity implements UltraphoneControllerListener, RemoteTriggerControllerListener {

    TextView txtDebugStatus, txtInstruction;
    TextView txtPageCnt;
    int pageCnt = 50;
    Button btnGoBack;

    boolean isTriggerEnabled;
    boolean isTriggerAskedActionFinished;
    boolean needToUpdateUIWhenSqueezed;


    UltraphoneController uc;
    RemoteTriggerController rtc;

    int squeezeCnt = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exp_squeeze_go_back);

        txtDebugStatus = (TextView) findViewById(R.id.txtDebugStatus);
        txtInstruction = (TextView) findViewById(R.id.txtInstruction);

        txtPageCnt = (TextView) findViewById(R.id.txtPageCnt);
        txtPageCnt.setText(""+pageCnt);

        btnGoBack = (Button) findViewById(R.id.btnGoBack);

        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToPreviousPage();
            }
        });

        isTriggerEnabled = false;
        isTriggerAskedActionFinished = false;
        needToUpdateUIWhenSqueezed = true;

        // init ultraphone controller
        uc = new UltraphoneController(D.DETECT_SSE, this, getApplicationContext());

        // connect remote trigger
        if(C.TRIGGERED_BY_NETWORK) {
            rtc = new RemoteTriggerController(this);
        }

    }

    @Override
    protected void onResume (){
        super.onResume();
        uc.startCheckSqueezeWhenPossible(); // NOTE: this must be called before startSesning/startEverything
        uc.startEverything();
        if(rtc!=null){
            rtc.connectServer();
        }

    }

    @Override
    protected void onPause (){
        uc.stopCheckSqueeze();
        uc.stopEverything();
        if (rtc!=null && rtc.isConnected()){
            rtc.closeServerIfServerIsAlive();
        }
        super.onPause();
    }

    static char colors[] = {'5','6','7','8','9','A','B','C','D','E','F'};
    void goToPreviousPage() {
        pageCnt = pageCnt-1;
        txtPageCnt.setText(""+pageCnt);
        View root = txtPageCnt.getRootView();

        // get random color
        Random r = new Random();
        String colorString = "#";
        for(int i=0;i<6;i++){
            if (i%2 == 0){

                colorString = colorString + colors[r.nextInt(9)];
            } else {
                colorString = colorString + r.nextInt(9);
            }
        }


        //root.setBackgroundColor(Color.parseColor(colorString));
        root.setBackgroundColor(Color.parseColor("#AAAAAA"));


        // ensure view will not be popout multiple times in a short time
        needToUpdateUIWhenSqueezed = false; // stop update for a while
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                needToUpdateUIWhenSqueezed = true;
                View root = txtPageCnt.getRootView();
                root.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }, 1500); // ms
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_exp_squeeze_go_back, menu);
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
        // never use this callback here
    }

    @Override
    public void squeezeUpdate(double check) {
        if (needToUpdateUIWhenSqueezed && check == 3) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(rtc!=null && rtc.isConnected()){
                        // always return the "right" click -> determin it is tp or fp on the remote server
                        rtc.sendTriggerCheck(RemoteTriggerController.TRIGGER_CHECK_OK);

                        // update instruction UI
                        if (isTriggerEnabled && !isTriggerAskedActionFinished) {
                            isTriggerAskedActionFinished = true;
                            txtInstruction.setText("Success");
                            txtInstruction.setTextColor(Color.BLACK);
                        } else {
                            txtInstruction.setText("False");
                            txtInstruction.setTextColor(Color.BLACK);
                        }
                    }

                    goToPreviousPage();
                }
            });

        }
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
        isTriggerEnabled = true;
        isTriggerAskedActionFinished = false;

        // save traces
        uc.setTriggerLog(1, squeezeCnt, -1);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtInstruction.setText("Squeeze "+squeezeCnt+" Times!");
                txtInstruction.setTextColor(Color.RED);
            }
        });
    }

    @Override
    public void remoteTriggerAskStop() {
        isTriggerEnabled = false;
        uc.setTriggerLog(2, squeezeCnt, -1); // code 2 indicates stop

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtInstruction.setText("Action Canceled");
                txtInstruction.setTextColor(Color.GRAY);
            }
        });
    }

    @Override
    public void remoteTriggerAskSave(final String suffix) {
        // go back to the previous activity
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                uc.moveTraceFolderWhenSensingEnded(suffix);
                uc.stopEverything();
                finish();
            }
        });
    }

    @Override
    public void remoteTriggerAskRevoke() {
        // just remember to save this rovoke to local traces
        uc.setTriggerLog(3,-1,-1);  // code 3 indicates ends
    }

    @Override
    public void remoteTriggerChangeScaleTo(final int scale) {
        squeezeCnt = scale;
    }

    @Override
    public void remoteTriggerChangeTarget(String target) {

    }
}
