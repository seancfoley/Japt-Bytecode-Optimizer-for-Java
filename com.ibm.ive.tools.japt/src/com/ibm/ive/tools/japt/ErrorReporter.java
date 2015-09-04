package com.ibm.ive.tools.japt;

import com.ibm.jikesbt.BT_ClassPathEntry;
import com.ibm.jikesbt.BT_Item;

public class ErrorReporter {
	Logger errorLogger;
	public static int errorCount;
	
	public void setErrorLogger(Logger errorLogger) {
		this.errorLogger = errorLogger;
	}
	
	private static boolean isInternal(JaptRepository rep, BT_ClassPathEntry errorEntry, BT_Item errorItem) {
		if(errorItem != null) {
			return rep.isInternalClass(errorItem.getDeclaringClass());
		}
		if(errorEntry != null) {
			return rep.isInternalClassPathEntry(errorEntry);
		}
		return false;
	}
	
	//TODO move the reportExternal up one level, it should not be down here
	
	public void noteError(JaptRepository rep, boolean checkExternal, BT_ClassPathEntry errorEntry, 
			BT_Item errorItem, String errorLocation, String message, String equivalentRuntimeError) {
		if(logging()) {
			if(checkExternal || isInternal(rep, errorEntry, errorItem)) {
				logError(++errorCount + ": " + equivalentRuntimeError);
				if(errorLocation != null) {
					logError(" in " + errorLocation);
				} else if(errorItem != null) {
					logError(" in " + errorItem.useName());
				}
				logError(Logger.endl);
				logError("\t" + message + Logger.endl);
			}
		}
	}
	
	public boolean logging() {
		return errorLogger != null;
	}
	
	public void logError(String error) {
		errorLogger.logError(error);
	}
}
