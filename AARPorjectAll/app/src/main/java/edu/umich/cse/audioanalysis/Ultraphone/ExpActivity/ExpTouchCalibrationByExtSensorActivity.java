package edu.umich.cse.audioanalysis.Ultraphone.ExpActivity;

// 2016/05/15: make new interface based on usb readed data
// 2016/05/19: This is made for calibirate touch force on the fly (based on force read from Arduino)

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import edu.umich.cse.audioanalysis.C;
import edu.umich.cse.audioanalysis.D;
import edu.umich.cse.audioanalysis.MyApp;
import edu.umich.cse.audioanalysis.Ultraphone.CalibrationController;
import edu.umich.cse.audioanalysis.Ultraphone.Graphic.MonitorView;
import edu.umich.cse.audioanalysis.Ultraphone.Graphic.TouchCalibrationCircleView;
import edu.umich.cse.audioanalysis.Ultraphone.UltraphoneControllerListener;
import edu.umich.cse.audioanalysis.UsbRelated.ExtForceReader;
import edu.umich.cse.audioanalysis.UsbRelated.ExtForceReaderListener;


public class ExpTouchCalibrationByExtSensorActivity extends AppCompatActivity implements UltraphoneControllerListener, ExtForceReaderListener {
    final static boolean SHOW_CALIB_VALUE_UPDATE_DIALOG = true;

    // UI-related variables
    int screenWidth;
    int screenHeight;

    RelativeLayout layout;
    TextView txtDebugStatus, txtDebugCalibInfo;
    TextView textInfo, textInfo2, textInstruction, textCountDown;
    List<TouchCalibrationCircleView> calibPointList;

    // monitor view to plot result
    MonitorView monitorView;
    static final int LINE_CNT = 3;
    static final int LINE_IDX_ULTRAPHONE_ORI = 0;
    static final int LINE_IDX_EXT_FORCE = 1;
    static final int LINE_IDX_CALIBRATED_FORCE = 2;

    // Inputted train settings
    Point targetLocation;
    int[] trainForceValues;
    String traceSuffix;
    int currentTargetLocationIdx;
    boolean isTargetLocationTouched;

    // Ultraphone and other controllers
    UltraphoneController uc;
    ExtForceReader extForceReader;

    // Calibarated veraibles
    List<Double> latestEstForceList;
    List<Double> latestExtForceList;

    CalibrationController calibrationController;

    Button btnStartClibration; // no stop button to avoid accidently click the stop during the calibration

    boolean calibrationIsStarted = false;


    FileOutputStream extOutputStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isTargetLocationTouched = false;
        currentTargetLocationIdx = -1;

        latestEstForceList = new ArrayList<>();
        latestExtForceList = new ArrayList<>();

        // hide status bar
        // ref: http://developer.android.com/training/system-ui/status.html
        // ref: http://stackoverflow.com/questions/8273186/android-show-hide-status-bar-power-bar
        // Hide Status Bar
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = getWindow().getDecorView();
            // Hide Status Bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }

