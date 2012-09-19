package org.yocto.sdk.remotetools.wizards.bsp;

/**
 * BSPThread that processes the output of "yocto-bsp list karch"
 * @author ioana.grigoropol
 *
 */
public class KernelArchGetter extends BSPThread{

	public KernelArchGetter(String command) {
		super(command);
	}

	@Override
	protected String[] processLine(String line) {
		if (line.contains(":"))
			return new String[]{SUCCESS, ""};
		line = line.replaceAll("^\\s+", "");
		line = line.replaceAll("\\s+$", "");
		return new String[]{SUCCESS, line};
	}

}
