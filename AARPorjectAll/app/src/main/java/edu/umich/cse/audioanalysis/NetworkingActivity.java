package edu.umich.cse.audioanalysis;

import android.content.Context;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

import edu.umich.cse.audioanalysis.Network.NetworkController;
import edu.umich.cse.audioanalysis.Network.NetworkControllerListener;

/*
* 2015/10/15: this activity ship the recorded signal to networking
* */

public class NetworkingActivity extends AppCompatActivity implements NetworkControllerListener, SurveyEndListener, MySensorControllerListener{
    TextView txtInfo;
    EditText editTextIp;
    EditText editTextPort;
    Button btnSelectVol;
    Button btnSelectSound;
    Button btnStartSurvey;
    Button btnSqueeze;
    Button btnPressure;
    boolean isSurvying;
    Button btnNetworkConnect;
    boolean isConnecting;
    Button btnLog;

    NetworkController nc;

    // audio related setting
    int RECORDER_SAMPLERATE = 48000;
    float volSelected = C.DEFAULT_VOL;
    String soundNameSelected = "default";
    //String soundSettingSelected = "48000rate-50repeat-4800period+chirp-18000Hz-24000Hz-1200samples+chwin600+pilotchirprepeat";
    String soundSettingSelected = "48000rate-5000repeat-2400period+chirp-18000Hz-24000Hz-1200samples+namereduced"; // 4min, 20Hz version
    SpectrumSurvey ss;
    MySensorController msc;
    LogController lc;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_networking);

        // init necessary variables
        isConnecting = false;
        isSurvying = false;
        nc = new NetworkController(this);


        // connect UI
        txtInfo = (TextView) findViewById(R.id.txtInfo);
        editTextIp = (EditText) findViewById(R.id.editTextIp);
        editTextIp.setText(nc.serverIp);
        /*
        editTextIp.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    Log.d(C.LOG_TAG, "edit editTextIp is done, string = " + editTextIp.getText());
                    nc.serverIp = editTextIp.getText().toString();
                }
                return false;
            }
        });
        editTextIp.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    Log.d(C.LOG_TAG, "edit editTextIp is done, string = " + editTextIp.getText());
                    nc.serverIp = editTextIp.getText().toString();
                }
            }
        });
        */
        editTextPort = (EditText) findViewById(R.id.editTextPort);
        editTextPort.setText("" + nc.serverPort);
        /*
        editTextPort.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    Log.d(C.LOG_TAG, "edit editTextPort is done, string = " + editTextPort.getText());
                    nc.serverPort = Integer.parseInt(editTextPort.getText().toString());
                }
            }
        });
        editTextPort.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    Log.d(C.LOG_TAG, "edit editTextPort is done, string = " + editTextPort.getText());
                    nc.serverPort = Integer.parseInt(editTextPort.getText().toString());
                }
                return false;
            }
        });*/

        btnNetworkConnect = (Button) findViewById(R.id.btnNetowrkConnect);
        btnNetworkConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btnNetworkConnectIsClicked();
            }
        });

        btnStartSurvey = (Button) findViewById(R.id.btnStartSurvey);
        btnStartSurvey.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btnStartSurveyIsClicked();
            }
        });

        btnLog = (Button) findViewById(R.id.btnLog);
        btnLog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Logged", Toast.LENGTH_SHORT).show();
            }
        });

        btnSqueeze = (Button) findViewById(R.id.btnSqueeze);

        btnPressure = (Button) findViewById(R.id.btnPressure);
        btnPressure.setOnTouchListener(new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        txtInfo.setText("on");
                        //lc.save(0, LogController.TAG_PRESSURE_SENSITIVE_ENABLE, 1);
                        break;
                    case MotionEvent.ACTION_UP:
                        txtInfo.setText("off");
                        //lc.save(1, LogController.TAG_PRESSURE_SENSITIVE_ENABLE, 1);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        TextView txtTitle = (TextView)findViewById(R.id.txtTitle);
        txtTitle.setText(ip);

        // connect sensor controller
        msc = new MySensorController(this, (SensorManager) getSystemService(Context.SENSOR_SERVICE), C.DEBUG_FOLDER, 1000);

        // create dummy log controller -> NOTE this logcontroller can't save trace since there is no folder is created yet
        lc = new LogController(C.appFolderPath+C.DEBUG_FOLDER, nc);
    }

