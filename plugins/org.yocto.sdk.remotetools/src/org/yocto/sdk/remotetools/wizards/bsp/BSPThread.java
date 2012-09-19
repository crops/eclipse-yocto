package org.yocto.sdk.remotetools.wizards.bsp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Receives a command to be run on a separate thread in the background
 * It contains an BSPAction object that will collect the output & error
 * Output lines are processed and collected into the items of BSPAction
 * @author ioana.grigoropol
 *
 */
public abstract class BSPThread implements Runnable {
	public static final String SUCCESS = "success";
	public static final String ERROR = "error";

	private BSPAction bspAction;
	private String command;

	/**
	 * Receives the command to be run in the background
	 * @param command
	 */
	public BSPThread(String command) {
		this.command = command;
		this.bspAction = new BSPAction(null, null);
	}

	@Override
	public void run() {
		ArrayList<String> values = new ArrayList<String>();

		try {
			ProcessBuilder builder = new ProcessBuilder(new String[] {"sh", "-c", command});
			// redirect error stream to collect both output & error
			builder.redirectErrorStream(true);
			Process process = builder.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			String errorMessage = "";
			while ( (line = br.readLine()) != null) {
				String[] result = processLine(line);
				String status = result[0];
				String value = result[1];
				if (status.equals(ERROR) && !value.isEmpty()) {
					errorMessage += value;
					continue;
				}
				if (!value.isEmpty())
					values.add(value);
			}
			int exitVal = process.waitFor();

			// if the background process did not exit with 0 code, we should set the status accordingly
			if (exitVal != 0) {
				bspAction.setMessage(errorMessage);
				bspAction.setItems(null);
			}
		} catch (Exception e) {
			bspAction.setMessage(e.getMessage());
			bspAction.setItems(null);
		}
		if (!values.isEmpty()) {
			bspAction.setMessage(null);
			bspAction.setItems(values.toArray(new String[values.size()]));
		}
	}

	/**
	 * Each command ran in the background will have a different output and a different way of processing it
	 * @param line
	 * @return
	 */
	protected abstract String[] processLine(String line);

	public BSPAction getBspAction() {
		return bspAction;
	}

	public void setBspAction(BSPAction bspAction) {
		this.bspAction = bspAction;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}
}
