package com.ibm.ive.tools.japt.execOpt.commandLine;

import com.ibm.ive.tools.japt.ErrorReporter;
import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.Messages;
import com.ibm.ive.tools.japt.TransferredClassPathEntry;
import com.ibm.jikesbt.BT_Attribute;
import com.ibm.jikesbt.BT_AttributeException;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassPathEntry;
import com.ibm.jikesbt.BT_Exception;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldRefIns;
import com.ibm.jikesbt.BT_Item;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodRefIns;
import com.ibm.jikesbt.BT_Repository;
import com.ibm.jikesbt.BT_Repository.LoadLocation;

public class RTFactory extends JaptFactory {

	public ErrorReporter errorReporter;
	final boolean reportExternalErrors;
	
	public RTFactory() {
		errorReporter = null;
		reportExternalErrors = true;
	}

	public RTFactory(Logger logger) {
		super(logger);
		reportExternalErrors = true;
	}

	public RTFactory(Messages messages, Logger logger, boolean reportExternalErrors) {
		super(messages, logger);
		errorReporter = null;
		this.reportExternalErrors = reportExternalErrors;
	}
	
	void setErrorReporter(ErrorReporter errorReporter) {
		this.errorReporter = errorReporter;
	}
	
	public void noteUndeclaredMethod(BT_Method m, BT_Class targetClass, BT_Method fromMethod, BT_MethodRefIns fromIns, LoadLocation location) {
		BT_ClassPathEntry entry = location == null ? null : location.getLocation().getClassPathEntry();
		if(entry instanceof TransferredClassPathEntry) {
			return;
		} 
		if(errorReporter != null) {
			if(!targetClass.isStub()) {
				errorReporter.noteError(
						(JaptRepository) targetClass.getRepository(), 
						reportExternalErrors, 
						entry, fromMethod.getDeclaringClass(), fromMethod.useName(), 
						messages.UNRESOLVED_METHOD.toString(new Object[] {m.useName(), fromMethod.useName()}, false),
						BT_Repository.JAVA_LANG_NO_SUCH_METHOD_ERROR);//TODO better message
			}
			return;
		}
		super.noteUndeclaredMethod(m, targetClass, fromMethod, fromIns, location);
	}
	
	public void noteUndeclaredField(BT_Field f, BT_Class targetClass, BT_Method fromMethod, BT_FieldRefIns fromIns, LoadLocation location) {
		BT_ClassPathEntry entry = location == null ? null : location.getLocation().getClassPathEntry();
		if(entry instanceof TransferredClassPathEntry) {
			return;
		}
		if(errorReporter != null) {
			if(!targetClass.isStub()) {
				errorReporter.noteError(
						(JaptRepository) targetClass.getRepository(), 
						reportExternalErrors, 
						entry, fromMethod.getDeclaringClass(), fromMethod.useName(), 
						messages.UNRESOLVED_FIELD.toString(new Object[] {f.useName(), fromMethod.useName()}, false),
						BT_Repository.JAVA_LANG_NO_SUCH_FIELD_ERROR);//TODO better message
			}
			return;
		}
		super.noteUndeclaredField(f, targetClass, fromMethod, fromIns, location);
	}
	
	public void noteClassLoaded(BT_Class c, String fromFileName) {
		//this.logger.logProgress(".");
	}
	
	public void noteClassDereferenced(BT_Class clazz) {
		//this.logger.logProgress(".");
		//messages.DEREFERENCED_CLASS.log(logger, new Object[] {clazz.fullKindName(), clazz});
	}
	
	public BT_Class noteClassNotFound(String className, BT_Repository repository, BT_Class stub, LoadLocation location) {
		if(location != null && location.getLocation().getClassPathEntry() instanceof TransferredClassPathEntry) {
			/* we mark the stub as not loaded so that if it is referenced from user code then we will be in here again */
			//TODO is there a way to do the same for methods and fields?
			if(stub == null) {
				stub = repository.createStub(className, true);
			}
			return stub;
		}
		if(errorReporter != null) {
			String missing = messages.COULD_NOT_FIND_CLASS.toString(className, false);
			if(location != null) {
				missing += " referenced from " + location;
			} 
			errorReporter.noteError(
					(JaptRepository) repository, 
					reportExternalErrors, 
					null, null, className, missing, BT_Repository.JAVA_LANG_NO_CLASS_DEF_FOUND_ERROR);
		} 
		return super.noteClassNotFound(className, repository, stub, location);
	}
	
	public void noteAttributeLoadFailure(BT_Repository rep, BT_Item item, String name, BT_Attribute attribute, BT_AttributeException e, LoadLocation loadLocation) {
		if(errorReporter != null) {
			BT_ClassPathEntry entry = null;
			if(loadLocation != null) {
				entry = loadLocation.getLocation().getClassPathEntry();
			}
			String msg = messages.COULD_NOT_LOAD_ATTRIBUTE.toString(
					new Object[] {attribute == null ? name : attribute.getName(), item.useName(), e.getInitialCause()}, false);
			errorReporter.noteError(
					(JaptRepository) rep, 
					reportExternalErrors, 
					entry, item, item.useName(), msg, null);
		} else {
			super.noteAttributeLoadFailure(rep, item, name, attribute, e, loadLocation);
		}
	}
	
	public void noteClassLoadFailure(
		BT_Repository rep,
		BT_ClassPathEntry entry,
		BT_Class clazz,
		String className,
		String fileName,
		Throwable ex,
		String equivalentRuntimeError) {
		if(ex instanceof BT_Exception) {
			ex = ((BT_Exception) ex).getInitialCause();
		}
		if(errorReporter != null) {
			String message = ex.getMessage();
			if(message == null && ex != null) {
				message = ex.toString();
			}
			errorReporter.noteError(
					(JaptRepository) rep,
					reportExternalErrors, 
					entry, clazz, className, message, equivalentRuntimeError);
		} else {
			super.noteClassLoadFailure(rep, entry, clazz, className, fileName, ex, equivalentRuntimeError);
		}
	}
	
	public void noteClassLoadError(
			BT_ClassPathEntry entry,
			BT_Class clazz,
			String className,
			String fileName,
			String problem,
			String equivalentRuntimeError) {
		if(errorReporter != null) {
			errorReporter.noteError(
					(JaptRepository) clazz.getRepository(), 
					reportExternalErrors, 
					entry, clazz, className, problem, equivalentRuntimeError);
		} else {
			super.noteClassLoadError(entry, clazz, className, fileName, problem, equivalentRuntimeError);
		}
	}
}
