/*
 * Created on Feb 20, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.out;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.ibm.ive.tools.commandLine.DelimitedListOption;
import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptFactory;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.PatternString;
import com.ibm.ive.tools.japt.Resource;
import com.ibm.ive.tools.japt.Specifier;
import com.ibm.ive.tools.japt.commandLine.options.SpecifierOption;
import com.ibm.jikesbt.BT_Attribute;
import com.ibm.jikesbt.BT_AttributeOwner;
import com.ibm.jikesbt.BT_AttributeVector;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_ClassVersion;
import com.ibm.jikesbt.BT_ClassWriteException;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_CodeException;
import com.ibm.jikesbt.BT_DuplicateClassException;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_GenericAttribute;
import com.ibm.jikesbt.BT_Item;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_StackMapAttribute;
import com.ibm.jikesbt.StringVector;

public abstract class GenerationExtension implements com.ibm.ive.tools.japt.commandLine.CommandLineExtension {
	final protected Messages messages = new Messages(this); 
	protected Logger logger;
	int loaderCounter = 0;
	
	//TODO SEAN reverse these "noStrip" to "strip"
	final public FlagOption noStripMetaFiles = new FlagOption(messages.NO_STRIP_META_LABEL, messages.NO_STRIP_META);
	final public FlagOption noStripDebugInfo = new FlagOption(messages.NO_STRIP_DEBUG_LABEL, messages.NO_STRIP_DEBUG);
	final public FlagOption noStripInfoAttributes = new FlagOption(messages.NO_STRIP_ATTS_LABEL, messages.NO_STRIP_ATTS);
	final public FlagOption noStripAnnotations = new FlagOption(messages.NO_STRIP_ANNOTATIONS_LABEL, messages.NO_STRIP_ANNOTATIONS);
	
	final public SpecifierOption excludeResource = new SpecifierOption(messages.EXCLUDE_RESOURCE_LABEL, messages.EXCLUDE_RESOURCE);
	final public FlagOption includeZipped = new FlagOption(messages.INCLUDE_ZIPPED_LABEL, messages.INCLUDE_ZIPPED);
	final public DelimitedListOption removeAttribute = new DelimitedListOption(messages.REMOVE_ATTRIBUTE_LABEL, messages.REMOVE_ATTRIBUTE, 1);
	final public FlagOption removeStackMaps = new FlagOption(messages.REMOVE_STACKMAPS_LABEL, messages.REMOVE_STACKMAPS);
	final public FlagOption addStackMaps = new FlagOption(messages.ADD_STACKMAPS_LABEL, messages.ADD_STACKMAPS);
	final public FlagOption preverifyForCLDC = new FlagOption(messages.PREVERIFY_LABEL, messages.PREVERIFY);
	final public ValueOption newVersion = new ValueOption(messages.CLASS_VERSION_LABEL, messages.CLASS_VERSION);
	final public FlagOption createAutoLoaders = new FlagOption(messages.CREATE_AUTO_LOADER_LABEL, messages.CREATE_AUTO_LOADER);
	//initialize classes in pre-loader
	final public FlagOption noLoaderInitialize = new FlagOption("xx");//initialize classes in pre-loader
	public FlagOption stripDigest = new FlagOption(messages.STRIP_DIGEST_LABEL, messages.STRIP_DIGEST);
	
	public String loaderPackage;
	BT_ClassVector excludeCandidates;
	
	GenerationExtension() {
		excludeResource.setDelimited(false);
	}
	
	public Option[] getOptions() {
		return new Option[] {
				noStripDebugInfo,
				noStripInfoAttributes,
				noStripAnnotations,
				removeAttribute,
				removeStackMaps,
				addStackMaps,
				preverifyForCLDC,
				excludeResource, 
				includeZipped,
				newVersion,
				createAutoLoaders};
	}
	
	static Option[] combine(Option one[], Option two[]) {
		Option res[] = new Option[one.length + two.length];
		System.arraycopy(one, 0, res, 0, one.length);
		System.arraycopy(two, 0, res, one.length, two.length);
		return res;
	}
	
	private StringVector attributesToRemove;
	private boolean removeDeprecated;
	private boolean removeSynthetic;
	private boolean removeStackMapAtts;
	
	StringVector getAttributesToRemove() {
		if(attributesToRemove == null) {
			StringVector stringVector = new StringVector(
					BT_Attribute.infoAttributes.length 
					+ BT_Attribute.debugAttributes.length
					+ BT_Attribute.annotationAttributes.length
					+ 5);
			if(!noStripDebugInfo.isFlagged()) {
				stringVector.addElements(BT_Attribute.debugAttributes);
			}
			if(!noStripInfoAttributes.isFlagged()) {
				stringVector.addElements(BT_Attribute.infoAttributes);
			}
			if(!noStripAnnotations.isFlagged()) {
				stringVector.addElements(BT_Attribute.annotationAttributes);
			}
			if(removeStackMaps.isFlagged()) {
				stringVector.addElement(BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME);
				stringVector.addElement(BT_StackMapAttribute.CLDC_STACKMAP_NAME);
			}
			String others[] = removeAttribute.getStrings();
			for(int i=0; i<others.length; i++) {
				stringVector.addUnique(others[i]);
			}
			removeDeprecated = stringVector.contains(BT_Attribute.DEPRECATED_ATTRIBUTE_NAME);
			removeSynthetic = stringVector.contains(BT_Attribute.SYNTHETIC_ATTRIBUTE_NAME);
			removeStackMapAtts = stringVector.contains(BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME) || 
					stringVector.contains(BT_StackMapAttribute.CLDC_STACKMAP_NAME);
			stringVector.removeElement(BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME);
			stringVector.removeElement(BT_StackMapAttribute.CLDC_STACKMAP_NAME);
			
			attributesToRemove = stringVector;
		}
		return attributesToRemove;
	}
	
	private boolean shouldBumpUpVersion(BT_Class clazz) {
		return (addStackMaps.isFlagged() && !clazz.getVersion().shouldHaveStackMaps())
				|| (preverifyForCLDC.isFlagged() && !clazz.getVersion().mustHaveStackMaps());
	}
	
	void writeToGenerator(JaptRepository rep,
			Generator generator, 
			BT_ClassVector classes, 
			Resource resources[]) throws IOException {
		BT_ClassVersion requestedClassVersion = null;	
		if(newVersion.appears()) {
			requestedClassVersion = BT_ClassVersion.getVersion(newVersion.getValue());
		}
		getAttributesToRemove();
		for(int i=0; i<classes.size(); i++) {
			BT_Class clazz = classes.elementAt(i);
			if(excludeCandidates != null && excludeCandidates.contains(clazz)) {
				continue;
			}
			
			try {
				if(requestedClassVersion != null) {
					if(requestedClassVersion.exceeds(clazz.getVersion())) {
						clazz.version = requestedClassVersion;
					}
					if(shouldBumpUpVersion(clazz)) {
						if(preverifyForCLDC.isFlagged() && !clazz.getVersion().mustHaveStackMaps() && !clazz.getVersion().olderCLDCStackMaps()) {
							//error: when preverifyForCLDC and version is 49/50, CLDC not supported for these two versions
							messages.VERSION_UNSUPPORTED_CLDC.log(logger, new Object[] {clazz.getVersion(), clazz});
						} else if(addStackMaps.isFlagged() && preverifyForCLDC.isFlagged() && clazz.getVersion().olderCLDCStackMaps()) {
							//error: when both preverifyForCLDC and addStackMaps and version is 48 and under, cannot have both types of stackmaps, so we use only CLDC stackmaps
							messages.CANNOT_HAVE_BOTH_STACKMAPS.log(logger, clazz);
						}
						if(addStackMaps.isFlagged() && !clazz.getVersion().shouldHaveStackMaps()) {
							//warning: addStackMaps and version 49 and under since the stack maps will likely be ignored
							messages.STACK_MAPS_LIKELY_IGNORED.log(logger, clazz);
						}
					}
				} else if(shouldBumpUpVersion(clazz)) {
					clazz.version = preverifyForCLDC.isFlagged() ?
							BT_ClassVersion.STACKMAP_COMPULSORY :
								BT_ClassVersion.STACKMAP_BENEFICIAL;
				}
				fixMiscellaneousByVersion(clazz, rep.getFactory());
				
				//at this point the version number is final
				
				//NOTES:
				//-newVersion: create class files with the max of the existing version and the specified version
				//-removeStackMaps: removes both the older CLDC 1.0 and 1.1 stack maps (StackMap attribute) and the Java SE 6/7/8/Java ME CLDC 8 stack maps (StackMapTable attribute)
				//-addStackMaps: Will bump class file version up to version 50 unless newVersion specified. Will add the Java SE 6/7/8/Java ME CLDC 8 stack maps (StackMapTable attribute).  
				//-preverifyCLDC: Will bump class file version up to version 51 unless newVersion specified.  Will add the Java SE 6/7/8/Java ME CLDC 8 stack maps (StackMapTable attribute) for versions 51 and up and the older CLDC 1.0 and 1.1 stack maps for version 48 and under
	
				//So in summary, if you want to regenerate stack maps, use -removeStackMaps and either -addStackMaps or -preverifyCLDC depending on whether you are using CLDC or Java SE.
				//If in addition they must run on an older VM, specify -newVersion with the intended version.
				
				//Notes:
				//
				//if newVersion not specified:
					//in cases of preverifyForCLDC specified for version 49 or 50, then version bumped up to 51
					//in cases of addStackMaps specified for version 49 and under, then version bumped up to 50
				//if newVersion specified, version will be the max of newVersion and the existing class file version (version number can only be increased)
					//in cases of addStackMaps specified for version 49 and under, Java 6 stackmaps will be added even though version is under 50
					//in cases of preverifyForCLDC specified for versions 49 and 50, it has no effect and an error is shown
					//if both addStackMaps and preverifyForCLDC specified for versions 50 and up, Java 6 stackmaps will be added
					//if both addStackMaps and preverifyForCLDC specified for versions 48 and down, CLDC stackmaps will be added
						//and an error is shown because you cannot have both types of stackmaps in same class file
				//classes with version greater than 50 will always have stack maps
				
				//http://docs.oracle.com/javame/config/cldc/opt-pkgs/api/cldc/api/doc-files/CLDCvm.html
				//https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.10.1
				//https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.10.2

				removeAttributes(clazz);
				addStackMaps(rep, clazz);
				generator.write(clazz);
				messages.WROTE_CLASS.log(logger, clazz.getName());
			} catch(BT_ClassWriteException e) {
				Throwable initial = e.getInitialCause();
				if(initial instanceof BT_CodeException) {
					rep.getFactory().noteCodeException((BT_CodeException) initial);
				} else {
					messages.ERROR_WRITING_CLASS.log(logger, new Object[] {clazz.getName(), initial});
				}
			} catch(BT_CodeException e) {
				rep.getFactory().noteCodeException(e);
				messages.ERROR_WRITING_CLASS.log(logger, new Object[] {clazz.getName(), e});
			}
		}
		topLoop:
		for(int i=0; i<resources.length; i++) {
			Resource resource = resources[i];
			Specifier excludeResources[] = excludeResource.getSpecifiers();
			for(int j=0; j<excludeResources.length; j++) {
				Specifier specifier = excludeResources[j];
				try {
					if(!specifier.isConditional() || specifier.conditionIsTrue(rep)) {
						PatternString pattern = specifier.getIdentifier().getPattern();
						if(pattern.isMatch(resource.getName())) {
							continue topLoop;
						}
					}
				}
				catch(InvalidIdentifierException e) {
					rep.getFactory().noteInvalidIdentifier(e.getIdentifier());
				}
			}
		
			try {
				generator.write(resource);
				messages.WROTE_RESOURCE.log(logger, resource.getName());
			} catch(IOException e) {
				//One possible reason for an error is duplicate resources, 
				//if we are reading from several jars/zips into a single one
		
				//this is why we catch IOExceptions generated from writing resources but we do not
				//catch IOExceptions generated from writing class files
				messages.ERROR_WRITING_RESOURCE.log(logger, new Object[] {resource.getName(), e});
			}
		}
	}
	
	private void fixMiscellaneousByVersion(BT_Class clazz, JaptFactory factory) throws BT_CodeException {
		BT_ClassVersion version = clazz.getVersion();
		if(version.interfacesMustBeAbstract() && clazz.isInterface() && !clazz.isAbstract()) {
			clazz.becomeAbstract();
		}
		if(version.invokeSpecialSemanticsMandatory()) {
			clazz.usesInvokeSpecial = true;
		} else if(version.invokeSpecialSemantics()) {
			if(clazz.isInterface()) {
				if(clazz.usesInvokeSpecial) {
					clazz.usesInvokeSpecial = false;
				}
			} else {
				if(!clazz.usesInvokeSpecial) {
					clazz.usesInvokeSpecial = true;
				}
			}
		}
		if(version.mustInlineJSRs() || addingStackMaps(clazz)) {
			inlineJSRs(clazz, factory);
		}
	}
	
	/*
	 * throws IOException, 
	BT_DuplicateClassException,
	BT_ClassWriteException,
	InvalidIdentifierException
	 */
	
	void writeLoader(Generator generator) throws IOException {
		AutoLoaderCreator loader = generator.loader;
		if(loader != null && loader.classes.size() > 0) {
			String loaderName = "Loader";
			if(loaderPackage != null && loaderPackage.length() > 0) {
				String myLoaderPackage = loaderPackage;
				if(myLoaderPackage.charAt(myLoaderPackage.length() - 1) != '.') {
					myLoaderPackage += '.';
				}
				loaderName = myLoaderPackage + loaderName;
			}
			loaderName += ++loaderCounter;
			try {
				BT_Method loaderMethod = loader.createLoaderMethod(
						loaderName, removeStackMaps.isFlagged(), excludeCandidates);
				if(loaderMethod != null) {
					loader.populateLoaderMethod(loaderMethod, !noLoaderInitialize.isFlagged());
					BT_Class clazz = loaderMethod.getDeclaringClass();
					messages.CREATED_LOADER.log(logger, new Object[] {loaderMethod.qualifiedName(), clazz.getName()});
					generator.write(clazz);
					messages.WROTE_CLASS.log(logger, clazz.getName());
				}
			} catch(BT_ClassWriteException e) {
				Throwable initial = e.getInitialCause();
				if(initial instanceof BT_CodeException) {
					loader.repository.getFactory().noteCodeException((BT_CodeException) initial);
				} else {
					messages.ERROR_WRITING_CLASS.log(logger, new Object[] {loaderName, initial});
				}
			} catch(BT_DuplicateClassException e) {
				messages.ERROR_WRITING_CLASS.log(logger, new Object[] {loaderName, e});
			} catch(InvalidIdentifierException e) {
				loader.repository.getFactory().noteInvalidIdentifier(e.getIdentifier());
				messages.ERROR_WRITING_CLASS.log(logger, new Object[] {loaderName, e});
			}
		}
	}
	
	private void inlineJSRs(BT_Class clazz, JaptFactory factory) throws BT_CodeException {
		BT_MethodVector methods = clazz.getMethods();
		for(int j=0; j<methods.size(); j++) {
			BT_Method method = methods.elementAt(j);
			if(method.isStub() || method.isAbstract() || method.isNative()) {
				continue;
			}
			BT_CodeAttribute code = method.getCode();
			if(code.inlineJsrs()) {
				code.optimizeAndRemoveDeadCode(false);
				factory.noteJSRsInlined(method);
			}
		}
	}
	
	private boolean addingStackMaps(BT_Class clazz) {
		//Note that we already bumped up the version if we could based on whether stackmaps are requested
		if(addStackMaps.isFlagged()) {
			return true;
		}
		BT_ClassVersion version = clazz.getVersion();
		return (version.olderCLDCStackMaps() && preverifyForCLDC.isFlagged()) || 
				version.mustHaveStackMaps() || //note: for this version and up, the class will always have stackmaps.  I can think of no reason to provide an option to forcibly remove them.
				(version.shouldHaveStackMaps() && !removeStackMapAtts);
	}
	
	private void addStackMaps(JaptRepository rep, BT_Class clazz) {
		if(addingStackMaps(clazz)) {
			boolean isCLDCMaps = preverifyForCLDC.isFlagged() && clazz.getVersion().olderCLDCStackMaps();
			
			BT_MethodVector methods = clazz.getMethods();
			for(int j=0; j<methods.size(); j++) {
				BT_Method method = methods.elementAt(j);
				if(method.isStub() || method.isAbstract() || method.isNative()) {
					continue;
				}
				BT_CodeAttribute code = method.getCode();
				//check if we have the right kind of stack map
				if(isCLDCMaps) {
					code.removeStackMaps();
					if(code.getAttributes().getAttribute(BT_StackMapAttribute.CLDC_STACKMAP_NAME) != null) {
						continue;
					}
				} else {
					code.getAttributes().removeAttribute(BT_StackMapAttribute.CLDC_STACKMAP_NAME);
					if(code.hasStackMaps()) {
						continue;
					}
				}
				BT_StackMapAttribute att = isCLDCMaps ? code.addCLDCStackMaps() : code.addStackMaps();
				try {
					att.populate();
					if(att.stackMaps == null) {
						messages.COULD_NOT_CREATE_STACKMAPS.log(logger, method.useName());
						code.removeStackMaps();
						code.getAttributes().removeAttribute(BT_StackMapAttribute.CLDC_STACKMAP_NAME);
					} else if(att.stackMaps.getFrameCount() > 0) {
						messages.ADDED_STACKMAPS.log(logger, new String[] {method.useName(), BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME});
					}
				} catch(BT_CodeException e) {
					rep.getFactory().noteCodeException(e);
					messages.COULD_NOT_CREATE_STACKMAPS.log(logger, method.useName());
					code.removeStackMaps();
					code.getAttributes().removeAttribute(BT_StackMapAttribute.CLDC_STACKMAP_NAME);
				}	
			}
		}
	}
	
	/*
	 * We don't actually remove attributes permanently.  We remove then temporarily
	 * only while we write the classes, then we put the attributes back.  A fix for this
	 * to be done better would be to have more control when writing classes.
	 * 
	 * But overall, we do not want attributes removed permanently when we write classes,
	 * because the Japt tool can continue processing those classes afterwards.
	 */
	
	private void removeAttributes(BT_Class clazz) {
		if((attributesToRemove.size() == 0
						&& !removeStackMapAtts
						&& !removeDeprecated
						&& !removeSynthetic)) {
			return;
		}
		ArrayList itemsRemoved = new ArrayList();
		BT_AttributeVector attributes = clazz.attributes;
		removeDeprecated(clazz, itemsRemoved);
		removeSynthetic(clazz, itemsRemoved);
		removeAttributes(clazz, attributes, itemsRemoved);
		BT_MethodVector methods = clazz.getMethods();
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			removeDeprecated(method, itemsRemoved);
			removeSynthetic(method, itemsRemoved);
			removeAttributes(method, method.attributes, itemsRemoved);
			BT_CodeAttribute code = method.getCode();
			if(code != null) {
				removeStackMaps(method, code, itemsRemoved);
				removeAttributes(code, code.attributes, itemsRemoved);
			}
		}
		BT_FieldVector fields = clazz.getFields();
		for(int i=0; i<fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			removeDeprecated(field, itemsRemoved);
			removeSynthetic(field, itemsRemoved);
			removeAttributes(field, field.attributes, itemsRemoved);
		}
	}
	
	private void removeStackMaps(BT_Method method, BT_CodeAttribute code, ArrayList itemsRemoved) {
		if(removeStackMapAtts) {
			boolean hasCLDCStackMap = code.attributes.contains(BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME);
			boolean hasJava6StackMap = code.hasStackMaps();
			if(method.getVersion().shouldHaveStackMaps() && !addingStackMaps(method.getDeclaringClass())
					&& (hasCLDCStackMap || hasJava6StackMap)) {
				messages.REMOVING_STACKMAPS_WARNING.log(logger, new String[] {method.qualifiedName(), method.getDeclaringClass().getName()});
			}
			if(hasJava6StackMap) {
				code.removeStackMaps();
				messages.ATTRIBUTE_REMOVED.log(logger, new String[] {BT_StackMapAttribute.STACK_MAP_TABLE_ATTRIBUTE_NAME, code.useName()});
			}
			if(hasCLDCStackMap) {
				code.getAttributes().removeAttribute(BT_StackMapAttribute.CLDC_STACKMAP_NAME);
				messages.ATTRIBUTE_REMOVED.log(logger, new String[] {BT_StackMapAttribute.CLDC_STACKMAP_NAME, code.useName()});
			}
		}
	}
	
	private void removeDeprecated(BT_Item item, ArrayList itemsRemoved) {
		if(removeDeprecated && item.isDeprecated() && item.attributes.contains(BT_GenericAttribute.DEPRECATED_ATTRIBUTE_NAME)) {
			item.attributes.removeAttribute(BT_GenericAttribute.DEPRECATED_ATTRIBUTE_NAME);
			messages.ATTRIBUTE_REMOVED.log(logger, new String[] {BT_GenericAttribute.DEPRECATED_ATTRIBUTE_NAME, item.useName()});
		}
	}
	
	private void removeSynthetic(BT_Item item, ArrayList itemsRemoved) {
		if(removeSynthetic && item.isSynthetic() && item.attributes.contains(BT_GenericAttribute.SYNTHETIC_ATTRIBUTE_NAME)) {
			item.attributes.removeAttribute(BT_GenericAttribute.SYNTHETIC_ATTRIBUTE_NAME);
			messages.ATTRIBUTE_REMOVED.log(logger, new String[] {BT_GenericAttribute.SYNTHETIC_ATTRIBUTE_NAME, item.useName()});
		}
	}
	
	/**
	 * @param attributesToRemove
	 * @param attributes
	 */
	private void removeAttributes(
			BT_AttributeOwner owner,
			BT_AttributeVector attributes,
			ArrayList itemsRemoved) {
		if(attributes.size() == 0) {
			return;
		}
		for(int j=0; j<attributesToRemove.size(); j++) {
			String toRemoveName = attributesToRemove.elementAt(j);
			for(int i=0; i<attributes.size(); i++) {
				BT_Attribute element = attributes.elementAt(i);
				if(!element.getName().equals(toRemoveName)) {
					continue;
				}
				attributes.removeElementAt(i--);
				messages.ATTRIBUTE_REMOVED.log(logger, new String[] {element.getName(), owner.useName()});
			}
		}
	}
	
	class ResourceController {
		static final String META_DIR = "META-INF/";
		static final String sigFileExtension = ".SF";
		
		int manifestIndex = -1;
		int signatureFileIndex = -1;
		final Resource initialResources[];
		
		Manifest manifest;
		Resource finalResources[];
		boolean filtered;
		
		ResourceController(Resource resources[]) {
			for(int j=0; j<resources.length; j++) {
				Resource resource = resources[j];
				String name = resource.getName();
				if(name.toUpperCase().startsWith(META_DIR)) {
					if(name.equals(JarFile.MANIFEST_NAME)) {
						manifestIndex = j;
					} else {
						String subFile = name.substring(META_DIR.length());
						if(subFile.toUpperCase().endsWith(sigFileExtension) && subFile.indexOf(JarGenerator.ZIPFILE_SEPARATOR_SLASH) == -1) {
							signatureFileIndex = j;
						}
					}
				}
			}
			this.initialResources = resources;
		}
		
		boolean hasManifest() {
			return manifestIndex >= 0;
		}
		
		Resource[] getResources() {
			filterResources();
			return finalResources;
		}
		
		boolean writeManifest(Generator gen) {
			if(!hasManifest()) {
				return false;
			}
			filterResources();
			try {
				if(manifest != null) { /* use the altered manifest if it is there */
					gen.write(manifest);
				} else if(manifestIndex >= 0) { /* use the original manifest */
					gen.write(initialResources[manifestIndex]);
				}
				messages.WROTE_RESOURCE.log(logger, JarFile.MANIFEST_NAME);
			} catch(IOException e) {
				messages.ERROR_WRITING_RESOURCE.log(logger, new Object[] {JarFile.MANIFEST_NAME, e});
			}
			return true;
		}
		
		private void filterResources() {
			if(filtered) {
				return;
			}
			filtered = true;
			finalResources = initialResources;
			if(!hasManifest()) {
				return;
			}
			Resource dups[] = (Resource[]) initialResources.clone();
			int removedCount = 0;
			try {
				Resource resourceManifest = initialResources[manifestIndex];
				manifest = new Manifest(resourceManifest.getInputStream());
				dups[manifestIndex] = null;
				removedCount++;
				         
				/* remove the digests and the magics from the other atts */
				if(stripDigest.isFlagged()) {
					Map map = manifest.getEntries();
					Iterator entries = map.entrySet().iterator();
					ArrayList entriesToRemove = new ArrayList();
					while(entries.hasNext()) {
						Map.Entry entry = (Map.Entry) entries.next();
						//String name = (String) entry.getKey();
						Attributes atts = (Attributes) entry.getValue();
						Iterator attsIterator = atts.entrySet().iterator();
						StringVector toRemove = new StringVector(); //TODO maybe strings are not what we need, might need Attributes.Name objects
						while(attsIterator.hasNext()) {
							Map.Entry attsEntry = (Map.Entry) attsIterator.next();
							Attributes.Name attsName = (Attributes.Name) attsEntry.getKey();
							String attsNameStr = attsName.toString().toLowerCase();
							if(attsNameStr.indexOf("-digest") != -1 || attsNameStr.equals("magic")) {
								toRemove.addUnique(attsName.toString());
							}
						}
						if(toRemove.size() > 0) {
							if(toRemove.size() == atts.size()) {
								//remove the whole entry
								entriesToRemove.add(entry.getKey());
							} else {
								for(int i=toRemove.size() - 1; i>=0; i--) {
									String string = toRemove.elementAt(i);
									atts.remove(string);//use string or Attributes.Name?
								}
							}
						}
					}
					if(entriesToRemove.size() > 0) {
						for(int i=0; i<entriesToRemove.size(); i++) {
							map.remove(entriesToRemove.get(i));
						}
					}
					if(signatureFileIndex >= 0) {
						/* remove the signature file */
						Resource sigFile = dups[signatureFileIndex];
						dups[signatureFileIndex] = null;
						removedCount++;
						
						/* remove any digital signature files */
						String sigFileName = sigFile.getName();
						String subFile = sigFileName.substring(META_DIR.length());
						String signatureFileBaseName = subFile.substring(0, subFile.length() - sigFileExtension.length());
						for(int j=0; j<dups.length; j++) {
							Resource resource = dups[j];
							if(resource == null) {
								continue;
							}
							String name = resource.getName();
							if(name.toUpperCase().startsWith(META_DIR)) {
								if(subFile.startsWith(signatureFileBaseName) && subFile.indexOf(JarGenerator.ZIPFILE_SEPARATOR_SLASH) == -1) {
									dups[j] = null;
									removedCount++;
								}
							}
						}
					}
				}
			} catch(IOException e) {
				//cannot read the manifest... will output the error later when we try to write it
			}
			if(!noStripMetaFiles.isFlagged()) {
				for(int j=0; j<dups.length; j++) {
					Resource resource = dups[j];
					if(resource == null) {
						continue;
					}
					String name = resource.getName();
					if(name.toUpperCase().startsWith(META_DIR)) {
						dups[j] = null;
						removedCount++;
					}
				}
			}
			Resource res[] = new Resource[dups.length - removedCount];
			for(int i=0, j=0; i<dups.length; i++) {
				Resource dup = dups[i];
				if(dup != null) {
					res[j++] = dup;
				}
			}
			finalResources = res;
		}
	}
}