package org.yocto.sdk.ide;

/* All specific exceptions raised from yocto project should use
 * this specific exception class.
 * Currently we only use it for message printing 
 */

public class YoctoGeneralException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6600798490815526253L;
	/**
	 * 
	 */

	public YoctoGeneralException(String message)
	{
		super(message);
	}
}
