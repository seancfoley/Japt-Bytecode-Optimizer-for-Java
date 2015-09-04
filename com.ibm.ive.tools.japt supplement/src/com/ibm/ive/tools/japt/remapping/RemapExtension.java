/*
 * Created on Jul 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.remapping;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.PatternString;
import com.ibm.ive.tools.japt.Specifier;
import com.ibm.ive.tools.japt.SpecifierIdentifierPair;
import com.ibm.ive.tools.japt.MemberActor.MemberCollectorActor;
import com.ibm.ive.tools.japt.PatternString.PatternStringPair;
import com.ibm.ive.tools.japt.PatternString.PatternStringQuadruple;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;
import com.ibm.ive.tools.japt.commandLine.options.SpecifierIdentifierOption;
import com.ibm.ive.tools.japt.commandLine.options.SpecifierOption;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_ConstantPool;
import com.ibm.jikesbt.BT_DescriptorException;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_HashedMethodVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;

/**
 * @author sfoley
 *
 * This extension is reponsible for remapping method calls and field accesses.
 * One option of this extension is to map method calls and field accesses to newly created methods and fields.
 * Combining with the inliner would then effectively remove such methods calls and fields accesses.
 * 
 */
public class RemapExtension implements CommandLineExtension {

	
	private final Messages messages = new Messages(this);
	private String name = messages.DESCRIPTION;
	
	/**
	 * method invocations and field accesses to be remapped
	 */
	private SpecifierIdentifierOption methodMappings = new SpecifierIdentifierOption(messages.REMAP_METHOD_LABEL, messages.REMAP_METHOD);
	private SpecifierIdentifierOption fieldMappings = new SpecifierIdentifierOption(messages.REMAP_FIELD_LABEL, messages.REMAP_FIELD);
	private SpecifierIdentifierOption fieldReadMappings = new SpecifierIdentifierOption(messages.REMAP_FIELD_WRITE_LABEL, messages.REMAP_FIELD_READ);
	private SpecifierIdentifierOption fieldWriteMappings = new SpecifierIdentifierOption(messages.REMAP_FIELD_READ_LABEL, messages.REMAP_FIELD_WRITE);
	private SpecifierIdentifierOption instantiationMappings = new SpecifierIdentifierOption(messages.REMAP_INSTANTIATION_LABEL, messages.REMAP_INSTANTIATION);
	{
		methodMappings.resolvableSpecifier =
			fieldMappings.resolvableSpecifier =
			fieldReadMappings.resolvableSpecifier =
			fieldWriteMappings.resolvableSpecifier = false;
		
		//TODO could make this more complex by allowing for resolvable specifiers BUT the mapping
		//is to the target class and not the resolved class
		//SAME goes for identifiers, could make a distinction between how a field/method is accessed,
		//either through the declaring class/interface or a subclass or subinterface
		methodMappings.resolvableIdentifier =
			fieldMappings.resolvableIdentifier =
			fieldReadMappings.resolvableIdentifier =
			fieldWriteMappings.resolvableIdentifier = false;
	}
	/**
	 * use this option when you explicitly want an invokespecial at a particular callsite as opposed to an invokevirtual
	 */
	private SpecifierIdentifierOption specialMethodMappings = new SpecifierIdentifierOption(messages.REMAP_METHOD_SPECIAL_LABEL, messages.REMAP_METHOD_SPECIAL);
	
	
	/**
	 * remap method calls only within the specified classes and/or methods
	 */
	private SpecifierOption withinClass = new SpecifierOption(messages.WITHIN_CLASS_LABEL, messages.WITHIN_CLASS);
	private SpecifierOption withinMethod = new SpecifierOption(messages.WITHIN_METHOD_LABEL, messages.WITHIN_METHOD);
	
	/**
	 * create a new target for remapping targets that do not exist
	 */
	private FlagOption create = new FlagOption(messages.CREATE_LABEL, messages.CREATE);
	
