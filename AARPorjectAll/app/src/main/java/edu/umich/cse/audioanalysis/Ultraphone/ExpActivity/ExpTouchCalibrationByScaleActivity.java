package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import edu.umich.cse.audioanalysis.R;

public class ExpTouchCalibrationByScaleActivity extends AppCompatActivity {

    Button btnTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exp_touch_calibration_by_scale);

        btnTest = (Button) findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionTest();
            }
        });
    }


    void actionTest(){
        Intent intent = new Intent(getApplicationContext(), ExpTouchCalibrationByExtSensorActivity.class);
        Bundle b = new Bundle();

        int x = 200;
        int y = 400;
        String suffix = "_hhaa";
        int[] forceValues = new int[]{100, 200, 300};

        b.putInt("targetX", x);
        b.putInt("targetY", y);
        b.putString("traceSuffix", suffix);
        b.putIntArray("trainForceValues", forceValues);
        intent.putExtras(b);

        startActivity(intent);
    }
}
