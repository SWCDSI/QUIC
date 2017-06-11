package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.Ultraphone.UltraphoneControllerListener;

public class ExpBatteryActivity extends AppCompatActivity implements UltraphoneControllerListener {
    Button btnStartPressure;
    Button btnStartUltraphone;

    TextView txtDebugStatus;
    double debugScale;

    UltraphoneController uc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exp_battery);

        txtDebugStatus = (TextView) findViewById(R.id.txtDebugStatus);


        debugScale = 0;


        btnStartUltraphone = (Button) findViewById(R.id.btnStartUltraphone);

        btnStartUltraphone.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        actionstartUltraphone();
                    }
                }
        );

        btnStartPressure = (Button) findViewById(R.id.btnStartPressure);
        btnStartPressure.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch ( event.getAction() ) {
                    case MotionEvent.ACTION_DOWN:
                        uc.startCheckPressure(new Point(0,0));
                        break;
                    case MotionEvent.ACTION_UP:
                        uc.stopCheckPressure();
                        break;
                }
                return false;
            }
        });
    }

    void actionstartUltraphone(){
        // init ultraphone controller
        uc = new UltraphoneController(D.DETECT_PSE, this, getApplicationContext());
        uc.startEverything();
    }

    @Override
    protected void onResume (){
        super.onResume();
        if(uc!=null) {
            uc.startEverything();
            uc = null;
        }
    }

    @Override
    protected void onPause (){
        super.onPause();
        //uc.stopEverything();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_exp_pressure_moving_ball, menu);
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

    // TODO: make response when ultraphone ends due to errors
    public void unexpectedEnd(int code, String reason){

    }
}
