package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.Ultraphone.UltraphoneControllerListener;

public class ExpPressureHandTrainerActivity extends AppCompatActivity  implements UltraphoneControllerListener {

    Button btnMoveTrainer;
    TextView txtDebugStatus;
    double debugScale;

    UltraphoneController uc;

    ImageView imageTrainer1, imageTrainer2, imageTrainer3;

    double maxMove;

    float xOri;

    boolean needToMoveTrainer;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exp_pressure_hand_trainer);

        txtDebugStatus = (TextView) findViewById(R.id.txtDebugStatus);

        debugScale = 0;

        float density = getResources().getDisplayMetrics().density;
        maxMove = 125;

        imageTrainer1 = (ImageView) findViewById(R.id.imageTrainer1);
        imageTrainer2 = (ImageView) findViewById(R.id.imageTrainer2);
        imageTrainer3 = (ImageView) findViewById(R.id.imageTrainer3);

        imageTrainer1.setPadding(0,0,0,0);
        imageTrainer2.setPadding(0,0,0,0);
        imageTrainer3.setPadding(0, 0, 0, 0);

        imageTrainer1.setAdjustViewBounds(false);
        imageTrainer2.setAdjustViewBounds(false);
        imageTrainer3.setAdjustViewBounds(false);

        imageTrainer1.setCropToPadding(true);
        imageTrainer2.setCropToPadding(true);
        imageTrainer3.setCropToPadding(true);

        xOri = imageTrainer1.getTranslationX();

        //imageTrainer1.setScaleType(ImageView.ScaleType.CENTER);

        needToMoveTrainer = false;

        btnMoveTrainer = (Button) findViewById(R.id.btnMoveTrainer);
        btnMoveTrainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveTrainerByScale(1.0);
            }
        });
        /*
        btnMoveBall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugScale = debugScale + 0.1;
                ballView.moveBallByScale(debugScale);
            }
        });*/
        /*
        btnMoveBall.setOnTouchListener(new View.OnTouchListener() {
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
        });*/


        // init ultraphone controller
        uc = new UltraphoneController(D.DETECT_PSE, this, getApplicationContext());
    }

    public void moveTrainerByScale(double scale){
        double scaleToMove = Math.min(scale, 1.0);
        float move = (float)(maxMove*scaleToMove);

        imageTrainer1.setTranslationX(xOri+move*2);
        imageTrainer2.setTranslationX(xOri+move);
    }

    @Override
    protected void onResume (){
        super.onResume();
        //uc.startEverything();
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
        /*
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ballMovingView.moveBallByScale(pressure);
            }
        });
        */
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
}
