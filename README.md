# ForcePhoneDemoForSamsung

It is a demo app for Samsung. This demo app shows how to import the ForcePhone .aar and how to layer it.


# Usage
You can control the flag called "checkPressureInsteadOfSqueeze" to switch the force detection mode or squeeze mode

# Force Detection (checkPressureInsteadOfSqueeze == true)
This app will update the "normalized" (not calibrated) force estimation when the screen is touched

# Squeeze Reference (checkPressureInsteadOfSqueeze == false)
This app will update the reference for detecting sqeeze (usually there is bump of this reference signals when the squeeze happens)

# Matlab server usage (for developing)
1. Enable the .aar to use the remove mode by setting the "useRemoteMatlabModeInsteadOfStandaloneMode" flag
2. Input the correct IP address of the server (where you will execute the Matlab) to C.SERVER_ADDR (ex: C.SERVER_ADDR = "10.0.0.229";)
4. Go to the root folder of this server: "matlab/old/UltraPhone"
5. Run "ParserConfig.m"
6. Run "ServerDemo.m"
7. Now run the app on the phone and you should see a figure including the force reference in real time (as attached)

After you turn off the app and the server stops, the force reference will be saved to a variable in Matlab called "forceRef". That metric is the number you have seen on the phone without using the server
