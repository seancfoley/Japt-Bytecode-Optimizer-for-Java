/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.jikesbt;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class retrieves strings from a resource bundle
 * and returns them, formatting them with MessageFormat
 * when required.
 * <p>
 * It is used by the system classes to provide national
 * language support, by looking up messages in the
 * <code>
 *    com.ibm.oti.util.ExternalMessages
 * </code>
 * resource bundle. Note that if this file is not available,
 * or an invalid key is looked up, or resource bundle support
 * is not available, the key itself will be returned as the
 * associated message. This means that the <em>KEY</em> should
 * a reasonable human-readable (english) string.
 *
 * @author		OTI
 * @version		initial
 */
public class Messages {

	private static final String BUNDLE_NAME = "com.ibm.jikesbt.ExternalMessages"; //$NON-NLS-1$

	private static ResourceBundle bundle = null;

	static {
		bundle = MsgHelp.setLocale(Locale.getDefault(), BUNDLE_NAME);
	}
	
	/**
	 * @param 		key	String
	 * 					the key to look up
	 * @return		String
	 * 					the message for that key in the system message bundle
	 */
	public static String getString(String key) {
		if(bundle == null) {
			return '!' + key + '!';
		}
		return bundle.getString(key);
	}
	
	/**
	 * @param		key String
	 *					the key to look up.
	 * @param		arg Object
	 *					the object to insert in the formatted output.
	 * @return		String
	 *					the message for that key in the system message bundle.
	 */
	public static String getString(String key, Object arg) {
		return getString(key, new Object[] {arg});
	}
	
	/**
	 * @param		msg String
	 *					the key to look up.
	 * @param		arg int
	 *					the integer to insert in the formatted output.
	 * @return		String
	 *					the message for that key in the system message bundle.
	 */
	public static String getString (String key, int arg) {
		return getString(key, new Object[] {Integer.toString(arg)});
	}
	
	/**
	 * @param		msg String
	 *					the key to look up.
	 * @param		args Object[]
	 *					the objects to insert in the formatted output.
	 * @return		String
	 *					the message for that key in the system
	 *					message bundle.
	 */
	public static String getString (String key, Object[] args) {
		if(bundle == null) {
			return '!' + key + '!';
		}
		return MsgHelp.format(bundle.getString(key), args);
	}
}

