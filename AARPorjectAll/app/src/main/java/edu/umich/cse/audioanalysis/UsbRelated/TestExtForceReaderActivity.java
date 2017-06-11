package edu.umich.cse.audioanalysis.UsbRelated;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.R;
import edu.umich.cse.audioanalysis.Ultraphone.Graphic.MonitorView;

public class TestExtForceReaderActivity extends AppCompatActivity implements ExtForceReaderListener {

    ExtForceReader extForceReader;
    TextView textData;
    MonitorView monitorView;
    Button buttonFuckAndroid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_ext_force_reader);
        textData = (TextView) findViewById(R.id.textData);
        monitorView = (MonitorView) findViewById(R.id.viewMonitorFuckAndroidDraw);
        buttonFuckAndroid = (Button) findViewById(R.id.buttonFuckAndroid);
        buttonFuckAndroid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // *** jsut for debug ***
                for(int i=0;i<1000;i++) {
                    monitorView.addPoint(0, 0.5);
                    monitorView.invalidate();
                }
            }
        });

        extForceReader = new ExtForceReader(this, this);
        monitorView.showLines(1,0,200,0,500);

    }

    @Override
    protected void onResume() {
        super.onResume();
        extForceReader.startUsbService();


    }

    @Override
    protected void onPause() {
        super.onPause();
        extForceReader.stopUsbService();
    }

    @Override
    public void updateExtForce(final String dataString) {
        //Log.d(C.LOG_TAG, dataString);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textData.setText("Data: "+dataString);
            }
        });
    }

    @Override
    public void updateExtForce(final int val0, final int val1) {
        Log.d(C.LOG_TAG, "updateExtForce: "+val0+","+val1);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textData.setText("Data: ("+val0+","+val1+")");

                monitorView.addPoint(0,(double) val1);
                monitorView.invalidate();
                monitorView.postInvalidate();

            }
        });
    }
}
