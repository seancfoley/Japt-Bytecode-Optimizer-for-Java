/*
 * Created on Sep 7, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.commandLine;

import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.IntegratedExtension;
import com.ibm.ive.tools.japt.MessageLogger;

public class ExtensionDescriptor {

	public final String className;
	public String newName;
	public CommandLineExtension instance;
	private CommandLineMessages messages;
	
	public ExtensionDescriptor(String className, CommandLineExtension instance, CommandLineMessages messages) {
		this.className = className;
		this.instance = instance;
		this.messages = messages;
	}
	
	public ExtensionDescriptor(String className, CommandLineMessages messages) {
		this.className = className;
		this.messages = messages;
	}
	
	/**
	 * Note that setName can only be called for extensions that are instances of IntegratedExtension
	 * @param name
	 */
	public void setName(String name) {
		this.newName = name;
		if(instance != null) {
			((IntegratedExtension) instance).setName(name);
		}
	}
	
	public CommandLineExtension createExtension(MessageLogger messageLogger) {
		if(instance != null) {
			return instance;
		}
		Class clazz = null;
		try {
			clazz = Class.forName(className);
			try {
				Object object = clazz.newInstance();
				instance = (CommandLineExtension) object;
				if(newName != null) {
					((IntegratedExtension) instance).setName(newName);
				}
			}
			catch(IllegalAccessException e) {
				messageLogger.logMessage(messages.UNAVAILABLE_CONSTRUCTOR, clazz);
			}
			catch(InstantiationException e) {
				messageLogger.logMessage(messages.UNINSTANTIABLE, clazz);
			}
			catch(ClassCastException e) {
				messageLogger.logMessage(messages.INVALID_EXTENSION, new Object[] {clazz, CommandLineExtension.class});
			}
		}
		catch(SecurityException e) {
			messageLogger.logMessage(messages.NO_PERMISSIONS, clazz);
		}
		catch(ClassNotFoundException e) {
			messageLogger.logMessage(messages.NO_EXTENSION, className);
		}
		catch(ExceptionInInitializerError e) {
			messageLogger.logMessage(messages.ERROR_INITIALIZING, new Object[] {className, e.getException()});
		}
		catch(LinkageError e) {
			messageLogger.logMessage(messages.ERROR_LINKING, className);
		}
		return instance;
	}
	
	public CommandLineExtension createParserExtension(CommandLineParser fromParser) {
		if(instance != null) {
			return instance;
		}
		createExtension(fromParser);
		if(instance != null) {
			addExtensionOptions(fromParser);
		}
		return instance;
	}

	public void addExtensionOptions(CommandLineParser fromParser) {
		Option newOptions[] = instance.getOptions();
		if(newOptions != null) {
			for(int i=0; i<newOptions.length; i++) {
				fromParser.addExtensionOption(newOptions[i]);
			}
		}
	}

}
