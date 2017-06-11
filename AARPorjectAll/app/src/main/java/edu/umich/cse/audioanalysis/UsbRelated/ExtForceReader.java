package edu.umich.cse.audioanalysis.UsbRelated;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Set;

/**
 * Created by eddyxd on 5/14/16.
 * This is the reader to get external force estiamted by Arduino
 */
public class ExtForceReader {
    private UsbService usbService;
    private TextView display;
    private EditText editText;
    private MyHandler mHandler;
    private String resultToDispaly;
    private Activity activity;
    private ExtForceReaderListener caller;

    public ExtForceReader(Activity activityIn, ExtForceReaderListener callerIn){
        this.activity = activityIn;
        this.caller = callerIn;
        mHandler = new MyHandler(this);

        resultToDispaly = new String();
    }



    // Notifications from UsbService will be received here.
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    public void startUsbService() {
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    public void stopUsbService() {
        activity.unregisterReceiver(mUsbReceiver);
        activity.unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(activity, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            activity.startService(startService);
        }
        Intent bindingIntent = new Intent(activity, service);
        activity.bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        activity.registerReceiver(mUsbReceiver, filter);
    }

    // This handler will be passed to UsbService. Data received from serial port is displayed through this handler
    private static class MyHandler extends Handler {
        private final WeakReference<ExtForceReader> mActivity;

        public MyHandler(ExtForceReader activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    //mActivity.get().display.append(data);
                    //mActivity.get().resultToDispaly = mActivity.get().resultToDispaly+data.replace("\n","");
                    //mActivity.get().display.setText(mActivity.get().resultToDispaly);
                    //mActivity.get().caller.updateExtForce(data);
                    mActivity.get().parseUsbData(data);
                    break;
                case UsbService.CTS_CHANGE:
                    //Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    //Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }


    // This function parses the data format sent by usb
    String remainingDataString = "";
    void parseUsbData (String dataString) {

        remainingDataString = remainingDataString+dataString.replace("\r",""); // append data String
        String[] dataStringsToParse = remainingDataString.split("\n");

        if(dataStringsToParse.length > 1) { // found valid segment to parse

            remainingDataString = dataStringsToParse[dataStringsToParse.length - 1];

            // NOTE: ignore the last component and wait the next parsing
            for (int i = 0; i < dataStringsToParse.length - 1; i++) {
                String[] valueStringsToParse = dataStringsToParse[i].split(",");

                // only parse the data String fit the format
                if (valueStringsToParse.length == 2 && isInteger(valueStringsToParse[0]) && isInteger(valueStringsToParse[1])) {
                    int val0 = Integer.parseInt(valueStringsToParse[0]);
                    int val1 = Integer.parseInt(valueStringsToParse[1]);
                    caller.updateExtForce(val0, val1);
                }
            }

        }

    }


    // ref: http://stackoverflow.com/questions/5439529/determine-if-a-string-is-an-integer-in-java
    public static boolean isInteger(String s) {
        return isInteger(s,10);
    }

    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }

}
