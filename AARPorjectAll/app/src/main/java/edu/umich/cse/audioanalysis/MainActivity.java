package edu.umich.cse.audioanalysis;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

import edu.umich.cse.audioanalysis.AudioRelated.AudioConfigManager;

public class MainActivity extends AppCompatActivity implements SurveyEndListener{

    Button btnCheckChirpResponse;
    Button btnCheckChirpSync;
    TextView txtCurrentSetting;
    TextView txtResult;
    SpectrumSurvey ss;

    // here is the test parameter setting
    int RECORDER_SAMPLERATE = 48000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCheckChirpResponse = (Button)findViewById(R.id.btnCheckChirpResp);
        btnCheckChirpResponse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CheckChirpResponse();
            }
        });

        btnCheckChirpSync = (Button)findViewById(R.id.btnCheckChirpSync);
        btnCheckChirpSync.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CheckChirpSync();
            }
        });

        txtCurrentSetting = (TextView)findViewById(R.id.txtCurrentSetting);
        txtResult = (TextView)findViewById(R.id.txtResult);

        txtCurrentSetting.setText("FS = "+RECORDER_SAMPLERATE+", VOL = "+C.DEFAULT_VOL);
    }

    // -------------------------------- button functions -------------------------------------------
    void CheckChirpResponse(){
        ss = new SpectrumSurvey(RECORDER_SAMPLERATE, AudioConfigManager.inputConfigChirpResponse, this);
        boolean initSuccess = ss.initSurvey(this, C.SURVEY_MODE_TRAIN); // reuse the train as chirps mode
        if(initSuccess==false){
            Toast.makeText(this, "Please wait the previos sensing ends", Toast.LENGTH_LONG);
        } else {
            txtResult.setText("Wait survey ends");
            ss.startSurvey();
        }
    }

    void CheckChirpSync(){
        ss = new SpectrumSurvey(RECORDER_SAMPLERATE, AudioConfigManager.inputConfigChirpSync, this);
        boolean initSuccess = ss.initSurvey(this, C.SURVEY_MODE_TRAIN); // reuse the train as chirps mode
        if(initSuccess==false){
            Toast.makeText(this, "Please wait the previos sensing ends", Toast.LENGTH_LONG);
        } else {
            txtResult.setText("Wait survey ends");
            ss.startSurvey();
        }
    }

    public void onSurveyEnd() {
        txtResult.setText("Survey completes!");
    }

    @Override
    public void audioRecorded(byte[] data, long audioTotalRecordedSampleCnt) {

    }


    // ------------------------------- other UI functions ------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
