package org.yocto.sdk.remotetools.wizards.bsp;

/**
 * Stores a list of items from the output of a background thread and the error message if something went wrong
 * @author ioana.grigoropol
 *
 */
public class BSPAction {
	private String[] items;
	private String message;

	BSPAction(String[] items, String message){
		this.setItems(items);
		this.setMessage(message);
	}

	public String[] getItems() {
		return items;
	}

	public void setItems(String[] items) {
		this.items = items;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}