        // get screen resolution -> so we can adapt the size of background based on resolution
        // ref: http://stackoverflow.com/questions/1016896/get-screen-dimensions-in-pixels
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        // get calibration points (TODO: should be inputted by the caller)
        final int CIRCLE_WIDTH = 200;
        final int CALIB_X_CNT = 3;
        final int CALIB_Y_CNT = 4;
        final int MARGIN_X = 100;
        final int MARGIN_Y = 150; // set it bigger to avoid touch the back/menu button accidently during calibration
        int CALIB_X_OFFSET = (screenWidth - CIRCLE_WIDTH - MARGIN_X*2)/(CALIB_X_CNT-1);
        int CALIB_Y_OFFSET = (screenHeight - CIRCLE_WIDTH - MARGIN_Y*2)/(CALIB_Y_CNT-1);
        List<Point> TARGET_POINT_LIST = new ArrayList<Point>();
        Point buttonStartPoint = null;
        for(int y=0;y<CALIB_Y_CNT;y++){
            for(int x=0;x<CALIB_X_CNT;x++){
                Point p = new Point(MARGIN_X + CIRCLE_WIDTH/2 + x*CALIB_X_OFFSET , MARGIN_Y + CIRCLE_WIDTH/2 + y*CALIB_Y_OFFSET);
                TARGET_POINT_LIST.add(p);

                if(x==CALIB_X_CNT-1&&y==CALIB_Y_CNT-1){
                    // insert the button in the middle of last two calibration points
                    int xButton = (TARGET_POINT_LIST.get(TARGET_POINT_LIST.size()-1).x+TARGET_POINT_LIST.get(TARGET_POINT_LIST.size()-2).x)/2;
                    int yButton = (TARGET_POINT_LIST.get(TARGET_POINT_LIST.size()-1).y+TARGET_POINT_LIST.get(TARGET_POINT_LIST.size()-2).y)/2;
                    buttonStartPoint = new Point(xButton, yButton);
                }
            }
        }

        layout = new RelativeLayout(this);
        android.view.ViewGroup.LayoutParams layoutParams;
        layout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        layout.setBackgroundColor(Color.BLACK);