//===================================================================================
// Networking callback functions
//===================================================================================
    public void isConnected(boolean success, final String resp) {
        if(success){
            // send init set actions
            nc.sendSetAction(NetworkController.SET_TYPE_STRING, "matlabSourceMatName", ("source_"+soundSettingSelected).getBytes());
            nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, "traceChannelCnt", "2".getBytes());
            nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, "traceVol", "0.5".getBytes());
            nc.sendInitAction();
            //nc.sendSetAction(NetworkController.SET_TYPE_INT, "traceChannelCnt", ByteBuffer.allocate(4).putInt(2).array());
            //nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING,"vol","0.5".getBytes());

            // update UI
            runOnUiThread(new Runnable() {
                public void run() {
                    txtInfo.setText("Connect successfully");
                    isConnecting = true;
                    btnNetworkConnect.setText("Disonnect"); // update UI for next click event
                    btnNetworkConnect.setEnabled(true);
                }
            });

        } else {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "[ERROR]: unable to connect server : "+resp, Toast.LENGTH_LONG).show();
                    btnNetworkConnect.setText("Connect"); // update UI for next click event
                    btnNetworkConnect.setEnabled(true);
                }
            });
        }
    }

    @Override
    public int consumeReceivedData(double dataReceived) {
        return 0;
    }


    //===================================================================================
// Survey end callback
//===================================================================================
    public void onSurveyEnd() {
        Log.d(C.LOG_TAG, "onSurveyEnd");
        isSurvying = false;
        btnStartSurvey.setText("Start Survey"); // update UI for next click event
        //msc.stopRecord();
    }

    //public void audioRecorded(ByteArrayOutputStream data) {
    public void audioRecorded(byte[] data, long audioTotalRecordedSampleCnt) {
        if(isConnecting){
            //nc.sendDataRequest(data.toByteArray());
            nc.sendDataRequest(data);
        }
    }

//===================================================================================
// My button setting functions
//===================================================================================
    private void btnNetworkConnectIsClicked(){
        if(isConnecting){ // action to stop connecting
            nc.closeServerIfServerIsAlive();
            isConnecting = false;
            btnNetworkConnect.setText("Connect"); // update UI for next click event
        } else { // action to make connecting
            txtInfo.setText("Wait connection to server...");
            btnNetworkConnect.setText("Wait");
            nc.connectServer(editTextIp.getText().toString(), Integer.parseInt(editTextPort.getText().toString()));
        }
    }

    private void btnStartSurveyIsClicked(){
        if(isSurvying){ // action to stop survey
            isSurvying = false;
            btnStartSurvey.setText("Start Survey"); // update UI for next click event
            //msc.stopRecord();
        } else { // action to start survey
            ss = new SpectrumSurvey(RECORDER_SAMPLERATE, soundSettingSelected, this);
            boolean initSuccess = ss.initSurvey(this, C.SURVEY_MODE_TRAIN); // reuse the train as chirps mode
            if(initSuccess==false){
                Toast.makeText(this, "Please wait the previos sensing ends", Toast.LENGTH_LONG).show();
            } else {
                isSurvying = true;
                btnStartSurvey.setText("Stop Survey"); // update UI for next click event

                txtInfo.setText("Wait survey ends");
                ss.startSurvey();
                //msc.startRecord();
            }
        }
    }

    public void onTiltChanged(double tiltX, double tiltY, double tiltZ) {
        /*
        if(nc!=null && isConnecting) {
            nc.sendSetAction(NetworkController.SET_TYPE_VALUE_STRING, "sensorDataNow", ("[" + msc.getDataString() + "]").getBytes());
        }
        */
    }

    public void onRecordedEnd() {

    }


    //===================================================================================
// Some other callback functions
//===================================================================================
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_networking, menu);
        return true;
    }


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
