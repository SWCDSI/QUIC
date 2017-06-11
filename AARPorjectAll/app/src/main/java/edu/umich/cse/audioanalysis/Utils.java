package edu.umich.cse.audioanalysis;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by eddyxd on 4/25/16.
 */
public class Utils {

    // Get device information, ref: http://stackoverflow.com/questions/1995439/get-android-phone-model-programmatically
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String result;
        if (model.toUpperCase().startsWith(manufacturer.toUpperCase())) {
            //result = capitalize(model);
            result = model.toUpperCase();
        } else {
            //result = capitalize(manufacturer) + "-" + model;
            result = (manufacturer+"-"+model).toUpperCase();
        }
        return result.replace(" ", ""); // remove unsecure strings to ensure this can be sent via URL
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public static String[] doubleArrayToStringArray(double[] dataArray) {
        String [] resultArray = new String[dataArray.length];
        for(int i=0;i<dataArray.length;i++){
            resultArray[i] = String.format("%.1f", dataArray[i]);
        }
        return resultArray;
    }

    public static void showSimpleAlertDialog(final Activity activity, final String title,final String message){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

                // set title
                alertDialogBuilder.setTitle(title);

                // set dialog message
                alertDialogBuilder
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton("Got it",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // if this button is clicked, close
                                // current activity
                            }
                        });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        });
    }

    // read audio files from assets
    public static short[] readBinaryAudioDataFromAsset(Context context, String fileName){
        short [] data;
        try {
            InputStream is = context.getAssets().open(fileName);

            byte[] fileBytes=new byte[is.available()];
            int arraySize = is.available()/2; // short take 2 bytes
            data = new short[arraySize];
            is.read(fileBytes, 0, is.available()); // read all buffer into bytebuffer
            ByteBuffer.wrap(fileBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data);

            return data;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(C.LOG_TAG, "[ERROR]: unable to load asset = "+fileName);
        }

        return null;
    }
}
