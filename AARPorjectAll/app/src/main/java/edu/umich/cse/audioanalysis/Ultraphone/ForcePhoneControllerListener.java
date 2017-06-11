package edu.umich.cse.audioanalysis.Ultraphone;

// NOTE: this listener is only used for samsung service

public interface ForcePhoneControllerListener {
	public void pressureUpdate(final double pressureNotCalibed);
	public void error(final int code, final String msg);
}
