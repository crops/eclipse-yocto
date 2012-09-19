package org.yocto.sdk.remotetools.wizards.bsp;

/**
 * BSPThread that ignores the output of the process and returns an error if the process exits with non zero code
 * @author ioana.grigoropol
 *
 */
public class ErrorCollectorThread extends BSPThread{

	public ErrorCollectorThread(String command) {
		super(command);
	}

	@Override
	protected String[] processLine(String line) {
		return null;
	}

}
