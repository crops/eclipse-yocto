package org.yocto.sdk.remotetools.wizards.bsp;

/**
 * BSPThread that processes the output of running "yocto-bsp list qemu property qemuarch"
 * @author ioana.grigoropol
 *
 */
public class QemuArchGetter extends BSPThread {

	public QemuArchGetter(String command) {
		super(command);
	}

	@Override
	protected String[] processLine(String line) {
		if (!line.startsWith("["))
			return new String[]{ERROR, line + "\n"};

		String[] values = line.split(",");

		String value = values[0];
		value = value.replace("[\"", "");
		value = value.replaceAll("\"$", "");
		return new String[]{SUCCESS, value};
	}

}
