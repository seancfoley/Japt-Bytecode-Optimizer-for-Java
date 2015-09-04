/*
 * Created on Apr 12, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.coldMethod;

import com.ibm.ive.tools.japt.AccessorCallSiteVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_Method;

public class Method {
	private boolean isCold;
	private BT_Method migratedMethod;
	public final BT_Method method;
	private MethodMigrator migrator;
	
	public Method(BT_Method method) {
		this.method = method;
	}
	
	public boolean equals(Object o) {
		if(o instanceof Method) {
			Method other = (Method) o;
			return method.equals(other.method);
		}
		return false;
	}
	
	public void setCold(boolean cold) {
		isCold = cold;
	}
	
	BT_Method getMigratedMethod() {
		return migratedMethod;
	}
	
	public AccessorCallSiteVector bypassAccessors() {
		if(migrator != null && migratedMethod != null) {
			return migrator.bypassAccessors();
		}
		return AccessorCallSiteVector.emptySiteVector;
	}
	
	/** 
	 * Create a copy of this cold method in the given class.  This might also entail adding accessors
	 * or changing the permissions of methods and fields that are accessed by the code in the migrated 
	 * method. 
	 */
	public boolean migrate(BT_Class toClass, ExtensionRepository coldRep, Clazz origClass) throws BT_CodeException {
		migrator = new MethodMigrator(coldRep, method, origClass.clazz, toClass);
		if(!migrator.canMigrate()) {
			return false;
		}
		migratedMethod = migrator.migrate();
		return migratedMethod != null;
	}
	
	public boolean isCold() {
		return isCold;
	}
	
	public String toString() {
		return method.toString();
	}
}
