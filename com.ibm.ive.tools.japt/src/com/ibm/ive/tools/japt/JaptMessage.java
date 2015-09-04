package com.ibm.ive.tools.japt;

import java.util.Locale;
import java.util.ResourceBundle;


/**
 * Represents a japt log message to be formatted with the common japt message formatting scheme.
 * 
 * @author sfoley
 *
 */
public abstract class JaptMessage extends LogMessage {

	private Component component;
	protected String prefix; 
	boolean enabled = true;
	
	private static final String BUNDLE_NAME = "com.ibm.ive.tools.japt.ExternalMessageLabels"; //$NON-NLS-1$
	private static ResourceBundle bundle = MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	private static final FormattedString summaryString = new FormattedString(getString("com.ibm.ive.tools.japt.Summary__{0}_errors__{1}_warnings_3"));
	
	public JaptMessage(Component component, FormattedString message) {
		super(message);
		this.component = component;
	}
	
	public JaptMessage(Component component, String message) {
		super(message);
		this.component = component;
	}

	public JaptMessage(Component component, String components[]) {
		super(components);
		this.component = component;
	}
	
	public Component getComponent() {
		return component;
	}
	
	/**
	 * If a message is disabled, attempts to log the message will fail.
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * @param 		key	String
	 * 					the key to look up
	 * @return		String
	 * 					the message for that key in the system message bundle
	 */
	static String getString(String key) {
		if(bundle != null) {
			return bundle.getString(key);
		}
		return '!' + key + '!';
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	public void log(Logger logger, Object arguments[]) {
		if(enabled) {
			outputMessageStart(logger, component);
			super.log(logger, arguments);
		}
	}
	
	public String toString(Object argument, boolean includeStart) {
		return toString(new Object[] {argument}, includeStart);
	}
	
	public String toString(Object arguments[]) {
		return toString(arguments, true);
	}
	
	public String toString(Object arguments[], boolean includeStart) {
		if(includeStart) {
			StringBuffer res = formatMessageStart(component);
			res.append(super.toString(arguments));
			return res.toString();
		} else {
			return super.toString(arguments);
		}
	}
		
	protected static String getComponentName(Component component) {
		if(component != null) { 
			return component.getName();
		}
		return null;
	}
	
	protected void outputMessageStart(Logger logger, Component component) {
		String prefix = getPrefix();
		String name = getComponentName(component);
		if(name != null || prefix != null) {
			outputObject(logger, "[");
			if(name != null) {
				outputObject(logger, name);
			}
			if(prefix != null) {
				if(name != null) {
					outputObject(logger, " ");
				}
				outputObject(logger, prefix);
			}
			outputObject(logger, "] ");
		}
	}
	
	protected StringBuffer formatMessageStart(Component component) {
		String prefix = getPrefix();
		return formatStart(component, prefix);
	}
	
	public static StringBuffer formatStart(Component component) {
		return formatStart(component, null);
	}

	public static StringBuffer formatStart(Component component, String prefix) {
		String name = getComponentName(component);
		StringBuffer res = new StringBuffer();
		if(name != null || prefix != null) {
			res.append('[');
			if(name != null) {
				res.append(name);
			}
			if(prefix != null) {
				if(name != null) {
					res.append(' ');
				}
				res.append(prefix);
			}
			res.append("] ");
		}
		return res;
	}
	
	public static void logSummary(Component component, Logger logger) {
		LogMessage message = new StatusMessage(component, summaryString);
		Object args[] = new Object[] {Integer.toString(ErrorMessage.count), Integer.toString(WarningMessage.count)};
		message.log(logger, args);
	}
	
	/**
	 * A japt log message to be delivered to the logger's status stream.
	 * 
	 * @author sfoley
	 *
	 */
	public static class StatusMessage extends JaptMessage {
		
		public StatusMessage(Component component, FormattedString message) {
			super(component, message);
		}
		
		public StatusMessage(Component component, String message) {
			super(component, message);
		}
	
		public StatusMessage(Component component, String components[]) {
			super(component, components);
		}
	
		protected void outputObject(Logger logger, Object object) {
			logger.logStatus(object.toString());
		}
	}

	public static class InfoMessage extends JaptMessage {
		
		public InfoMessage(Component component, FormattedString message) {
			super(component, message);
		}
		
		public InfoMessage(Component component, String message) {
			super(component, message);
		}
	
		public InfoMessage(Component component, String components[]) {
			super(component, components);
		}
	
		protected void outputObject(Logger logger, Object object) {
			logger.logInfo(object.toString());
		}
	}
	
	public static class ErrorMessage extends JaptMessage {
		public static int count;
		private static final String ERROR_STR = getString("com.ibm.ive.tools.japt.error_1");
	
		public ErrorMessage(Component component, FormattedString message) {
			super(component, message);
			this.prefix = ERROR_STR;
		}

		public ErrorMessage(Component component, String message) {
			super(component, message);
			this.prefix = ERROR_STR;
		}
	
		public ErrorMessage(Component component, String components[]) {
			super(component, components);
			this.prefix = ERROR_STR;
		}
		
		protected void outputObject(Logger logger, Object object) {
			logger.logError(object.toString());
		}
		
		public void log(Logger logger, Object arguments[]) {
			if(enabled) {
				super.log(logger, arguments);
				synchronized(getClass()) {
					count++;
				}
			}
		}
	}
	
	public static class WarningMessage extends JaptMessage {
		public static int count;
		private static final String WARNING_STR = getString("com.ibm.ive.tools.japt.warning_2");
		
		public WarningMessage(Component component, FormattedString message) {
			super(component, message);
			this.prefix = WARNING_STR;
		}
		
		public WarningMessage(Component component, String message) {
			super(component, message);
			this.prefix = WARNING_STR;
		}
	
		public WarningMessage(Component component, String components[]) {
			super(component, components);
			this.prefix = WARNING_STR;
		}
	
		protected void outputObject(Logger logger, Object object) {
			logger.logWarning(object.toString());
		}
		
		public void log(Logger logger, Object arguments[]) {
			if(enabled) {
				super.log(logger, arguments);
				synchronized(getClass()) {
					count++;
				}
			}
		}
	}
	
	public static class ProgressMessage extends JaptMessage {
		
		public ProgressMessage(Component component, FormattedString message) {
			super(component, message);
		}
		
		public ProgressMessage(Component component, String message) {
			super(component, message);
		}
	
		public ProgressMessage(Component component, String components[]) {
			super(component, components);
		}
	
		protected void outputObject(Logger logger, Object object) {
			logger.logProgress(object.toString());
		}
	}
}
