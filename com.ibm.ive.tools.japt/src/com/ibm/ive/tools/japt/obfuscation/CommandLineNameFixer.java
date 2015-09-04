package com.ibm.ive.tools.japt.obfuscation;



import com.ibm.ive.tools.japt.InternalClassesInterface;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;


/**
 * @author sfoley
 * <p>
 * Prevents names that were specified on the command line from being changed
 */
class CommandLineNameFixer {

	private NameHandler nameHandler;
	private JaptRepository repository;
 	private String reason = "command line";
		
	public CommandLineNameFixer(JaptRepository repository, NameHandler nameHandler) {
		this.nameHandler = nameHandler;
		this.repository = repository;
	}
	
	void fixNamesFromCommandLine() {
		InternalClassesInterface internalClassesInterface = repository.getInternalClassesInterface();
		fixSpecifiedClassNames(internalClassesInterface.getAllInterfaceClasses(), false);
		fixSpecifiedMethodNames(internalClassesInterface.getAllInterfaceMethods());
		fixSpecifiedFieldNames(internalClassesInterface.getAllInterfaceFields());
	}
	
	private void fixSpecifiedClassNames(BT_ClassVector fixedClasses, boolean fixSubClasses) {
		for(int j=0; j<fixedClasses.size(); j++) {
			BT_Class clazz = fixedClasses.elementAt(j);
			if(repository.isInternalClass(clazz)) { /* the standard name fixer will fix all external classes, so we skip them */
				nameHandler.fixName(clazz, reason);
			}
			if(fixSubClasses) {
				fixSpecifiedSubclassNames(clazz);
			}
		}
	}
	
	private void fixSpecifiedSubclassNames(BT_Class clazz) {
		BT_ClassVector kids = clazz.getKids();
		for(int i=0; i<kids.size(); i++) {
			BT_Class kid = kids.elementAt(i);
			if(repository.isInternalClass(kid)) { /* the standard name fixer will fix all external classes, so we skip them */
				nameHandler.fixName(kid, reason);	
			}
			fixSpecifiedSubclassNames(kid);
		}
	}
	
	
	private void fixSpecifiedMethodNames(BT_MethodVector methods) {
		for(int j=0; j<methods.size(); j++) {
			BT_Method method = methods.elementAt(j);
			if(repository.isInternalClass(method.getDeclaringClass()) && !StandardNameFixer.isStandardMethod(method)) { 
				/* the standard name fixer will fix all external classes, so we skip them, as well as all standard methods (initializers and finalizers) */
				nameHandler.fixName(method, reason);
			}
		}
	}
	
	private void fixSpecifiedFieldNames(BT_FieldVector fields) {
		for(int j=0; j<fields.size(); j++) {
			BT_Field field = fields.elementAt(j);
			if(repository.isInternalClass(field.getDeclaringClass())) { /* the standard name fixer will fix all external classes, so we skip them */
				nameHandler.fixName(field, reason);
			}
		}
	}
	


}
