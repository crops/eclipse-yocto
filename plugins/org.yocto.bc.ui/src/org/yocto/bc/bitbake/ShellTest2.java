package org.yocto.bc.bitbake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Tests for ShellSession.
 * 
 * @author kgilmer
 * 
 */
public class ShellTest2 {
	public static void main(String[] args) {
		ShellTest2 st = new ShellTest2();

		//testSimpleCommands();

		testStreamResponse();
	}

	private static void streamOut(InputStream executeIS) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(executeIS));

		String line = null;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}
	}

	private static void testSimpleCommands() {
		try {
			ShellSession ss = new ShellSession(ShellSession.SHELL_TYPE_BASH, null, null, null);
			System.out.println(ss.execute("echo \"bo is $boo\""));

			System.out.println(ss.execute("export boo=asdf"));

			System.out.println(ss.execute("echo \"bo is $boo\""));

			System.out.println(ss.execute("cd /home/kgilmer/dev/workspaces/com.buglabs.build.oe"));
			System.out.println(ss.execute("source reinstate-build-env"));
			System.out.println(ss.execute("echo $BBPATH"));
			System.out.println(ss.execute("bitbake -e"));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void testStreamResponse() {
		try {
			final ShellSession ss = new ShellSession(ShellSession.SHELL_TYPE_BASH, null, null, null);

			ss.execute("./loop.sh", ShellSession.TERMINATOR, new ICommandResponseHandler() {

				public void response(String line, boolean isError) {
					if (isError) {
						System.out.println("ERROR: " + line);
					} else {
						System.out.println(line);
						ss.interrupt();
					}
					
				}
			});
			
			ss.execute("ls /home", ShellSession.TERMINATOR, new ICommandResponseHandler() {

				public void response(String line, boolean isError) {
					if (isError) {
						System.err.println("ERROR: " + line);
					} else {
						System.out.println(line);
					}
				}
			});
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

/*	*//**
	 * A reader that will terminate upon reading a specific line.  Prevents reader from blocking.
	 * 
	 * @author kgilmer
	 *//*
	private class LineTerminatingReader extends BufferedReader {
		
		private final String terminator;

		public LineTerminatingReader(Reader in, String terminator) {
			super(in);
			this.terminator = terminator;
		}

		public String readLine() throws IOException {
			String line = null;
			
			while (((line = this.readLine()) != null) && !line.equals(terminator)) {
				return line;
			}
	
			return null;
		}

	}*/
}
