/*
 * Created on Sep 11, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.removal;

import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.JaptMessage;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.MemberSelector;
import com.ibm.ive.tools.japt.Specifier;
import com.ibm.ive.tools.japt.JaptMessage.InfoMessage;
import com.ibm.ive.tools.japt.MemberActor.MemberCollectorActor;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;
import com.ibm.ive.tools.japt.commandLine.options.SpecifierOption;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_HashedFieldVector;
import com.ibm.jikesbt.BT_HashedMethodVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;

public class RemovalExtension implements CommandLineExtension {
	final public SpecifierOption clinits = new SpecifierOption("removeStaticInits", "remove static init(s) from specified class(es)");
	final public SpecifierOption privates = new SpecifierOption("removePrivateMembers", "remove private method(s) and field(s) from specified class(es)");
	final public SpecifierOption privatePackaged = new SpecifierOption("removePackageMembers", "remove private/package-access method(s) and field(s) from specified class(es)");
	final public SpecifierOption privateClasses = new SpecifierOption("removePackageClasses", "remove specified package-access class(es)");
	final public SpecifierOption anyClasses = new SpecifierOption("removeClasses", "remove specified class(es)");
	//final public SpecifierOption innerClasses = new SpecifierOption("removeInnerClasses", "remove specified class(es)");
	
	final public SpecifierOption methodBodies = new SpecifierOption("removeMethodBodies", "remove body(ies) from specified method(s)");
	final public SpecifierOption classMethodBodies = new SpecifierOption("removeMethBodsInClass", "remove body(ies) from method(s) in specified class(es)");
	final JaptMessage clinitMessage = new InfoMessage(this, new FormattedString("removed static initializer from class {0}"));
	final JaptMessage removedBodyMessage = new InfoMessage(this, new FormattedString("removed body from method {0}"));
	final JaptMessage removedMessage = new InfoMessage(this, new FormattedString("removed {0}"));
	
	public RemovalExtension() {}

	public Option[] getOptions() {
		return new Option[] {clinits, methodBodies, classMethodBodies, privates, privatePackaged, privateClasses, anyClasses};
	}

	public void execute(JaptRepository repository, Logger logger)
			throws ExtensionException {
		
		Specifier specs[];
		BT_ClassVector classes;
		
		specs = anyClasses.getSpecifiers();
		classes = repository.findClasses(specs, true);
		for(int i=0; i<classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			removedMessage.log(logger, clazz.useName());
			clazz.remove();
		}
		
//		specs = innerClasses.getSpecifiers();
//		classes = repository.findClasses(specs, true);
//		for(int i=0; i<classes.size(); i++) {
//			BT_Class clazz = classes.elementAt(i);
//			if(clazz.isInnerClass()) {
//				removedMessage.log(logger, clazz.useName());
//				clazz.remove();
//			}
//		}
		
		specs = privateClasses.getSpecifiers();
		classes = repository.findClasses(specs, true);
		for(int i=0; i<classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			if(!clazz.isPublic()) {
				removedMessage.log(logger, clazz.useName());
				clazz.remove();
			}
		}
		
		if(privates.appears() || privatePackaged.appears()) {
			BT_MethodVector methods = new BT_HashedMethodVector();
			BT_FieldVector fields = new BT_HashedFieldVector();
			MemberCollectorActor actor = new MemberCollectorActor(methods, fields);
			
			if(privatePackaged.appears()) {
				specs = privatePackaged.getSpecifiers();
				repository.findClasses(specs, actor, MemberSelector.privatePackageResolvableSelector, true);
			}
			if(privates.appears()) {
				specs = privates.getSpecifiers();
				repository.findClasses(specs, actor, MemberSelector.privateResolvableSelector, true);
			}
			
			for(int i=0; i<methods.size(); i++) {
				BT_Method method = methods.elementAt(i);
				removedMessage.log(logger, method.useName());
				method.remove();
			}
			for(int i=0; i<fields.size(); i++) {
				BT_Field field = fields.elementAt(i);
				removedMessage.log(logger, field.useName());
				field.remove();
			}
		}
		
		specs = clinits.getSpecifiers();
		classes = repository.findClasses(specs, true);
		for(int i=0; i<classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			BT_MethodVector methods = clazz.methods;
			for(int j=0; j<methods.size(); j++) {
				BT_Method method = methods.elementAt(j);
				if(method.isStaticInitializer()) {
					clinitMessage.log(logger, clazz);
					method.remove();
				} 
			}
		}
		
		specs = classMethodBodies.getSpecifiers();
		BT_MethodVector methods = new BT_HashedMethodVector();
		MemberCollectorActor actor = new MemberCollectorActor(methods, null);
		repository.findClasses(specs, actor, MemberSelector.allNonResolvableMethodSelector, true);
		
		specs = methodBodies.getSpecifiers();
		repository.findMethods(specs, actor);
		
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			if(!method.isAbstract() 
				&& !method.isNative() 
				&& !method.isStub() 
				&& method.getCode() != null
				&& repository.isInternalClass(method.getDeclaringClass())) {
				removedBodyMessage.log(logger, method.useName());
				method.makeCodeSimplyReturn();
			}
		}
	}
	
	

	public String getName() {
		return "removal";
	}

	

}
