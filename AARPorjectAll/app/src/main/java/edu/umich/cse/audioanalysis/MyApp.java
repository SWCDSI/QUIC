package edu.umich.cse.audioanalysis;

/*
 *  2015/02/05: a common class to assign global parameters
 *  update this from EchoTag project
 * */

import android.app.Application;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import edu.umich.cse.audioanalysis.AudioRelated.AudioConfigManager;
import edu.umich.cse.audioanalysis.Ultraphone.CalibrationControllerSetting;


public class MyApp extends Application {
    public AppSetting appSetting;

    //public MyApp() {
    //}

    public void onCreate () {
        super.onCreate();
        // 1. check static variable setting is valid
        String settingErrorMessage = C.isValidSetting();
        if(settingErrorMessage != null){
            Toast.makeText(getApplicationContext(),settingErrorMessage, Toast.LENGTH_LONG).show();
        }

        // 2. init global varialbes
        UpdateGlobalVariables();

        // 3. load exisitng appSetting or create a new one (if necessary)
        CreateNewAppSettingIfNeed();

        // 4. copy data from asset if necessary
        Log.w(C.LOG_TAG, "[WARN]: copyAssetsDataIfNeed is turned off for debug purpose -> remember to turn it back and put the file back to asset");
        //copyAssetsDataIfNeed();
    }


    // initialize the global variables need to be done in run time
    private void UpdateGlobalVariables(){
        C.systemPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        C.appFolderName = "AudioAna";
        C.appFolderPath = C.systemPath + C.appFolderName + "/";

        // update models based on device setting
        D.initBasedOnModelName(Utils.getDeviceName());

        CreateAppFolderIfNeed();
        AudioConfigManager.init();
    }

    private void CreateAppFolderIfNeed(){
        // 1. create app folder if necessary
        File folder = new File(C.appFolderPath);
        // create necessary folders
        if (!folder.exists()) {
            folder.mkdir();
            Log.d(C.LOG_TAG, "Appfolder is not existed, create one");
        } else {
            Log.w(C.LOG_TAG, "ERROR: Appfolder has not been deleted");
        }

        // 2. create input data folder if necessary
        folder = new File(C.systemPath+C.INPUT_FOLDER);
        if (!folder.exists()) {
            folder.mkdir();
            Log.d(C.LOG_TAG, "Input folder is not existed, create one");
        }
    }


    private void copyAssetsDataIfNeed(){
        // TODO: make name in asset folder consistent to matalb source folder
        //String assetsToCopy[] = {C.INPUT_PREFIX+AudioConfigManager.inputConfigChirpResponse +".wav", C.INPUT_PREFIX+AudioConfigManager.inputConfigChirpSync+".wav"};
        //String targetPath[] = {C.systemPath+C.INPUT_FOLDER+C.INPUT_PREFIX+AudioConfigManager.inputConfigChirpResponse +".wav", C.systemPath+C.INPUT_FOLDER+C.INPUT_PREFIX+AudioConfigManager.inputConfigChirpSync+".wav"};

        // copy all the audio files
        String assetsToCopy[] = new String[AudioConfigManager.audioConfigs.size()];
        String targetPath[] = new String[AudioConfigManager.audioConfigs.size()];
        for(int i=0;i<AudioConfigManager.audioConfigs.size();i++){
            String fileNameNow = AudioConfigManager.audioConfigs.get(i).sourceName;
            assetsToCopy[i] = C.INPUT_PREFIX+fileNameNow+".wav";
            targetPath[i] = C.systemPath+C.INPUT_FOLDER+C.INPUT_PREFIX+fileNameNow+".wav";
        }


        for(int i=0; i<assetsToCopy.length; i++){
            String from = assetsToCopy[i];
            String to = targetPath[i];

            // 1. check if file exist
            File file = new File(to);
            if(file.exists()){
                Log.d(C.LOG_TAG, "copyAssetsDataIfNeed: file exist, no need to copy:" + from);
            } else {
                // do copy
                Log.e(C.LOG_TAG, "[ERROR] you need to put the file back to asset (temporarly removed for debug efficiency)");
                boolean copyResult = copyAsset(getAssets(), from, to);
                Log.d(C.LOG_TAG, "copyAssetsDataIfNeed: copy result = " + copyResult + " of file = " + from);
            }

        }

    }

    private void CreateNewAppSettingIfNeed() {
        String jsonSettingPath = C.appFolderPath+C.SETTING_JSON_FILE_NAME;
        // check if json file exist
        File folder = new File(jsonSettingPath);

        if (!folder.exists()) {
            // need to create new AppSetting
            appSetting = new AppSetting();
            appSetting.name = "YuChih";
            appSetting.other = "kerker";

            // wirte it to files
            updateAppSettingToFile();
        } else {
            // load old setting
            LoadOldAppSetting();
        }

    }

