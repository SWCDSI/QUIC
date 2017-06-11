package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.MyApp;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.Utils;

public class UltraphoneExpMainActivity extends AppCompatActivity {
    TextView txtDeviceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ultraphone_exp_main);



        txtDeviceInfo = (TextView) findViewById(R.id.txtDeviceInfo);



        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);
            } else {
                //do something

            }
        } else {
            //do something -> no need to get permission in run time
        }

        Button b;

        // ball moving activity
        b = (Button) findViewById(R.id.btnGoPressureMovingBall);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ExpPressureMovingBallActivity.class);
                startActivity(intent);
            }
        });

        // engine sound activity
        b = (Button) findViewById(R.id.btnGoPressureEngineSound);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ExpPressureEngineSoundActivity.class);
                startActivity(intent);
            }
        });

        // monitor activity
        b = (Button) findViewById(R.id.btnGoPressureMonitor);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ExpPressureMonitorActivity.class);
                startActivity(intent);
            }
        });

        // touch calibration
        b = (Button) findViewById(R.id.btnGoPressureCalibration);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ExpTouchCalibrationByExtSensorActivity.class);
                startActivity(intent);
            }
        });

        // realtime test
        b = (Button) findViewById(R.id.btnGoRealtime);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ExpRealtimeRecordActivity.class);
                startActivity(intent);
            }
        });

        // service for Samsung
        b = (Button) findViewById(R.id.btnGoService);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ExpServiceActivity.class);
                startActivity(intent);
            }
        });


        // hand trainer activity
        b = (Button) findViewById(R.id.btnGoPressureHandTrainer);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ExpPressureHandTrainerActivity.class);
                startActivity(intent);
            }
        });
        if(C.DISABLE_SQUEEZE_APPS){
            b.setVisibility(View.GONE);
        }

        // hard pressed button
        b = (Button) findViewById(R.id.btnGoPressureButton);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ExpPressureButtonActivity.class);
                startActivity(intent);
            }
        });


        // go back by squeeze activity
        b = (Button) findViewById(R.id.btnGoSqueezeGoBack);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ExpSqueezeGoBackActivity.class);
                startActivity(intent);
            }
        });
        if(C.DISABLE_SQUEEZE_APPS){
            b.setVisibility(View.GONE);
        }

        // squeeze monitor activity
        b = (Button) findViewById(R.id.btnGoSqueezeMonitor);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ExpSqueezeMonitorActivity.class);
                startActivity(intent);
            }
        });
        if(C.DISABLE_SQUEEZE_APPS){
            b.setVisibility(View.GONE);
        }

        TextView t;
        t = (TextView) findViewById(R.id.txtTitleSqueeze);
        if(C.DISABLE_SQUEEZE_APPS){
            t.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApp myApp = (MyApp)getApplication();
        txtDeviceInfo.setText("Device=" + D.modelName + "\nSetting=" + D.name + "\nCalibration="+myApp.getValidSelectedCalibrationSettingName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ultraphone_exp_main, menu);
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
}

