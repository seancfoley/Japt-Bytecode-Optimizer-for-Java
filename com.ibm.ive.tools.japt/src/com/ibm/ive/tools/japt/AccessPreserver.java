/*
 * Created on May 4, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_InsVector;

/**
 * 
 * @author sfoley
 *
 * Objects of this type preserve the access permissions for a code attribute.  Objects that implement this type wish to
 * preserve access to the items referenced by a given code attribute from a pre-specified class.
 * 
 * Like an iterator, the order in which the methods in this type are called affect the results.  Callers originally make a
 * call to preserveAccess and receive an answer regarding whether it is possible for the AccessPreserver object to preserve the
 * access to the code's referenced items.  If the result is true, then subsequent calls to changesAreRequired will inform the caller
 * whether the preserve must make changes to existing internal classes to allow access to be preserved.  Subsequent calls to doChanges
 * will make these changes happen. 
 */
public interface AccessPreserver {
	
	boolean preserveAccess(BT_CodeAttribute code, BT_InsVector ignoreInstructions) throws BT_CodeException;
	
	boolean changesAreRequired();
	
	void doChanges();

}
