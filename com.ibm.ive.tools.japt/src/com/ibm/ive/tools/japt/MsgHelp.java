/*
 * Created on Feb 12, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.ive.tools.japt;

/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2003, 2004  All Rights Reserved
 */

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * This class contains helper methods for loading resource
 * bundles and formatting external message strings.
 *
 * @author		OTI
 * @version		initial
 */

public final class MsgHelp {

	/**
	 * Changes the locale of the messages.
	 *
	 * @author		OTI
	 * @version		initial
	 *
	 * @param		locale Locale
	 *					the locale to change to.
	 */
	static public ResourceBundle setLocale (final Locale locale, final String resource) throws MissingResourceException {
		return (ResourceBundle)AccessController.doPrivileged(
			new PrivilegedAction() {
				public Object run() {
					return ResourceBundle.getBundle(resource, locale);
				}
			});
	}
}
