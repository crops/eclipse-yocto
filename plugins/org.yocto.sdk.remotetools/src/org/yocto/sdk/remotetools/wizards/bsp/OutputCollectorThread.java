package org.yocto.sdk.remotetools.wizards.bsp;

/**
 * BSPThread that returns all the output lines of the process execution
 * @author ioana.grigoropol
 *
 */
public class OutputCollectorThread extends BSPThread{

	public OutputCollectorThread(String command) {
		super(command);
	}

	@Override
	protected String[] processLine(String line) {
		return new String[]{SUCCESS, line};
	}

}
