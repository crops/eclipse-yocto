package org.yocto.remote.utils;

import org.eclipse.jface.dialogs.MessageDialog;

public 	class DialogRunnable implements Runnable{
	int type = 0;
	boolean result;
	public static final int QUESTION = 1;
	public static final int ERROR = 2;
	String title;
	String message;

	DialogRunnable(String title, String message, int type){
		this.title = title;
		this.message = message;
		this.type = type;
	}
	@Override
	public void run() {
		if (type == QUESTION) {
			result = MessageDialog.openQuestion(null, title, message);
		} else if (type == ERROR) {
			MessageDialog.openError(null, title, message);
		}
	}
}