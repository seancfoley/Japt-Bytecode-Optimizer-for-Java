/*
 * Created on Mar 17, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import com.ibm.jikesbt.BT_JarResource;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class NamedResource extends BT_JarResource {

	/**
	 * @param name
	 * @param contents
	 */
	public NamedResource(String name, byte[] contents) {
		super(name, contents);
	}

	public boolean equals(Object o) {
		if(o instanceof BT_JarResource) {
			BT_JarResource r = (BT_JarResource) o;
			return name.equals(r.name);
		}
		return false;
	}
}