        // create txtDebugStatus
        txtDebugStatus = new TextView(this);
        txtDebugStatus.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 50));
        layout.addView(txtDebugStatus);

        // create monitor view
        monitorView = new MonitorView(this);
        monitorView.setLayoutParams(new RelativeLayout.LayoutParams(screenWidth, screenHeight/2));
        layout.addView(monitorView);
        monitorView.showLines(LINE_CNT,0,200,0,1);

        // create calibration test points
        calibPointList = new ArrayList<>();
        for(int i=0;i<TARGET_POINT_LIST.size();i++){
            TouchCalibrationCircleView c = new TouchCalibrationCircleView(this);
            c.updateStartPoint(TARGET_POINT_LIST.get(i));
            c.updateRadius(CIRCLE_WIDTH/2);

            layout.addView(c);
            calibPointList.add(c);
        }
        calibrationController = new CalibrationController(calibPointList, this);
        monitorView.drawBox(0,200,calibrationController.targetValueRangeMin,calibrationController.targetValueRangeMax);

        btnStartClibration = new Button(this);
        btnStartClibration.setText("Start Clibration\n(click back button to stop)");
        btnStartClibration.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(btnStartClibration);

        btnStartClibration.setTranslationX(0);
        btnStartClibration.setTranslationY((float)screenHeight/2-btnStartClibration.getHeight()/2);
        btnStartClibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionStartUltraphone();
            }
        });


        txtDebugCalibInfo = new TextView(this);
        txtDebugCalibInfo.setTextColor(Color.WHITE);
        txtDebugCalibInfo.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        txtDebugCalibInfo.setText("AAA\nBBB\nCCC");
        layout.addView(txtDebugCalibInfo);
        txtDebugCalibInfo.setTranslationX(0);
        txtDebugCalibInfo.setTranslationY((float)(screenHeight*(3.0/4.0))-btnStartClibration.getHeight()/2);


        // set layout to this dynamic generated layouts
        setContentView(layout);
        updateUI();

        // init ultraphone controller
        uc = new UltraphoneController(D.DETECT_PSE, this, getApplicationContext());


        extForceReader = new ExtForceReader(this, this);
    }

    void updateUI() {
        layout.setBackgroundColor(Color.BLACK);
        if(!isTargetLocationTouched || currentTargetLocationIdx < 0){ // no circle(calibrate point) is selected -> show all circles
            for(int i=0;i<calibPointList.size();i++) {
                calibPointList.get(i).setVisibility(View.VISIBLE);
            }
        } else { // some point is selected! -> hide the other point
            for(int i=0;i<calibPointList.size();i++) {
                if(i!=currentTargetLocationIdx) {
                    calibPointList.get(i).setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    void redrawCalibrateViews(){
        // remove the old list from view if need
        if(calibPointList!=null && calibPointList.size()>0) {
            for (int i = 0; i < calibPointList.size(); i++) {
                TouchCalibrationCircleView c = calibPointList.get(i);
                ((ViewGroup) c.getParent()).removeView(c);
            }
        }

        // load the new list from
        calibPointList = calibrationController.calibPointList;
        for(int i=0;i<calibPointList.size();i++){
            TouchCalibrationCircleView c = calibPointList.get(i);
            layout.addView(c);
        }
        layout.invalidate();
    }

    void actionTouchCalibPoint(int index){
        // update internal state
        isTargetLocationTouched = true;
        currentTargetLocationIdx = index;
        TouchCalibrationCircleView p = calibPointList.get(currentTargetLocationIdx);


        // ask ultraphone to estimate force if need
        if(uc!=null && calibrationIsStarted && isTargetLocationTouched) {
            uc.startCheckPressure(p.start);
        }

        // update UI
        p.pressed = true;
        p.invalidate();
        updateUI();
    }

    void actionReleaseTouchPoint(){
        boolean wasTargetLocationTouched = isTargetLocationTouched;
        isTargetLocationTouched = false;

        // ask point to adapt the calib value if need
        if(currentTargetLocationIdx>=0) {
            calibPointList.get(currentTargetLocationIdx).estimateAndUpdateCalibRatio();
        }

        for(int i=0;i<calibPointList.size();i++){
            TouchCalibrationCircleView p = calibPointList.get(i);
            p.pressed = false;
            p.invalidate();
        }

        // ask ultraphone to stop estimating force if need
        if(uc!=null && calibrationIsStarted && wasTargetLocationTouched) {
            uc.stopCheckPressure();
        }

        updateUI();
    }

    void actionInitLatestCalibData(){
        latestEstForceList.clear();
        latestExtForceList.clear();

        // set background to white for knowning the latest array is inited
        layout.setBackgroundColor(Color.WHITE);
    }

    private void actionStartUltraphone(){
        calibrationIsStarted = true;
        uc.startEverything();
        btnStartClibration.setEnabled(false);

        if(C.TRACE_SAVE_TO_FILE) {
            try {
                extOutputStream = new FileOutputStream(new File(C.appFolderPath+C.DEBUG_FOLDER+"ext.dat"), true);
            } catch (FileNotFoundException e) {
                Log.e(C.LOG_TAG, "ERROR: can't open sensor file to write");
                e.printStackTrace();
            }
        }
    }

    private void actionStopUltraphone(){
        calibrationIsStarted = false;
        uc.stopEverything();
        btnStartClibration.setEnabled(true);

        if(extOutputStream!=null){
            try {
                extOutputStream.close();
            } catch (IOException e){
                Log.e(C.LOG_TAG, "ERROR: can't close ext file to write");
                e.printStackTrace();
            }
            extOutputStream = null;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event){
        super.dispatchTouchEvent(event);

        int x = (int)event.getX();
        int y = (int)event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                // check if the hit inside any rect
                // ref: http://stackoverflow.com/questions/11370721/how-do-i-detect-if-a-touch-event-has-landed-within-an-edittext
                // NOTE: this doesn't work -> it seems I hack the way to add view
                Log.d(C.LOG_TAG, "Prssed: ("+x+", "+y+")");
                for(int i=0;i<calibPointList.size();i++){
                    TouchCalibrationCircleView p = calibPointList.get(i);
                    if(p.contain(x,y)){ // find the touch point
                        Log.d(C.LOG_TAG, "Found match: ("+p.start.x+", "+p.start.y+")");
                        actionTouchCalibPoint(i);
                        break;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if(SHOW_CALIB_VALUE_UPDATE_DIALOG){
                    if(!calibrationIsStarted && isTargetLocationTouched){
                        showUpdateCalibrateValueDialog(currentTargetLocationIdx);
                    }

                }

                actionReleaseTouchPoint();

                break;
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if ( keyCode == KeyEvent.KEYCODE_BACK ) {
            // perform your desired action here
            Log.d(C.LOG_TAG, "Back Menu is pressed");

            showCalibrationDialog();


            // return 'true' to prevent further propagation of the key event
            return true;
        }

        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume (){
        super.onResume();
        //uc.startEverything();
        extForceReader.startUsbService();
    }

    @Override
    protected void onPause (){
        //uc.stopEverything();
        extForceReader.stopUsbService();

        super.onPause();
    }

//==================================================================================================
//                                  Callbacks
//==================================================================================================
    @Override
    public void pressureUpdate(final double pressure) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(isTargetLocationTouched) { // only run it when target point is clicked
                    double extForce = currentExtForce;
                    //latestEstForceList.add(new Double(pressure));
                    //latestExtForceList.add(new Double(extForce));

                    calibrationController.updateEstForceAndExtForce(pressure, extForce, currentTargetLocationIdx);
                    monitorView.addPoint(LINE_IDX_ULTRAPHONE_ORI, pressure);
                    monitorView.addPoint(LINE_IDX_EXT_FORCE, extForce);
                    double calibPressure = calibPointList.get(currentTargetLocationIdx).calib(pressure);
                    monitorView.addPoint(LINE_IDX_CALIBRATED_FORCE, calibPressure);

                    txtDebugCalibInfo.setText(String.format(" ori force: %.2f\n ext force: %.2f\n calib force: %.2f", pressure, extForce, calibPressure));

                    monitorView.invalidate();
                }
            }
        });
    }

    @Override
    public void squeezeUpdate(double check) {

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
        /*
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                uc.stopEverything();
                finish();
            }
        });
        */
    }

    @Override
    public void updateExtForce(String dataString) {

    }

    double currentExtForce = 0;
    @Override
    public void updateExtForce(int val0, int val1) {
        currentExtForce = ((double)val1)/300.0;

        // output to trace file if need
        if(C.TRACE_SAVE_TO_FILE && extOutputStream!=null){
            ByteBuffer bStamp = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            bStamp.putInt((int)uc.currentAudioTotalRecordedSampleCnt);
            bStamp.putInt(val1);

            try {
                extOutputStream.write(bStamp.array());
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

//==================================================================================================
//                                  Some other utility
//==================================================================================================
    void showSaveTraceAlert(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save the trace?");
        builder.setMessage(String.format("with the suffix = %s", traceSuffix));
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO: popout this activity
            }
        });
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO: add save function
            }
        });

        builder.create().show();
    }

    void showCalibrationDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Calibration Action");
        builder.setItems(new CharSequence[]
                        {"Load Calibration", "Save Calibration", "Delete Calibration", "Select Calibration", "Stop Calibration", "Go Back"},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        ExpTouchCalibrationByExtSensorActivity ref = ExpTouchCalibrationByExtSensorActivity.this;
                        switch (which) {
                            case 0: // load setting
                                ref.showSavedCalibrationDialogWithAction(SHOW_DIALOG_ACTION_LOAD);
                                break;
                            case 1: // save calib setting
                                ref.showSaveClibrationDialog();
                                break;
                            case 2: // delete calib setting
                                ref.showSavedCalibrationDialogWithAction(SHOW_DIALOG_ACTION_DELETE);
                                break;
                            case 3:
                                ref.showSavedCalibrationDialogWithAction(SHOW_DIALOG_ACTION_SELECT);
                                break;
                            case 4: // just stop calibration sound
                                ref.actionStopUltraphone();
                                break;
                            case 5: // end the activity
                                ref.uc.stopEverything();
                                ref.finish();
                                break;
                        }
                    }
                });
        Dialog dialog = builder.create();
        dialog.show();
    }

    //ref: http://stackoverflow.com/questions/10903754/input-text-dialog-android
    private String newCalibrationNameToSave = "";
    void showSaveClibrationDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please input the setting name");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if(calibrationController.currentSettingName!=null){
            input.setText(calibrationController.currentSettingName);
        }
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                newCalibrationNameToSave = input.getText().toString();
                if(newCalibrationNameToSave==""){
                    newCalibrationNameToSave = "No Name";
                }
                calibrationController.saveSetting(newCalibrationNameToSave);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    static final int SHOW_DIALOG_ACTION_DELETE = 1;
    static final int SHOW_DIALOG_ACTION_LOAD = 2;
    static final int SHOW_DIALOG_ACTION_SELECT = 3; // select used calibration setting
    void showSavedCalibrationDialogWithAction(final int action){
        String actionButtonText = "";
        if(action == SHOW_DIALOG_ACTION_DELETE){
            actionButtonText = "Delete";
        } else if(action == SHOW_DIALOG_ACTION_LOAD) {
            actionButtonText = "Load";
        } else if(action == SHOW_DIALOG_ACTION_SELECT){
            actionButtonText = "Select";
        } else{
            actionButtonText = "Undefined(Error)";
        }

        // build the string array
        final MyApp myApp = (MyApp)getApplication();
        CharSequence list[] = new CharSequence[myApp.appSetting.calibrationControllerSettings.size()];
        for(int i=0;i<myApp.appSetting.calibrationControllerSettings.size();i++){
            list[i] = myApp.appSetting.calibrationControllerSettings.get(i).name;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if(list.length>0) {
            builder.setSingleChoiceItems(list, 0, null);
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.setPositiveButton(actionButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selectedPosition < 0) {
                        Toast.makeText(ExpTouchCalibrationByExtSensorActivity.this, "You select nothing, action aborted", Toast.LENGTH_LONG).show();
                    } else { // make proper actions
                        String selectedSettingName = myApp.appSetting.calibrationControllerSettings.get(selectedPosition).name;
                        if (action == SHOW_DIALOG_ACTION_LOAD) {
                            calibrationController.loadSetting(selectedSettingName);
                            redrawCalibrateViews();
                        } else if (action == SHOW_DIALOG_ACTION_DELETE) {
                            myApp.deleteCalibrationSettingIfExisted(selectedSettingName);
                        } else if (action == SHOW_DIALOG_ACTION_SELECT) {
                            myApp.appSetting.selectedCalibrationSettingName = selectedSettingName;
                            myApp.updateAppSettingToFile();
                        }
                    }
                }
            });
            builder.show();
        } else { // nothing to select
            Toast.makeText(ExpTouchCalibrationByExtSensorActivity.this, "No calibration existed", Toast.LENGTH_LONG).show();

        }
    }


    void showUpdateCalibrateValueDialog(final int index){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please input the setting name");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String valueString = input.getText().toString();
                double value = Double.parseDouble(valueString);
                Log.d(C.LOG_TAG, "updated value = "+value+" from String = "+valueString);
                calibPointList.get(index).clearCalibRatioAndUpdateRatio(value);
                calibPointList.get(index).estimateAndUpdateCalibRatio();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    void showLatestDataDialog(){
        Log.d(C.LOG_TAG, "showLatestDataDialog");

        // print this string to debug window for matlab parsing
        String debugExtForceString = "ext = [";
        String debugEstForceString = "est = [";

        for(int i=0;i<latestEstForceList.size();i++){
            debugExtForceString += String.format("%.3f;", latestExtForceList.get(i));
            debugEstForceString += String.format("%.3f;", latestEstForceList.get(i));
        }
        debugExtForceString += "];";
        debugEstForceString += "];";

        Log.w(C.LOG_TAG,debugEstForceString+debugExtForceString);

        Log.d(C.LOG_TAG, "end of print debug message");
    }

}
