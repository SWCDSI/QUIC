# ForcePhoneDemoForSamsung

It is a demo app for Samsung. This demo app shows how to import the ForcePhone .aar and how to layer it.


# Usage
You can control the flag called "checkPressureInsteadOfSqueeze" to switch the force detection mode or squeeze mode

# Force Detection (checkPressureInsteadOfSqueeze == true)
This app will update the "normalized" (not calibrated) force estimation when the screen is touched

# Squeeze Reference (checkPressureInsteadOfSqueeze == false)
This app will update the reference for detecting sqeeze (usually there is bump of this reference signals when the squeeze happens)
