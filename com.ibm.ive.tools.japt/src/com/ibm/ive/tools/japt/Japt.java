package com.ibm.ive.tools.japt;

import com.ibm.ive.tools.japt.commandLine.ExtensionDescriptor;


//TODO consider an extension that searches for common bytecode patterns and inserts method
//calls to a new method

/**
 * @author sfoley
 *
 */
public class Japt {
	private JaptRepository repository;
	private JaptFactory factory;
	
	public Japt(JaptRepository repository) {
		this.repository = repository;
		factory = repository.getFactory();
	}
	
	public static String computeTime(Messages messages, long elapsedTime) {
		if(elapsedTime <= 0) {
			elapsedTime = 1;
		}
		long integralTime = elapsedTime / 1000;
		long decimalTime = (elapsedTime - (integralTime * 1000));
		
		int desiredDecimals = 2;
		int shift = 3 - desiredDecimals;
		if(shift > 0) {
			int multipler = (int) Math.pow(10, shift);
			decimalTime = (decimalTime / multipler) * multipler;
		}
		String decimalString;
		if(decimalTime > 999) {
			//this should never happen
			decimalString = "999";
		} else {
			decimalString = Long.toString(decimalTime);
			if(decimalString.length() >= 4) {
				//this should never happen
				decimalString = "999";
			}
		}
		String zeros = "000";
		if(integralTime < 60) {
			String fullDecimalString = zeros.substring(decimalString.length()) + decimalString;
			fullDecimalString = chopZeros(fullDecimalString);
			return messages.SECONDS.toString(new String[] {
					Long.toString(integralTime), 
					fullDecimalString});
		}
		else {
			long minutes = integralTime / 60;
			long seconds = integralTime - (minutes * 60);
			Message msg;
			if(minutes == 1) {
				msg = messages.MINUTE_SECONDS;
			}
			else {
				msg = messages.MINUTES_SECONDS;
			}
			String fullDecimalString = zeros.substring(decimalString.length()) + decimalString;
			//fullDecimalString = chopExtra(fullDecimalString, 2);
			fullDecimalString = chopZeros(fullDecimalString);
			return msg.toString(new String[] {
				Long.toString(minutes), 
				Long.toString(seconds), 
				fullDecimalString});
		}
	}
	 
	/**
	 * @param fullDecimalString
	 * @return
	 */
	private static String chopZeros(String fullDecimalString) {
		while(true) {
			int len = fullDecimalString.length();
			if(len <= 1 || fullDecimalString.charAt(fullDecimalString.length() - 1) != '0') {
				break;
			}
			fullDecimalString = fullDecimalString.substring(0, len - 1);
		}
		return fullDecimalString;
	}

	public void executeExtension(Extension extension, Logger logger) throws ExtensionException {
		Messages messages = factory.getMessages();
		String name = extension.getName();
		boolean isIntegrated = extension instanceof IntegratedExtension;
		if(isIntegrated) {
			((IntegratedExtension) extension).noteExecuting(logger);
		} else {
			messages.EXECUTING_EXTENSION.log(logger, name);
		}
		long startTime = System.currentTimeMillis();
		extension.execute(repository, logger);
		long totalExtensionTime = System.currentTimeMillis() - startTime;
		String timeString = computeTime(messages, totalExtensionTime);
		if(isIntegrated) {
			((IntegratedExtension) extension).noteExecuted(logger, timeString);
		} else {
			messages.COMPLETED_EXECUTING_EXTENSION.log(logger, new String[] {name, timeString});
		}
		logger.flush();
	}

	public void executeExtensions(Extension extensions[], Logger logger) {
		try {
			for(int i=0; i<extensions.length; i++) {
				Extension instance = extensions[i];
				if(instance != null) {
					executeExtension(instance, logger);
					//We allow for immediate garbage collection by removing the known references to the extension interfaces
					extensions[i] = null;
				}
			}
		}
		catch(ExtensionException e) {
			handleExtensionException(logger, e);
		}
	}
	
	public void executeExtensions(ExtensionDescriptor extensions[], Logger logger) {
		try {
			for(int i=0; i<extensions.length; i++) {
				ExtensionDescriptor extension = extensions[i];
				Extension instance = extension.instance;
				if(instance != null) {
					executeExtension(instance, logger);
				}
				//We allow for immediate garbage collection by removing the known references to the extension interfaces
				extensions[i] = null;
			}
		}
		catch(ExtensionException e) {
			handleExtensionException(logger, e);
		}
	}

	/**
	 * @param logger
	 * @param e
	 */
	private void handleExtensionException(Logger logger, ExtensionException e) {
		Messages messages = factory.getMessages();
		String eName = e.extension.getName();
		if(eName == null) {
			messages.ERROR.log(logger, e);
		}
		else {
			messages.ERROR_EXECUTING_EXTENSION.log(logger, new Object[] {eName, e});
		}
	}

	private void logCompleted(Logger logger, String timeString) {
		Messages messages = factory.getMessages();
		messages.COMPLETED_EXECUTION_IN.log(logger, timeString);
	}
	
	public void logCompleted(Logger logger, long totalExtensionTime) {
		String timeString = computeTime(factory.getMessages(), totalExtensionTime);
		logCompleted(logger, timeString);
	}
	
}