	/**
	 * check that the types of the remapping properly match
	 */
	private FlagOption noCheckTypes = new FlagOption(messages.NO_CHECK_TYPES_LABEL, messages.NO_CHECK_TYPES);
	
	/**
	 * increase class and class member permissions to make a remapping possible if necessary
	 */
	public FlagOption overridePermissions = new FlagOption(messages.EXPAND_PERMISSIONS_LABEL, messages.EXPAND_PERMISSIONS); 
	
	/**
	 * 
	 */
	public RemapExtension() {}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.commandLine.CommandLineExtension#getOptions()
	 */
	public Option[] getOptions() {
		return new Option[] {methodMappings, specialMethodMappings, 
				fieldMappings, fieldReadMappings, fieldWriteMappings, 
				instantiationMappings, withinClass, withinMethod, overridePermissions,
				create, noCheckTypes};
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Extension#execute(com.ibm.ive.tools.japt.JaptRepository, com.ibm.ive.tools.japt.Logger)
	 */
	public void execute(JaptRepository repository, Logger logger)
			throws ExtensionException {
		SpecifierIdentifierPair methodPairs[] = methodMappings.get();
		SpecifierIdentifierPair specialMethodPairs[] = specialMethodMappings.get();
		SpecifierIdentifierPair fieldPairs[] = fieldMappings.get();
		SpecifierIdentifierPair fieldReadPairs[] = fieldReadMappings.get();
		SpecifierIdentifierPair fieldWritePairs[] = fieldWriteMappings.get();
		SpecifierIdentifierPair instantiationPairs[] = instantiationMappings.get();
		if(methodPairs.length == 0 && fieldPairs.length == 0 && specialMethodPairs.length == 0
				&& fieldReadPairs.length == 0 && fieldWritePairs.length == 0 && instantiationPairs.length == 0) {
			return;
		}
		BT_ClassVector containingClasses = null;
		if(withinClass.appears()) {
			containingClasses = new BT_HashedClassVector();
			Specifier[] classSpecifiers = withinClass.getSpecifiers();
			for(int j=0; j<classSpecifiers.length; j++) {
				Specifier classSpecifier = classSpecifiers[j];
				try {
					if(!classSpecifier.isConditional() || classSpecifier.conditionIsTrue(repository)) {
						BT_ClassVector found = repository.findClasses(classSpecifier.getIdentifier(), true);
						containingClasses.addAll(found);
					}
				}
				catch(InvalidIdentifierException e) {
					repository.getFactory().noteInvalidIdentifier(e.getIdentifier());
				}
				
			}
		}
		BT_MethodVector containingMethods = null;
		if(withinMethod.appears()) {
			containingMethods = new BT_HashedMethodVector();
			Specifier[] methodSpecifiers = withinMethod.getSpecifiers();
			for(int j=0; j<methodSpecifiers.length; j++) {
				Specifier methodSpecifier = methodSpecifiers[j];
				try {
					if(!methodSpecifier.isConditional() || methodSpecifier.conditionIsTrue(repository)) {
						MemberCollectorActor actor = new MemberCollectorActor(containingMethods, null);
						repository.findMethods(methodSpecifier.getIdentifier(), actor);
					}
				}
				catch(InvalidIdentifierException e) {
					repository.getFactory().noteInvalidIdentifier(e.getIdentifier());
				}
				
			}
		}
		SiteMapper mapper = new SiteMapper(messages, logger, repository, containingClasses, containingMethods);
		mapper.overridePermissions = overridePermissions.isFlagged();
		mapper.checkTypes = !noCheckTypes.isFlagged();
		doMappings(mapper, logger, methodPairs, METHOD);
		mapper.makeSpecial = true;
		doMappings(mapper, logger, specialMethodPairs, METHOD);
		doMappings(mapper, logger, fieldPairs, FIELD);
		doMappings(mapper, logger, fieldReadPairs, FIELD_READ);
		doMappings(mapper, logger, fieldWritePairs, FIELD_WRITE);
		doMappings(mapper, logger, instantiationPairs, INSTANTIATION);
	}
	
	private static final int FIELD_READ = 0;
	private static final int FIELD_WRITE = 1;
	private static final int METHOD = 2;
	private static final int FIELD = 3;
	private static final int INSTANTIATION = 4;
	
	
	private void doMappings(SiteMapper mapper, Logger logger, SpecifierIdentifierPair[] pairs, int accessType) {
		for(int i=0; i<pairs.length; i++) {
			try {
				switch(accessType) {
					case FIELD_READ:
						mapFields(mapper, logger, pairs[i], true, false);
						break;
					case FIELD_WRITE:
						mapFields(mapper, logger, pairs[i], false, true);
						break;
					case FIELD:
						mapFields(mapper, logger, pairs[i], true, true);
						break;
					case METHOD:
						mapMethods(mapper, logger, pairs[i]);
						break;
					case INSTANTIATION:
						mapReferences(mapper, logger, pairs[i]);
						break;
					default:
						throw new RuntimeException(); //should never reach here
				}
			}
			catch(InvalidIdentifierException e) {
				mapper.repository.getFactory().noteInvalidIdentifier(e.getIdentifier());
			}
		}
	}

	private void mapReferences(SiteMapper mapper, Logger logger, SpecifierIdentifierPair pair) throws InvalidIdentifierException {
		JaptRepository repository = mapper.repository;
		Specifier specifier = pair.specifier;
		if(specifier.isConditional() && !specifier.conditionIsTrue(repository)) {
			return;
		}
		BT_ClassVector specifiedClasses = repository.findClasses(specifier.getIdentifier(), false);		
		if(specifiedClasses.size() == 0) {
			messages.NO_MATCH_FOUND.log(logger, specifier);
			return;
		}
		Identifier ident = pair.identifier;
		BT_ClassVector identifiedClasses = repository.findClasses(ident, false);		
		int size = identifiedClasses.size();
		switch(size) {
		
			case 0:
				if(!create.isFlagged()) {
					messages.NO_TARGET_FOUND.log(logger, ident);
					break;
				}
				
				//create a target for the remapping and then do the remapping
				Creator creator = new Creator("refMappings", repository, logger, messages);
				try {
					if(!ident.getPattern().isRegularString()) {
						messages.TARGET_AMBIGUOUS.log(logger, ident);
						break;
					}
					BT_Class target = creator.createClassTarget(ident, false);
					if(target == null) {
						messages.TARGET_INVALID.log(logger, ident);
					}
					//at this point in time we are mapping only instantiations so we ignore the accessType argument
					mapper.mapClass(specifiedClasses, target);
				}
				catch(InvalidIdentifierException e) {
					messages.TARGET_INVALID.log(logger, ident);
				}
				break;
			case 1: //a single target has been identified
				BT_Class target = identifiedClasses.firstElement();
				//at this point in time we are mapping only instantiations so we ignore the accessType argument
				mapper.mapClass(specifiedClasses, target);
				break;
			default:
				messages.TARGET_NOT_UNIQUE.log(logger, ident);
				break;
		}
		
	}
	

	
	private void mapMethods(SiteMapper mapper, Logger logger, SpecifierIdentifierPair pair) throws InvalidIdentifierException {
		JaptRepository repository = mapper.repository;
		Specifier specifier = pair.specifier;
		if(specifier.isConditional() && !specifier.conditionIsTrue(repository)) {
			return;
		}
		MemberCollectorActor specifierActor = new MemberCollectorActor();
		repository.findMethods(specifier.getIdentifier(), specifierActor);		
		BT_MethodVector specifiedMethods = specifierActor.methods;
		if(specifiedMethods.size() == 0) {
			messages.NO_MATCH_FOUND.log(logger, specifier);
			return;
		}
		
		boolean haveInterfaceMethods = false;
		boolean haveClassMethods = false;
		for(int i=0; i<specifiedMethods.size(); i++) {
			if(specifiedMethods.elementAt(i).cls.isInterface()) {
				haveInterfaceMethods = true;
			} else {
				haveClassMethods = true;
			}
		}
		if(haveClassMethods && haveInterfaceMethods) {
			messages.INCOMPATIBLE_CLASSES.log(logger, specifier);
			return;
		}
		
		MemberCollectorActor identifierActor = new MemberCollectorActor();
		Identifier ident = pair.identifier;
		repository.findMethods(ident, identifierActor);		
		
		BT_MethodVector identifiedMethods = identifierActor.methods;
		int size = identifiedMethods.size();
		switch(size) {
			case 0:
				if(!create.isFlagged()) {
					messages.NO_TARGET_FOUND.log(logger, ident);
					break;
				}
				
				//create a target for the remapping and then do the remapping
				Creator creator = new Creator("methodMappings", repository, logger, messages);
				try {
					if(!ident.getPattern().isRegularString()) {
						messages.TARGET_AMBIGUOUS.log(logger, ident);
						break;
					}
					PatternStringQuadruple[] splitIdentifier = ident.splitAsMethodIdentifier();
					if(splitIdentifier.length == 0) {
						messages.TARGET_INVALID.log(logger, ident);
						break;
					}
					else if(splitIdentifier.length > 1) {
						messages.TARGET_AMBIGUOUS.log(logger, ident);
						break;
					}
					PatternStringQuadruple targetIdentifier = splitIdentifier[0];
					BT_MethodSignature sig;
					PatternString third = targetIdentifier.third;
					PatternString fourth = targetIdentifier.fourth;
					if(third.alwaysMatches() && fourth.alwaysMatches()) {
						//no signature was specified
						//must find a common signature
						sig = creator.getCommonSignature(specifiedMethods);
						if(sig == null) {
							messages.INCOMPATIBLE_TYPES.log(logger, specifier);
							break;
						}
					}
					//a signature was specified
					else if(fourth.alwaysMatches()) {
						//no return type specified, must find a common type
						BT_ClassVector returnTypes = new BT_HashedClassVector();
						for(int i=0; i<specifiedMethods.size(); i++) {
							returnTypes.addUnique(specifiedMethods.elementAt(i).getSignature().returnType);
						}
						BT_Class returnType = creator.getCommonType(returnTypes);
						if(returnType == null) {
							messages.INCOMPATIBLE_TYPES.log(logger, specifier);
							break;
						}
						String argString = '(' + third.getString() + ')';
						try {
							//try internal name specifiers
							sig = BT_MethodSignature.create(argString + BT_ConstantPool.toInternalName(returnType.getName()), repository);
						} catch(BT_DescriptorException e) {
							//now try java name specifiers
							sig = BT_MethodSignature.create(returnType.getName(), argString, repository);
						}
					}
					else if(third.alwaysMatches()) {
						sig = creator.getCommonSignature(specifiedMethods);
						if(sig == null) {
							messages.INCOMPATIBLE_TYPES.log(logger, specifier);
							break;
						}
						BT_Class returnType;
						try {
							//try internal name specifiers
							returnType = repository.forName(BT_ConstantPool.toJavaName(fourth.getString()));
						}
						catch(InternalError e) {
							//now try java name specifiers
							returnType = repository.forName(fourth.getString());
						}
						if(!sig.returnType.equals(returnType)) {
							messages.INCOMPATIBLE_TYPES.log(logger, specifier);
							break;
						}
					}
					else {
						String thirdString = third.getString();
						String argString = '(' + thirdString + ')';
						try {
							//try internal name specifiers
							sig = BT_MethodSignature.create(argString + fourth.getString(), repository);
						} catch(BT_DescriptorException e) {
							//now try java name specifiers
							sig = BT_MethodSignature.create(fourth.getString(), argString, repository);
						}
					}
					BT_Method target = creator.createMethodTarget(targetIdentifier, ident.getFrom(), sig, haveInterfaceMethods);
					if(target != null) {
						mapper.mapMethod(specifiedMethods, target);
					}
				}
				catch(InvalidIdentifierException e) {
					messages.TARGET_INVALID.log(logger, ident);
				}
				break;
			case 1: //a single target has been identified
				BT_Method target = identifiedMethods.firstElement();
				if(target.cls.isInterface()) {
					if(!haveInterfaceMethods) {
						messages.INCOMPATIBLE_CLASSES.log(logger, specifier);
						break;
					}
				} else {
					if(!haveClassMethods) {
						messages.INCOMPATIBLE_CLASSES.log(logger, specifier);
						break;
					}
				}
				mapper.mapMethod(specifiedMethods, target);
				break;
			default:
				messages.TARGET_NOT_UNIQUE.log(logger, ident);
				break;
		}
	
	}
	
	private void mapFields(SiteMapper mapper, Logger logger, SpecifierIdentifierPair pair, boolean read, boolean write) throws InvalidIdentifierException {
		if(!read && !write) {
			return;
		}
		JaptRepository repository = mapper.repository;
		Specifier specifier = pair.specifier;
		if(specifier.isConditional() && !specifier.conditionIsTrue(repository)) {
			return;
		}
		MemberCollectorActor specifierActor = new MemberCollectorActor();
		repository.findFields(specifier.getIdentifier(), specifierActor);		
		BT_FieldVector specifiedFields = specifierActor.fields;
		if(specifiedFields.size() == 0) {
			messages.NO_MATCH_FOUND.log(logger, specifier);
			return;
		}
		MemberCollectorActor identifierActor = new MemberCollectorActor();
		Identifier ident = pair.identifier;
		repository.findFields(ident, identifierActor);		
		BT_FieldVector identifiedFields = identifierActor.fields;
		int size = identifiedFields.size();
		switch(size) {
			case 0:
				if(!create.isFlagged()) {
					messages.NO_TARGET_FOUND.log(logger, ident);
					break;
				}
				
				//create a target for the remapping and then do the remapping
				Creator creator = new Creator("fieldMappings", repository, logger, messages);
					
				try {
					if(!ident.getPattern().isRegularString()) {
						messages.TARGET_AMBIGUOUS.log(logger, ident);
						break;
					}
					PatternStringPair splitIdentifier[] = ident.splitAsMemberIdentifier();
					if(splitIdentifier.length == 0) {
						messages.TARGET_INVALID.log(logger, ident);
						break;
					}
					else if(splitIdentifier.length > 1) {
						messages.TARGET_AMBIGUOUS.log(logger, ident);
						break;
					}
					BT_Class type = creator.getCommonType(specifiedFields);
					if(type == null) {
						messages.INCOMPATIBLE_TYPES.log(logger, specifier);
					}
					else {
						BT_Field target = creator.createFieldTarget(splitIdentifier[0], ident.getFrom(), type);
						if(target != null) {
							mapFieldAccess(mapper, read, write, specifiedFields, target);
						}
					}
				}
				catch(InvalidIdentifierException e) {
					messages.TARGET_INVALID.log(logger, ident);
				}
				break;
			case 1: //a single target has been identified
				BT_Field target = identifiedFields.firstElement();
				mapFieldAccess(mapper, read, write, specifiedFields, target);
				break;
			default:
				messages.TARGET_NOT_UNIQUE.log(logger, ident);
				break;
		}
	
	}

	

	/**
	 * @param mapper
	 * @param read
	 * @param write
	 * @param specifiedFields
	 * @param target
	 */
	private void mapFieldAccess(SiteMapper mapper, boolean read, boolean write, BT_FieldVector specifiedFields, BT_Field target) {
		if(read) {
			if(write) {
				mapper.mapField(specifiedFields, target);
			}
			else {
				mapper.mapFieldReads(specifiedFields, target);
			}
		}
		else {
			mapper.mapFieldWrites(specifiedFields, target);
		}
	}

	/* (non-Javadoc)
	 * @see com.ibm.ive.tools.japt.Component#getName()
	 */
	public String getName() {
		return name;
	}
}
