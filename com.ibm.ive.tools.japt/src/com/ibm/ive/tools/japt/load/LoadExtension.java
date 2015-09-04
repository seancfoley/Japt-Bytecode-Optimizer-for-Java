/*
 * Created on Sep 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.load;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;

import com.ibm.ive.tools.commandLine.NullOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.IntegratedExtension;
import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;
import com.ibm.jikesbt.BT_Attribute;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_Item;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Item.ReferenceSelector;

/**
 * @author sfoley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LoadExtension implements CommandLineExtension, IntegratedExtension {

	public Messages messages = new Messages(this);
	public Options options = new Options(messages);
	public String name = messages.DESCRIPTION;
	
	/**
	 * 
	 */
	public LoadExtension() {}

	/**
	 * @return null or an array of options pertaining to the extension
	 */
	public Option[] getOptions() {
		return new Option[] {
			
			options.internalClassPathList,
			options.internalClassPathAll,
			options.externalClassPathList,
			options.externalClassPathAll,
			options.jreClassPath,
			
			options.load,
			options.loadAll,
			options.loadClass,
			options.loadResource,
			
			NullOption.nullOption, /* skips a line in the usage message */
			
			options.includeClass,
			options.includeWholeClass,
			options.includeLibraryClass,
			options.includeAccessibleClass,
			options.includeField,
			options.includeMethod,
			options.includeMainMethod,
			options.includeMethodEx,
			
			options.includeExtendedLibraryClass,
			options.includeExtendedAccessibleClass,
			options.includeSubclass,
			options.includeResource,
			options.includeSerializable,
			options.includeSerialized,
			options.includeExternalized,
			options.includeDynamicClassLoad,
			
			NullOption.nullOption, /* skips a line in the usage message */
			
			options.mainClass,
			//options.addStackMaps,
			options.verify,
			options.printClass,
			options.printMethod,
			options.optimize,
			options.reflectionWarnings,
			options.builtInRules,
			options.noBuiltInRules,
			options.refTree,
			options.noFollow,
			options.resetClassPath,
			options.fileExtension,
			options.referenceFile,
			options.unresolvedReferenceFile,
			options.noDebug,
			options.noTrackClasses,
			options.noReadStackMaps
		};
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Extension#execute(com.ibm.ive.tools.japt.JaptRepository, com.ibm.ive.tools.japt.Logger)
	 */
	public void execute(JaptRepository repository, Logger logger)
			throws ExtensionException {
		JaptFactory factory = repository.getFactory();
		
		if(options.noDebug.appears()) {
			factory.readDebugInfo = false;
		} 
		if(options.noTrackClasses.appears()) {
			factory.trackClassReferences = false;
		}
		if(options.noReadStackMaps.appears()) {
			factory.readStackMaps = false;
		}
		if(options.unresolvedReferenceFile.appears()) {
			factory.saveUnresolved = true;
		}
		int internalCount = factory.getInternalClassCount();
		int externalCount = factory.getExternalClassCount();
		int notLoadedCount = factory.getNotLoadedClassCount();
		int notFoundCount = factory.getNotFoundClassCount();
			
		RefReport ref = createRefReport(logger, factory);
		RepositoryLoader loader = createRepositoryLoader(ref, repository, logger);
		if(options.resetClassPath.isFlagged()) {
			repository.resetClassLoading();
		}
		loader.setClassPath();
		loader.load(options.verify.isFlagged(), !options.noFollow.isFlagged());
		loader.inspect();
		ref.completeRefTree();
		repository.trimToSize();
		messages.COMPLETED_LOADING.log(logger, new String[] {
				Integer.toString(factory.getInternalClassCount() - internalCount), 
				Integer.toString(factory.getExternalClassCount() - externalCount), 
				Integer.toString(factory.getNotLoadedClassCount() - notLoadedCount),
				Integer.toString(factory.getNotFoundClassCount() - notFoundCount)
		});
		repository.resetClassLoading();
		if(options.unresolvedReferenceFile.appears()) {
			String file = options.unresolvedReferenceFile.getValue();
			if(file.indexOf('.') == -1) {
				file += ".txt";
			}
			try {
				PrintStream stream = new PrintStream(
								new BufferedOutputStream(
										new FileOutputStream(file)));
				messages.CREATING_UNRESOLVED_REF.log(logger, file);
				writeUnresolved(repository, stream);
				stream.close();
			} catch(FileNotFoundException e) {
				messages.NO_UNRESOLVED_REF.log(logger, file);
			}
			factory.trimUnresolved();
		}
		if(options.referenceFile.appears()) {
			String file = options.referenceFile.getValue();
			if(file.indexOf('.') == -1) {
				file += ".txt";
			}
			try {
				PrintStream stream = new PrintStream(
								new BufferedOutputStream(
										new FileOutputStream(file)));
				messages.CREATING_INTERNAL_REF.log(logger, file);
				writeReferences(repository, stream);
				stream.close();
			} catch(FileNotFoundException e) {
				messages.NO_INTERNAL_REF.log(logger, file);
			}
		}
		
		
	}

	void printEntry(BT_Item from, String extra, PrintStream stream) {
		if(from == null) {
			if(extra != null) {
				stream.print('\t');
				stream.print(extra);
			}
		} else {
			stream.print('\t');
			stream.print(from.useName());
			BT_Class owner = from.getDeclaringClass();
			if(!owner.isStub() && owner.loadedFrom != null && owner.loadedFrom.length() > 0) {
				stream.print(" [");
				stream.print(owner.loadedFrom);
				stream.print(']');
			}
			if(extra != null) {
				stream.print(' ');
				stream.print(extra);
			}
		}
		stream.println();
	}
	
	static class Entry {
		BT_Item to;
		BT_Item from;
		String extra;
		
		Entry(BT_Item to, BT_Item from, String extra) {
			this.to = to;
			this.from = from;
			if(extra == null) {
				extra = "";
			}
			this.extra = extra;
		}
		
		public int hashCode() {
			return to.hashCode() + (from == null ? 0 : from.hashCode()) + extra.hashCode();
		}
		
		public boolean equals(Object o) {
			if(o instanceof Entry) {
				Entry other = (Entry) o;
				return to.equals(other.to) && ((from == null) ? (other.from == null) : from.equals(other.from)) && extra.equals(other.extra);
			}
			return false;
		}
	}
	
	//Entry lastEntry;
	
	void print(BT_Item to, BT_Item from, String extra, PrintStream stream, HashSet previousEntries, Entry entry, Entry lastEntry) {
//		Entry entry = new Entry(to, from, extra);
//		if(previousEntries == null) {
//			stream.println(to.useName());
//			previousEntries = new HashSet();
//		} else {
			if(lastEntry != null && to.equals(lastEntry.to)) {
				/* avoid printing a duplicate reference */
				if(previousEntries.contains(entry)) {
					return;
				}
			} else {
				/* next "to" entry */
				stream.print(to.useName());
				BT_Class toClass = to.getDeclaringClass();
				if(!toClass.isStub() && toClass.loadedFrom != null && toClass.loadedFrom.length() > 0) {
					stream.print(" [");
					stream.print(toClass.loadedFrom);
					stream.print(']');
				}
				stream.println();
				previousEntries.clear();
			}
//		}
		/* next "from" entry */
		previousEntries.add(entry);
//		if(to.isClassMember()) {
//			stream.print('\t');
//		}
		printEntry(from, extra, stream);
	}
	
	protected void writeReferences(JaptRepository repository, final PrintStream stream) {
		final HashSet previousEntries = new HashSet();
		class MyReferenceSelector implements ReferenceSelector {
			boolean foundRef;
			Entry lastEntry;
			
			public boolean selectReference(BT_Item to, BT_Item from, BT_Attribute att) {
				if(to.getDeclaringClass().equals(from.getDeclaringClass())) {
					return false;
				}
				return foundRef = true;
			}
			
			public void printReference(BT_Item to, BT_Item from, String extra) {
				Entry entry = new Entry(to, from, extra);
				print(to, from, extra, stream, previousEntries, entry, lastEntry);
				lastEntry = entry;
			}
		};
		MyReferenceSelector allSelector = new MyReferenceSelector();
		BT_ClassVector classes = (BT_ClassVector) repository.getInternalClasses().clone();
		classes.sort();
		//Entry lastEntry = null;
		for(int i=0; i<classes.size(); i++) {
			previousEntries.clear();
			allSelector.foundRef = false;
			BT_Class clazz = classes.elementAt(i);
			clazz.printReferences(allSelector);
			if(!allSelector.foundRef) {
				allSelector.printReference(clazz, null, "<none>");//TODO internationalize?
			}
		}
	}
	
	/**
	 * @param repository
	 * @param factory
	 * @param report
	 */
	protected void writeUnresolved(JaptRepository repository, final PrintStream stream) {
		JaptFactory factory = repository.getFactory();
		final HashSet previousEntries = new HashSet();
		
		ReferenceSelector allSelector = new ReferenceSelector() {
			Entry lastEntry;
			
			public boolean selectReference(BT_Item to, BT_Item from, BT_Attribute att) {
				return true;
			}
			
			public void printReference(BT_Item to, BT_Item from, String extra) {
				Entry entry = new Entry(to, from, extra);
				print(to, from, extra, stream, previousEntries, entry, lastEntry);
				lastEntry = entry;
			}
		};
		
		BT_ClassVector unresolvedClasses = factory.getUnresolvedClasses();
		unresolvedClasses.sort();
		for(int i=0; i<unresolvedClasses.size(); i++) {
			BT_Class clazz = unresolvedClasses.elementAt(i);
			previousEntries.clear();
			clazz.printReferences(allSelector);
		}
		
		BT_MethodVector unresolvedMethods = factory.getUnresolvedMethods();
		unresolvedMethods.sort();
		for(int i=0; i<unresolvedMethods.size(); i++) {
			BT_Method method = unresolvedMethods.elementAt(i);
			if(unresolvedClasses.contains(method.getDeclaringClass())) {
				/* already done */
				continue;
			}
			previousEntries.clear();
			method.printReferences(allSelector);
		}
		
		BT_FieldVector unresolvedFields = factory.getUnresolvedFields();
		unresolvedFields.sort();
		for(int i=0; i<unresolvedFields.size(); i++) {
			BT_Field field = unresolvedFields.elementAt(i);
			if(unresolvedClasses.contains(field.getDeclaringClass())) {
				/* already done */
				continue;
			}
			previousEntries.clear();
			field.printReferences(allSelector);
		}
		
		/* 
		 * Now let's look at all fields, classes and methods to find things that are not accessible
		 * 
		 * This also identifies missing items, in the sense that an accessible field or method may
		 * be missing, and this may result in resolving to a method of the same signature or field
		 * of the same name that is inaccessible.
		 */
		ReferenceSelector inaccessibleSelector = new ReferenceSelector() {
			Entry lastEntry;
			
			public boolean selectReference(BT_Item to, BT_Item from, BT_Attribute att) {
				if(from == null || to.isStub() || (att != null && !att.getName().equals(BT_CodeAttribute.ATTRIBUTE_NAME))) {
					return false;
				}
				BT_Class declaringClass = from.getDeclaringClass();
				boolean result = !to.isVisibleFrom(declaringClass);
				return result;
			}
			
			public void printReference(BT_Item to, BT_Item from, String extra) {
				Entry entry = new Entry(to, from, extra);
				print(to, from, "inaccessible " + extra, stream, previousEntries, entry, lastEntry);
				lastEntry = entry;
			}
		};
		BT_ClassVector classes = repository.classes;
		classes.sort();
		for(int i=0; i<classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			if(clazz.isStub()) {
				continue;
			}
			previousEntries.clear();
			clazz.printReferences(inaccessibleSelector);
		}
	}
	
	protected RepositoryLoader createRepositoryLoader(RefReport ref, JaptRepository rep, Logger logger) {
		return new RepositoryLoader(ref, rep, options, logger);
	}
	
	protected RefReport createRefReport(Logger logger, JaptFactory factory) {
		RefReport ref;
		if(options.refTree.appears()) {
			String fileName = options.refTree.getValue();
			try {
				ref = new RefReport(fileName, factory);
				messages.CREATING_REF.log(logger, fileName);
			}
			catch(FileNotFoundException e) {
				messages.NO_REF.log(logger, fileName);
				ref = new RefReport();
			}
		}
		else {
			ref = new RefReport();
		}
		return ref;
	}


	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Component#getName()
	 */
	public String getName() {
		return name;
	}
	
	public void noteExecuting(Logger logger) {}
	
	public void noteExecuted(Logger logger, String timeString) {}

}
