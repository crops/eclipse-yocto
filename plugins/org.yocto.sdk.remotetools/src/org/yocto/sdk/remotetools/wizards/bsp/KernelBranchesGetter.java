package org.yocto.sdk.remotetools.wizards.bsp;

/**
 * BSPThread that processes the output lines from running command "yocto-bsp list" for the selected kernel
 * @author ioana.grigoropol
 *
 */
public class KernelBranchesGetter extends BSPThread {

	public KernelBranchesGetter(String command) {
		super(command);
	}

	@Override
	protected String[] processLine(String line) {
		// [TODO : Ioana]: find a better way to identify error lines
		if (!line.startsWith("["))
			return new String[]{ERROR, line + "\n"};

		String[] items = line.split(",");

		String value = items[0];
		value = value.replace("[\"", "");
		value = value.replaceAll("\"$", "");
		return new String[]{SUCCESS, value};
	}

}
