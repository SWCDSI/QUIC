package edu.umich.cse.audioanalysis;

public interface MySensorControllerListener {
	public void onTiltChanged(double tiltX, double tiltY, double tiltZ);
	public void onRecordedEnd();
}