    // overwrite exisitng AppSetting to json file
    public void updateAppSettingToFile(){
        if (appSetting == null) {
            Log.e(C.LOG_TAG, "UpdateAppSettingToFile: appSetting = null");
        } else {
            // save exisitng AppSetting to storage
            Gson gson = new Gson();
            String json = gson.toJson(appSetting);
            try {
                FileWriter writer = new FileWriter(C.appFolderPath+C.SETTING_JSON_FILE_NAME);
                writer.write(json);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // load jason file back to object
    private void LoadOldAppSetting() {
        // clear original AppSetting
        appSetting = null;

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(C.appFolderPath+C.SETTING_JSON_FILE_NAME));
            //convert the json string back to object
            Gson gson = new Gson();
            appSetting = gson.fromJson(br, AppSetting.class);

            Log.d(C.LOG_TAG, "LoadOldAppSetting: name = " + appSetting.name);

        } catch (FileNotFoundException e) {
            Log.e(C.LOG_TAG, "Fail to open old json files");
            e.printStackTrace();
        }
    }

    public void addCalibrationSettingToAppSetting(CalibrationControllerSetting calibrationControllerSetting){
        if(isCalibrationSettingExistedInAppSetting(calibrationControllerSetting.name)){
            Log.w(C.LOG_TAG, "calibrationControllerSetting name = "+calibrationControllerSetting.name+" already existed -> overwrite the old setting");
            int existedIdx = findCalibrationSettingIdxByName(calibrationControllerSetting.name);
            appSetting.calibrationControllerSettings.set(existedIdx, calibrationControllerSetting);
            updateAppSettingToFile();
        } else {
            // add the new setting
            appSetting.calibrationControllerSettings.add(calibrationControllerSetting);
            updateAppSettingToFile();
        }
    }

    public int findCalibrationSettingIdxByName(String settingName){
        for(int i=0;i<appSetting.calibrationControllerSettings.size();i++){
            if(appSetting.calibrationControllerSettings.get(i).name.equals(settingName)){
                return i;
            }
        }
        return -1;
    }

    public void deleteCalibrationSettingIfExisted(String settingName){
        int settingIdx = findCalibrationSettingIdxByName(settingName);
        appSetting.calibrationControllerSettings.remove(settingIdx);
        updateAppSettingToFile();
    }

    // this will return speical string if the calibration is deleted
    public String getValidSelectedCalibrationSettingName(){
        if(isCalibrationSettingExistedInAppSetting(appSetting.selectedCalibrationSettingName)){
            return  appSetting.selectedCalibrationSettingName;
        } else {
            return "NULL";
        }
    }

    public boolean isCalibrationSettingExistedInAppSetting(String settingName){
        return findCalibrationSettingIdxByName(settingName)>=0;
    }

    public void DeleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                DeleteRecursive(child);

        fileOrDirectory.delete();
    }

    public static void CopyRecursive(File src, File dest){
        if(src.isDirectory()){
            //if directory not exists, create it
            if(!dest.exists()){
                dest.mkdir();
            }

            //list all the directory contents
            String files[] = src.list();

            for (String file : files) {
                //construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                //recursive copy
                CopyRecursive(srcFile,destFile);
            }

        } else {
            //if file, then copy it
            //Use bytes stream to support all file types
            try {
                InputStream in = new FileInputStream(src);

                OutputStream out = new FileOutputStream(dest);

                byte[] buffer = new byte[1024];
                int length;
                //copy the file content in bytes
                while ((length = in.read(buffer)) > 0){
                    out.write(buffer, 0, length);
                }
                in.close();
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void resetAll(){
        // remove old setting
        File folderToDelete = new File(C.appFolderPath);
        if(!folderToDelete.exists()){
            Log.e(C.LOG_TAG, "resetAll: folder has been deleted, path = " + C.appFolderPath);
        } else {
            DeleteRecursive(folderToDelete);
        }

        // create new setting
        CreateAppFolderIfNeed();
        CreateNewAppSettingIfNeed();
    }

    private boolean copyAssetFolder(AssetManager assetManager, String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            new File(toPath).mkdirs();
            boolean res = true;
            for (String file : files)
                if (file.contains("."))
                    res &= copyAsset(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
                else
                    res &= copyAssetFolder(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            Log.e(C.LOG_TAG, "[ERROR]: copyAsset: unable to copy file = " + fromAssetPath);
            return false;
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

}

