package com.ibm.ive.tools.japt.test;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/*
 * Created on Jun 18, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ProcessThread extends Thread {

	String out, err;
	String commandLine;
	IOException e, errE;

	ProcessThread(String commandLine) {
		this.commandLine = commandLine;
	}

	public void run() {
		Runtime runtime = Runtime.getRuntime();
		try {
			final Process process = runtime.exec(commandLine);
			Thread errThread = new Thread() {
				public void run() {
					try {
						err = readStream(process.getErrorStream());
					} catch(IOException e) {
						errE = e;
					}
				}
			};
			errThread.start();
			out = readStream(process.getInputStream());
			errThread.join();
		}
		catch(InterruptedException e) {}
		catch(IOException e) {
			this.e = e;
		}
	}

	private String readStream(InputStream stream) throws IOException {
		int numRead;
		char buffer[] = new char[80];
		StringBuffer output = new StringBuffer();
		InputStreamReader reader = new InputStreamReader(stream);
		while(true) {
			numRead = reader.read(buffer, 0, buffer.length);
			if(numRead < 0) {
				break;
			}
			output.append(buffer, 0, numRead);
		}
		return output.toString();
	}

	public String getOutput() throws IOException {
		if(e != null) {
			throw e;
		}
		if(out != null) {
			return out;
		}
		return "";
	}
	
	public String getErrorOutput() throws IOException {
		if(errE != null) {
			throw errE;
		}
		if(err != null) {
			return err;
		}
		return "";
	}
	

}
