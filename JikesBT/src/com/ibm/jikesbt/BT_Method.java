package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/*
 Implementation notes:
 - The words "attach" and "detach" are generally used for consistency-preserving public methods, whereas
   "link", "delink", and "relink" are generally not consistency-preserving and are private.

*/

/**
 Represents a method, constructor, or static-initializer of a class.

 <p> See the <a href=../jikesbt/doc-files/UserGuide.html#BT_METHOD>User Guide<a>.
 * @author IBM
**/
public class BT_Method extends BT_Member implements BT_Opcodes {
	
	public static final String STATIC_INITIALIZER_NAME = "<clinit>";
	public static final String INITIALIZER_NAME = "<init>";
	
	/**
	 The method's return and parameter types.
	 Normally accessed via {@link BT_Method#getSignature} and {@link #setSignature} for extensibility.
	**/
	MethodType methodType;
	
	ReentrantLock referencingAttributesLock = new ReentrantLock();
	ReentrantLock callSiteLock = new ReentrantLock();
	ReentrantLock methodKidsLock = new ReentrantLock();
	
	/**
	 Construct classes from classpath using reflection API 
	 for applications that want to use the hosting VM for loading
	 classes, instead of letting JikesBT use its own classpath.
	**/
	public void initializeFrom(Class returnType, Class args[]) {

		BT_ClassVector types = new BT_ClassVector();
		for (int n = 0; n < args.length; n++) {
			BT_Class c = cls.getRepository().forName(toJavaName(args[n]));
			types.addElement(c);
		}

		BT_Class retC = cls.getRepository().forName(toJavaName(returnType));
		methodType = BT_MethodSignature.create(retC, types, cls.getRepository());
	}

	public void initializeFrom(java.lang.reflect.Constructor m) {
		name = INITIALIZER_NAME;
		setFlags((short) m.getModifiers());
		initializeFrom(void.class, m.getParameterTypes());
	}

	/**
	 Construct classes from classpath using reflection API 
	 for applications that want to use the hosting VM for loading
	 classes, instead of letting JikesBT use its own classpath.
	**/
	public void initializeFrom(java.lang.reflect.Method m) {
		name = m.getName();
		setFlags((short) m.getModifiers());
		initializeFrom(m.getReturnType(), m.getParameterTypes());
		Class etypes[] = m.getExceptionTypes();
		for (int n = 0; n < etypes.length; n++)
			addDeclaredException(
				cls.getRepository().forName(etypes[n].getName()));
	}

	public String toJavaName(Class c) {
		if (c.isArray()) {
			String arrayName = c.getName();
			String filteredName = arrayName.replace('.', '/'); 
			//the names of arrays is a hybrid of the class file format name and the java name (go figure!!)
			//so we first translate to class file format and then translate to java
			return BT_ConstantPool.toJavaName(filteredName);
		}
		return c.getName();
	}

	/**
	 Attributes which reference this method, excluding the code attribute (BT_CodeAttribute)
	 and attributes which are contained in the code attribute (BT_LocalVariableAttribute, 
	 BT_LineNumberAttribute, BT_LocalVariableTypeTableAttribute)
	**/
	public BT_AttributeVector referencingAttributes;
	
	/**
	 Instructions that call this method.
	**/
	public BT_MethodCallSiteVector callSites = new BT_MethodCallSiteVector();

	
	/**
	 Overridden and implemented methods.
	 Added if find _some_ path <=> deleted if find _no_ path.
	**/
	final BT_MethodVector parents =
		cls.getRepository().factory.buildMethodRelationships
			? new BT_MethodVector()
			: null;

	boolean addParent(BT_Method parent) {
		return parents.addUnique(parent);
	}
	
	boolean addKid(BT_Method kid) {
		return kids.addUnique(kid);
	}
	
	boolean replaceKid(BT_Method kid, int index) {
		kids.setElementAt(kid, index);
		return true;
	}
	
	/**
	 @return  The methods that this method overrides or implements.
	 They may be in classes or interfaces.
	 They may be indirect class-wise (e.g., in grandparent classes), but must
	 be direct method-wise (i.e., have no intervening overriding methods).
	 See {@link <a href=../jikesbt/doc-files/glossary.html#kid_method>kid method</a>}.
	
	 <p> The collection is updated when classes are read, when {@link
	 BT_MethodVector#refreshInterMethodRelationships} is called, or by
	 directly by the application.
	
	 <p> Like most other references returned by JikesBT, the set
	 returned is part of JikesBT's data model, so updating it will
	 change the parents of this method.
	
	 <p> Must not be called unless {@link BT_Factory#buildMethodRelationships cls.getRepository().factory.buildMethodRelationships} is true.
	**/
	//   Must not be called unless {@link BT_Factory#buildMethodRelationships cls.getRepository().factory.buildMethodRelationships} is true.
	public BT_MethodVector getParents() {
		if (CHECK_USER && !cls.getRepository().factory.buildMethodRelationships)
			assertFailure("! cls.getRepository().factory.buildMethodRelationships");
		return parents;
	}

	/**
	 True if this is overridden or implemented by "kidM".
	 An abbreviation of <code>getKids().contains(kidM)</code>.
	
	 <p> Must not be called unless {@link BT_Factory#buildMethodRelationships cls.getRepository().factory.buildMethodRelationships} is true.
	**/
	public boolean isParentOf(BT_Method kidM) {
		if (CHECK_USER && !cls.getRepository().factory.buildMethodRelationships)
			assertFailure("! cls.getRepository().factory.buildMethodRelationships");
		return kids.contains(kidM);
	}

	/**
	 Overriding and implementing methods.
	 Accessed via {@link BT_Method#getKids}.
	**/
	// Added if find _some_ path <=> deleted if find _no_ path.
	BT_MethodVector kids =
		cls.getRepository().factory.buildMethodRelationships
			? new BT_MethodVector()
			: null;

	/**
	 Returns the methods that override or implement this method.
	 May be in classes or interfaces.
	 May be indirect class-wise (e.g., in grandkid classes), but must be
	 direct method-wise (e.g., a method that overrides a method that
	 overrides this one would not be included).
	 See {@link <a href=../jikesbt/doc-files/glossary.html#kid_method>kid method</a>}.
	
	 <p> The collection is updated when classes are read, when {@link
	 BT_MethodVector#refreshInterMethodRelationships} is called, or by
	 directly by the application.
	
	 <p> The set returned is part of JikesBT's data model, so updating it will
	 change the kids of this method.
	
	 <p> Must not be called unless {@link BT_Factory#buildMethodRelationships cls.getRepository().factory.buildMethodRelationships} is true.
	**/
	//   Must not be called unless {@link BT_Factory#buildMethodRelationships cls.getRepository().factory.buildMethodRelationships} is true.
	public BT_MethodVector getKids() {
		if (CHECK_USER && !cls.getRepository().factory.buildMethodRelationships)
			assertFailure("! cls.getRepository().factory.buildMethodRelationships");
		return kids;
	}

	/**
	 True if this is overrides or implements parentM.
	 An abbreviation of <code>getParents().contains(parentM)</code>.
	
	 <p> Must not be called unless {@link BT_Factory#buildMethodRelationships cls.getRepository().factory.buildMethodRelationships} is true.
	**/
	//   Must not be called unless {@link BT_Factory#buildMethodRelationships cls.getRepository().factory.buildMethodRelationships} is true.
	public boolean isKidOf(BT_Method parentM) {
		if (CHECK_USER && !cls.getRepository().factory.buildMethodRelationships)
			assertFailure("! cls.getRepository().factory.buildMethodRelationships");
		return parents.contains(parentM);
	}

//	/**
//	 Null, or the one method that this method directly overrides in a
//	 superclass (not in an interface or a superinterface).
//	 E.g., always null if this method is in an interface.
//	
//	 @see #getParents()
//	**/
//	//   Must not be called unless {@link BT_Factory#buildMethodRelationships cls.getRepository().factory.buildMethodRelationships} is true.
//	public BT_Method overrides() {
//		if (CHECK_USER && !cls.getRepository().factory.buildMethodRelationships)
//			assertFailure("! cls.getRepository().factory.buildMethodRelationships");
//		for (int i = 0; i < parents.size(); ++i)
//			if (parents.elementAt(i).cls.isClass)
//				return parents.elementAt(i);
//		return null;
//	}

	// Added if find _some_ path <=> deleted if find _no_ path.
	final BT_MethodInlawVector inlaws =
		cls.getRepository().factory.buildMethodRelationships
			? new BT_MethodInlawVector()
			: null;

	/**
	 Returns a <em>set</em> of {@link BT_Method#nlaw}s where each will
	 contain (1) a pair of methods that are an {@link <a
	 href=../jikesbt/doc-files/glossary.html#inlaws>inlaw</a>} (one of which will
	 be this method) and (2) the class that caused this relationship.
	
	 Note that if this method is in a class (not in an interface),
	 then each method in the inlaws set will be in an interface
	 (i.e., one of the two related methods must be in an interface).
	
	 <p> The collection is updated when classes are read, when {@link
	 BT_MethodVector#refreshInterMethodRelationships} is called, or by
	 directly by the application.
	
	 <p> Must not be called unless {@link BT_Factory#buildMethodRelationships cls.getRepository().factory.buildMethodRelationships} is true.
	**/
	//   Must not be called unless {@link BT_Factory#buildMethodRelationships cls.getRepository().factory.buildMethodRelationships} is true.
	public BT_MethodInlawVector getInlaws() {
		if (CHECK_USER && !cls.getRepository().factory.buildMethodRelationships)
			assertFailure("! cls.getRepository().factory.buildMethodRelationships");
		return inlaws;
	}

	
	/**
	 Gets field {@link BT_Method#signature}.
	 Returns null if not yet defererenced.
	**/
	public BT_MethodSignature getSignature() {
		BT_MethodSignature sig = methodType.getSignature();
		methodType = sig;  /* in case not dereferenced yet */
		return sig;
	}

	/**
	 Tests if the signatures have the same name, parameters, and return type.
	**/
	public boolean sigEquals(BT_Method other) {
		return name.equals(other.name) && getDescriptor().equals(other.getDescriptor());
	}

	/**
	 The method's code, including instructions, exceptions caught, ...
	 This is a cache for the value in "attributes".
	**/
	BT_CodeAttribute code_;

	/**
	 Initializes the main fields of a BT_Method and updates the containing class to reference this method.
	 This exists since JikesBT can't invoke a rich constructor directly.
	 {@link BT_Method#createMethod( BT_Class inClass, short flags, BT_MethodSignature sig, String simpleName)} is like a rich constructor.
	**/
	private final void initialize(
		short flags,
		BT_MethodSignature sig,
		String simpleName) {
		setFlags(flags);
		this.name = simpleName;
		setSignature(sig);
		this.cls.methods.addElement(this);
	}

	/**
	 Constructs a method that points to its containing class.
	 The containing class is <em>not</em> updated to reference to this method.
	
	 <p> This is never called by JikesBT except in {@link BT_Factory#createMethod}.
	**/
	protected BT_Method(BT_Class cls) {
		super(cls);
	}

	/**
	 Constructs a method and updates the containing class to reference this method.
	 The containing class is also updated to reference this method.
	
	 <p> This is never called by JikesBT.
	**/
	protected BT_Method(
		BT_Class cls,
		short flags,
		String simpleName,
		BT_MethodSignature sig) {
		super(cls);
		initialize(flags, sig, simpleName);
	}

	/**
	 Constructs a method and updates the containing class to reference this method.
	 E.g., new BT_Method(cls, (short)(BT_Method.PUBLIC+BT_Method.FINAL), "void", "<clinit>", "()", code)
	
	 <p> This is never be called by JikesBT.
	**/
	BT_Method(
		BT_Class cls,
		short flags,
		String returnType,
		String simpleName,
		String args,
		BT_CodeAttribute code) throws  BT_DescriptorException {
		this(cls, flags, returnType, simpleName, args);
		setCode(code);
	}

	/**
	 Constructs a method and updates the containing class to reference this method.
	 The superclass _is_ updated to point to this method.
	 E.g., new BT_Method(cls, (short)(BT_Method.PUBLIC+BT_Method.FINAL), "void", "<clinit>", "()")
	
	 <p> This is never be called by JikesBT because JikesBT always creates BT_Methods by invoking {@link BT_Factory#createMethod(BT_Class)}.
	**/
	protected BT_Method(
		BT_Class cls,
		short flags,
		String returnType,
		String simpleName,
		String args) throws BT_DescriptorException {
		this(
			cls,
			flags,
			simpleName,
			BT_MethodSignature.create(returnType, args, cls.getRepository()));
	}

	/**
	 Constructs a method and updates the containing class to reference this method.
	 E.g., new BT_Method(cls, "void", "<clinit>", "()")
	
	 <p> This is never be called by JikesBT because JikesBT always creates BT_Methods by invoking {@link BT_Factory#createMethod(BT_Class)}.
	**/
	protected BT_Method(
		BT_Class cls,
		String returnType,
		String simpleName,
		String args) throws BT_DescriptorException {
		this(cls, BT_Item.PUBLIC, returnType, simpleName, args);
	}

	/**
	 Constructs a method and updates the containing class to reference this method.
	 E.g., new BT_Method(cls, "void", "<clinit>", "()", code)
	
	 <p> This is never be called by JikesBT because JikesBT always creates BT_Methods by invoking {@link BT_Factory#createMethod(BT_Class)}.
	**/
	protected BT_Method(
		BT_Class cls,
		String returnType,
		String simpleName,
		String args,
		BT_CodeAttribute code) throws  BT_DescriptorException {
		this(cls, BT_Item.PUBLIC, returnType, simpleName, args);
		setCode(code);
	}

	public static BT_Method createMethod(
			BT_Class inClass,
			short flags,
			String returnType,
			String simpleName,
			String args) {
		BT_Method m = inClass.repository.createMethod(inClass);
		BT_MethodSignature sig = BT_MethodSignature.create(returnType, args, inClass.getRepository());
		m.initialize(flags, sig, simpleName);
		return m;
	}
	
	public static BT_Method createMethod(
			BT_Class inClass,
			String returnType,
			String simpleName,
			String args) {
		return createMethod(inClass, PUBLIC, returnType, simpleName, args);
	}
	
	public static BT_Method createMethod(
			BT_Class inClass,
			short flags,
			BT_MethodSignature sig,
			String simpleName) {
		BT_Method m = inClass.repository.createMethod(inClass);
		m.initialize(flags, sig, simpleName);
		return m;
	}
	
	/**
	 Constructs a method and updates the containing class to reference this method.
	 Similar to a constructor, but calls {@link BT_Factory#createMethod} to allocate the object.
	 @deprecated in favour of the same method that takes no code attribute
	**/
	public static BT_Method createMethod(
		BT_Class inClass,
		short flags,
		BT_MethodSignature sig,
		String simpleName,
		BT_CodeAttribute codex /* unused */) {
		return createMethod(inClass, flags, sig, simpleName);
	}

	/*
	 Design observations:
	       - Cases:
	                 P->me->K w/w/o P->K
	         - Branching==1 special case optimizations:
	           - Common
	           - Know there are no alternate paths if branching==1
	           - This is the case when going up the parent chain of
	             superclasses when interfaces are not implemented
	           - This may be the case when going down the kid chain
	             when superclasses have only one subclass, but this
	             case seems too rare for special handling.
	           - Especially useful when none of my kids have >1 parent
	             -- Then know none are pivot points for inlaws, so no inlaws go thru me
	       - Removing me:
	         - Know:
	           - Am removing a method from a class in an unchanged class hierarchy
	           - My old kids are still descendents of my old parents -- not of me
	         - Algs:
	           - Parents/kids:
	             - Link each of my parents (not ancestors) to each of my kids (not descendents)
	             - Delink me
	             - Remember if any of my kids have >1 parent!
	           - Inlaws:
	             - My inlaws will become inlaws of my old (method-) parents
	               -- except if they don't satisfy the direct inheritence test
	             - Other inlaws that didn't exist because they didn't
	               meet the direct inheritence test may in theory meet
	               it now that something's been deleted
	               - That would be those between the ends of now-deleted edges
	                 - There are none of those
	             - Inlaws from my old ancestors down my old class and
	               immediately up elsewhere will be added (if I had >1 parent)
	       - Adding me:
	         - Know:
	           - Am adding a method to a class in an unchanged class hierarchy.
	           - My new parents are a subset of the old parents of my new kids & similarly for my new kids.
	           - The old parent-kid relationships between my new parents my new kids are broken
	             -- except those for which there is another inheritence path
	           - Branching==1 special case optimizations:
	             - If my descendents inherit only via me (not around me),
	               then their old parents==my new ones
	             - If I and my intermediate ancestors inherit only via ancestor GP (not around GP),
	               then __.
	           - Inlaws in parents may be moved down to start at me (deleted from them and added to me)
	             - The ones in which a descendent-class of mine is the cause/marriage/fulcrum/pivot-point (how detect this condition?)
	             -- except that those for which there is another inheritence path are _copied_ (how detect this condition?)
	           - Inlaws in ancestors in which my class is the cause/marriage/fulcrum/pivot-point will be deleted.
	         - Algs:
	           - Parents/kids:
	             - For each new descendent? kid?
	               - If all branching==1
	                 - Just interpose me between it and each of its parents!
	               - Else
	                 - Delink it from each of its parents
	                 - Link it to each of its parents
	             - If all kids were reached w/ branching==1
	               - Done
	             - Else
	               - Link me to my parents w/o need to delete existing links first
	           - Inlaws:
	             - Redo my new parents' (not ancestors) old inlaws (result should be only deleting some)
	               - Scan & mark as new
	               - Delete old ones
	                 - Cannot do better by moving them to me because don't know about inlaws that should have been _copied_,
	                   so have to redo my inlaws anyway
	                   - Actually, might be faster since wouldn't have to update collections as much, but not worth the bother yet.
	             - Redo my inlaws
	             - Branching==1 special case optimization:
	               - If none of the descendents of my class had
	                 (==have) >1 parent, they didn't cause any inlaws,
	                 so none need be deleted, so nothing need be done.
	*/
	public void resetDeclaringClassAndName(BT_Class newC, String newNm) {
		if (cls.getRepository().factory.buildMethodRelationships) {
			BT_MethodRelationships.relinkInlawsOfDeletedMethod(this);
			BT_MethodRelationships.relinkParentsAndKidsOfDeletedMethod(this);
		}

		this.name = newNm;
		this.cls = newC;

		//TODO I don't think the relinking is working...  SF
		if (cls.getRepository().factory.buildMethodRelationships) {
			BT_MethodRelationships.relinkParentsAndKidsOfAddedMethod(this);
			BT_MethodRelationships.relinkInlawsOfAddedMethod(this);
		}
	}

	public void resetDeclaringClass(BT_Class newC) {
		resetDeclaringClassAndName(newC, this.name);
	}

	/**
	 Does a {@link BT_Method#setName} and updates related information.
	 Is a <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>consistency-preserving method</a>
	 (except it won't cause special methods such as "<clinit>",
	 "<init>", "final" to become static or non-static).
	 For example, assumes inheritance is ok -- e.g., if this is static
	 or private, it does not attempt to override another.
	**/
	public void resetName(String newNm) {
		//Note: potentially invalidates Signature attribute
		resetDeclaringClassAndName(this.cls, newNm);
	}

	// ----------------------------------------------------------------------------

	/**
	 Reads the rest of the method from the class file.
	**/
	final void read(DataInputStream dis, BT_ConstantPool pool, BT_Repository repo, LoadLocation loadedFrom)
			throws BT_ClassFileException, IOException {
		try {
			short readFlags = dis.readShort();
			name = pool.getSimpleNameAt(dis.readUnsignedShort());
			// All static initializer flags, except for STRICT, must be ignored, but JikesBT expects at least STATIC to be true.
			if (isStaticInitializer()) {
				setFlags((short) ((readFlags & STRICT) | STATIC));
			} else {
				setFlags(readFlags);
				int allFlags =
					readFlags
						& (PERMISSION_FLAGS
							| STATIC
							| FINAL
							| SYNCHRONIZED
							| NATIVE
							| ABSTRACT
							| STRICT);
				int accessFlags = allFlags & (PERMISSION_FLAGS);
				int otherFlags = allFlags & (PRIVATE | STATIC | FINAL | SYNCHRONIZED | NATIVE | STRICT);
				if ((cls.isInterface() && (allFlags != (ABSTRACT | PUBLIC)))
					|| (cls.isClass()
						&& (!(accessFlags == 0
							|| accessFlags == PUBLIC
							|| accessFlags == PRIVATE
							|| accessFlags == PROTECTED)
							|| (isAbstract() && otherFlags != 0)))
					|| (isConstructor()
						&& (allFlags & ~(PERMISSION_FLAGS | STRICT))
							!= 0)) {
					if (BT_Factory.strictVerification)
						throw new BT_ClassFileException(
							Messages.getString("JikesBT.invalid_access_flags_for_method_{0}_8", name));
				}
			}
			methodType = new UndereferencedMethodType(pool.getMethodDescriptorAt(dis.readUnsignedShort()));
			attributes = BT_AttributeVector.read(dis, pool, this, this, repo, new MemberLocation(loadedFrom));
		} catch(BT_ConstantPoolException e) {
			throw new BT_ClassFileException(e);
		}
		
		// Cache the code attribute
		//
		code_ = (BT_CodeAttribute) attributes.getAttribute(BT_CodeAttribute.ATTRIBUTE_NAME);
		if (cls.inProject() && ((isNative() || isAbstract()) != (code_ == null)))
			throw new BT_ClassFileException(
				BT_CodeAttribute.ATTRIBUTE_NAME
					+ Messages.getString("JikesBT.{0}_attribute_misplaced_or_missing_9"));
	}
	
	class UndereferencedMethodType implements MethodType {
		private String descriptor;
		
		UndereferencedMethodType(String desc) {
			this.descriptor = desc;
		}
		
		public String getDescriptor() {
			return descriptor;
		}
		
		public BT_MethodSignature getSignature() throws BT_DescriptorException {
			BT_Repository repo = cls.getRepository();
			String desc = descriptor;
			BT_Class returnType = repo.linkTo(BT_ConstantPool.getReturnType(desc)); 
			BT_ClassVector args = BT_ConstantPool.linkToArgumentTypes(desc, repo);
			return BT_MethodSignature.create(desc, returnType, args, repo);
		}
		
		public int getArgsSize() {
			String desc = descriptor;
			return BT_ConstantPool.getArgsSize(desc);
		}
		
	}
	
	
	
	static interface MethodType {
		String getDescriptor();
		
		BT_MethodSignature getSignature() throws BT_DescriptorException;
		
		/**
		 @return  The sum of all the parameters sizes in terms of 32-bit words (doubles and longs are 2 words).
		**/
		int getArgsSize();
	}
	
	
	
	public String getDescriptor() {
		return methodType.getDescriptor();
	}

	/**
	 An abbreviation of <code>signature.returnType.name.equals("void")</code>.
	 @return  True if the method returns "void".
	**/
	public boolean isVoidMethod() {
		String desc = getDescriptor();
		return desc.endsWith(")V");
	}

	/**
	 @return  Null if is abstract or native or is in an interface or a
	 {@link <a href=../jikesbt/doc-files/glossary.html#system_class>system class</a>}.
	 Otherwise, should not be null.
	 Related: {@link BT_Method#setCode}, {@link BT_Item#attributes}.
	**/
	public BT_CodeAttribute getCode() {
		return code_;
	}
	
	/**
	 @return  The sum of all the parameter sizes including the "this" parameter if the method is not static.
	**/
	public int getArgsSize() {
		int size = methodType.getArgsSize();
		if(!isStatic()) {
			size++;
		}
		return size;
	}

	/**
	 Sets field {@link BT_Method#signature}.
	**/
	public void setSignature(BT_MethodSignature newSignature) {
		//note this potentially invalidates RuntimeVisibleParameterAnnotations and RuntimeInvisibleParameterAnnotations attributes
		removeSignatureReferences();
		methodType = newSignature;
		addSignatureReferences();
		if(code_ != null) {
			code_.resetArgs();
		}
	}
	
	/**
	 This <em>must</em> be used to set a method's Code attribute instead of
	 updating {@link BT_Item#attributes} directly.
	 Related: {@link BT_Method#getCode}.
	 
	 This method does not empty the attributes and instructions from the existing code.
	**/
	public void setCode(BT_CodeAttribute newCode) {
		if (code_ != null) {
			attributes.removeElement(code_);
			code_.setMethod(null);
		}
		code_ = newCode;
		if (newCode != null) {
			attributes.addElement(newCode);
			newCode.setMethod(this);
		}
	}

	/**
	 Removes any {@link BT_CodeAttribute} attribute and declares the method abstract.
	**/
	public void removeCode() {
		if (code_ != null) {
			code_.remove();
			setCode(null);
		}
		becomeAbstract();
	}
	
	/**
	 A method with a body cannot be abstract or native
	 **/
	public void signalNewBody() {
		disableFlags((short) (ABSTRACT | NATIVE));
	}

	/**
	 Replaces any code in the method by a return of 0 or null (or void).
	 Sets {@link BT_CodeAttribute#maxStack}.
	**/
	public void makeCodeSimplyReturn() {
		boolean withStackMaps;
		if (code_ != null) {
			withStackMaps = code_.hasStackMaps();
			code_.removeContents();
		} else {
			withStackMaps = getVersion().shouldHaveStackMaps();
			signalNewBody();
		}
		BT_Ins ins[];
		BT_MethodSignature signature = getSignature();
		if(isVoidMethod()) {
			ins = new BT_Ins[] {
				BT_Ins.make(signature.returnType.getOpcodeForReturn())
			};
		} else {
			ins = new BT_Ins[] {
				BT_Ins.make(signature.returnType.getOpcodeForReturnValue()),
				BT_Ins.make(signature.returnType.getOpcodeForReturn())
			};
		}
		setCode(new BT_CodeAttribute(ins, withStackMaps));
	}
	
	public boolean simplyReturns() {
		if(code_ == null) {
			return false;
		}
		BT_InsVector ins = code_.getInstructions();
		if(isVoidMethod()) {
			return ins.size() == 1 && ins.firstElement().isReturnIns();
		}
		return ins.size() == 2 && ins.elementAt(1).isReturnIns();
	}

	/**
	 Replaces any code in the method by throw new VerifyError().
	 Dereferences the new code.
	 Sets {@link BT_CodeAttribute#maxStack}.
	**/
	public void makeCodeThrowVerifyError() {
		makeCodeThrowError(BT_Repository.JAVA_LANG_VERIFY_ERROR);
	}

	/**
	 Replaces any code in the method by throw the given Error.
	 If the requested Error class does not exist in the run time library linking against
	 then a plain java.lang.Error will be thrown.
	 Dereferences the new code.
	 Sets {@link BT_CodeAttribute#maxStack}.
	**/
	public void makeCodeThrowError(String errorClassName) {
		boolean withStackMaps;
		if (code_ != null) {
			code_.removeContents();
			withStackMaps = code_.hasStackMaps();
		}
		else {
			signalNewBody();
			withStackMaps = getVersion().shouldHaveStackMaps();
		}
		String methodName = INITIALIZER_NAME;
		BT_MethodSignature signature = cls.getRepository().basicSignature;
		BT_Class exceptionClass = cls.getRepository().forName(errorClassName);
		if(exceptionClass.isStub() && !errorClassName.equals(BT_Repository.JAVA_LANG_ERROR)) {
			exceptionClass = cls.getRepository().forName(BT_Repository.JAVA_LANG_ERROR);
		}
		BT_Method initMethod =
			exceptionClass.findMethodOrNull(methodName, signature);
		if (initMethod == null) {
			initMethod = exceptionClass.addStubMethod(methodName, signature);
		}
		BT_Ins ins[] =
			{
				new BT_NewIns(opc_new, exceptionClass),
				BT_Ins.make(BT_Ins.opc_dup),
				BT_Ins.make(BT_Ins.opc_invokespecial, initMethod),
				BT_Ins.make(BT_Ins.opc_athrow),
				};
		BT_CodeAttribute code = new BT_CodeAttribute(ins, withStackMaps);
		setCode(code);
	}

	/**
	 Updates this BT_Method object to directly reference related objects
	 (instead of using class file artifacts such as indices and
	 offsets to identify them).
	 This is used internally while JikesBT reads class files.
	
	 Links this method to other methods "below it" with which it has
	 a relationship (such as implemented and overridden methods), and
	 builds the instructions for the method body from the bytecodes.
	
	 @see <a href=../jikesbt/doc-files/ProgrammingPractices.html#dereference_method>dereference method</a>
	**/
	protected void dereference() throws BT_ClassFileException {
		if (CHECK_USER && !cls.inProject())
			expect(Messages.getString("JikesBT.Not_in_the_project__{0}_22", this));
		
		try {
			methodType = methodType.getSignature();
		} catch(BT_DescriptorException e) {
			throw new BT_ClassFileException(e);
		}
		addSignatureReferences();
		BT_Repository repo = cls.getRepository();
		if (code_ != null) {
			boolean dereferenced = false;
			try {
				code_.dereference(repo);
				dereferenced = true;
			} finally {
				if(!dereferenced) setThrowsVerifyErrorTrue();
			}
		}
		
		for(int i=0; i<attributes.size(); i++) {
			BT_Attribute att = attributes.elementAt(i);
			if(code_ != att) {
				//TODO this code can be shared with the same in BT_AttributeVector
				try {
					att.dereference(repo);
				} catch(BT_AttributeException e) {
					try {
						repo.factory.noteAttributeLoadFailure(repo, this, att.getName(), att, e, att.loadedFrom);
					} finally {
						//we remove the invalid attribute
						attributes.removeElementAt(i--);
					}
				} catch(BT_ClassFileException e) {
					setThrowsVerifyErrorTrue();
					throw e;
				}
			}
		}
	}
	
	private void removeSignatureReferences() {
		if(methodType == null) {
			return;
		}
		BT_MethodSignature signature = getSignature();
		short j=0;
		for(; j<signature.types.size(); j++) {
			BT_Class sigType = signature.types.elementAt(j);
			sigType.removeReferencingSignature(this, j);
		}
		BT_Class sigType = signature.returnType;
		sigType.removeReferencingSignature(this, j);
	}
	
	private void addSignatureReferences() {
		if(!cls.repository.factory.trackClassReferences) {
			return;
		}
		BT_MethodSignature signature = getSignature();
		short j = 0;
		for(; j<signature.types.size(); j++) {
			BT_Class sigType = signature.types.elementAt(j);
			sigType.addReferencingSignature(this, j);
		}
		BT_Class sigType = signature.returnType;
		sigType.addReferencingSignature(this, j);
	}
	
	/**
	 Removes any relationships established when dereferenced.  Also removes
	 some relationships established when created, namely the visibility of the method
	 as a child method, parent method or inlaw method to its parents, children and inlaws respectively.
	 **/
	public void remove() {
		
		removeSignatureReferences();
		
//		for(int n=0; n<parents.size(); n++) {
//			BT_Method parent = parents.elementAt(n);
//			BT_MethodVector parentKids = parent.getKids();
//			parentKids.removeElement(this);
//			for(int m=0; m<kids.size(); m++) {
//				parent.addKid(kids.elementAt(m));
//			}
//		}
//		
//		for(int n=0; n<kids.size(); n++) {
//			BT_Method kid = kids.elementAt(n);
//			BT_MethodVector kidParents = kid.getParents();
//			kidParents.removeElement(this);
//			for(int m=0; m<parents.size(); m++) {
//				kid.addParent(parents.elementAt(m));
//			}
//		}
//		
//		for(int n=0; n<inlaws.size(); n++) {
//			BT_MethodInlaw methodInlaw = inlaws.elementAt(n);
//			BT_Method inlaw = methodInlaw.getOtherMethod(this);
//			inlaw.getInlaws().removeElement(methodInlaw);	
//		}
		
		//TODO removing the method can introduce new inlaws, maybe should use BT_MethodRelationships.relinkInlawsOfDeletedMethod
		//TODO added the two below to handle above TODO and also removed the three for blocks above
		BT_MethodRelationships.relinkInlawsOfDeletedMethod(this);
		BT_MethodRelationships.relinkParentsAndKidsOfDeletedMethod(this);
		
		if (code_ != null) {
			// remove instructions to back out relationships
			code_.remove();
			setCode(null);
		}
		
		for(int i=callSites.size() - 1; i>= 0; i--) {
			BT_MethodCallSite site = callSites.elementAt(i);
			//this instruction removal does not remove the instruction from the code of the other method, 
			//instead it backs out all relationships created during dereferencing,
			//such as the storage of the call site in the code attribute
			site.instruction.unlink(site.from);
			//the instruction target remains the same and will throw an error if executed
		}
		if(referencingAttributes != null) {
			BT_AttributeVector referencingAttributes = (BT_AttributeVector) this.referencingAttributes.clone();
			for(int i=0; i<referencingAttributes.size(); i++) {
				BT_Attribute att = referencingAttributes.elementAt(i);
				att.removeReference(this);
			}
		}
		attributes.remove();
		
		cls.methods.removeElement(this);
		cls.repository.removeMethod(this);
		
		setStub(true);
	}
	
	/**
	 * trims all vectors related to this method that grow as new classes are loaded.  Calling this method
	 * when all loading is complete will release unused memory.
	 */
	public void trimToSize() {
		callSites.trimToSize();
		if(inlaws != null) {
			inlaws.trimToSize();
		}
		if(kids != null) {
			kids.trimToSize();
		}
		if(referencingAttributes != null) {
			referencingAttributes.trimToSize();
		}
	}

	public void removeDeclaredException(BT_Class exceptionClass) {
		BT_ExceptionsAttribute ea = (BT_ExceptionsAttribute)
			attributes.getAttribute(BT_ExceptionsAttribute.ATTRIBUTE_NAME);
		if(ea != null) {
			BT_ClassVector exceptions = ea.declaredExceptions;
			if(exceptions != null && exceptions.removeElement(exceptionClass)) {
				exceptionClass.removeReferencingAttribute(ea);
				if(exceptions.isEmpty()) {
					ea.remove();
				}
			} 
		}
	}
	
	public BT_Class[] getDeclaredExceptions() {
		BT_ExceptionsAttribute ea = (BT_ExceptionsAttribute)
			attributes.getAttribute(BT_ExceptionsAttribute.ATTRIBUTE_NAME);
		return (ea != null) ? ea.declaredExceptions.toArray()
					: BT_ClassVector.emptyVector.toArray();
	}
	
	public BT_ClassVector getDeclaredExceptionsVector() {
		BT_ExceptionsAttribute ea = (BT_ExceptionsAttribute)
			attributes.getAttribute(BT_ExceptionsAttribute.ATTRIBUTE_NAME);
		return (ea != null) ? ea.declaredExceptions : null;
	}
	
	public void addDeclaredExceptions(BT_Class exceptions[]) {
		BT_ClassVector classes;
		BT_ExceptionsAttribute ea = (BT_ExceptionsAttribute)
			attributes.getAttribute(BT_ExceptionsAttribute.ATTRIBUTE_NAME);
		if(ea == null) {
			classes = new BT_ClassVector(exceptions.length);
			ea = new BT_ExceptionsAttribute(classes, this);
			attributes.addElement(ea);
			classes.addAll(exceptions);
		} else {
			classes = ea.declaredExceptions;
			if(classes == null) {
				classes = new BT_ClassVector(exceptions.length);
				ea.declaredExceptions = classes;
				classes.addAll(exceptions);
			} else {
				classes.addAllUnique(exceptions);
			}
		}
	}
	
	public void addDeclaredException(BT_Class clazz) {
		BT_ClassVector classes;
		BT_ExceptionsAttribute ea = (BT_ExceptionsAttribute)
			attributes.getAttribute(BT_ExceptionsAttribute.ATTRIBUTE_NAME);
		if(ea == null) {
			classes = new BT_ClassVector(1);
			ea = new BT_ExceptionsAttribute(classes, this);
			attributes.addElement(ea);
			classes.addElement(clazz);
		} else {
			classes = ea.declaredExceptions;
			if(classes == null) {
				classes = new BT_ClassVector(1);
				ea.declaredExceptions = classes;
				classes.addElement(clazz);
			} else {
				classes.addUnique(clazz);
			}
		}
	}


	/**
	 True if this method is declared abstract as determined by {@link BT_Item#flags}.
	**/
	public boolean isAbstract() {
		return areAnyEnabled(ABSTRACT);
	}

	/**
	 Returns true if this method is "static void main(String[])".
	**/
	public boolean isMain() {
		if(!isStatic() || !name.equals("main")) {
			return false;
		}
		return getDescriptor().equals("([Ljava/lang/String)V");
	}

	/**
	 True if this method is declared synchronized as determined by {@link BT_Item#flags}.
	**/
	public boolean isSynchronized() {
		return areAnyEnabled(SYNCHRONIZED);
	}
	
	/**
	 True if this method is declared strict as determined by {@link BT_Item#flags}.
	**/
	public boolean isStrict() {
		return areAnyEnabled(STRICT);
	}

	/**
	 True if this method is declared native as determined by {@link BT_Item#flags}.
	**/
	public boolean isNative() {
		return areAnyEnabled(NATIVE);
	}

	public void becomeStatic() {
		BT_Class clazz = getDeclaringClass();
		if(BT_Factory.multiThreadedLoading) {
			clazz.classLock.lock();
		}
		super.becomeStatic();
		if(BT_Factory.multiThreadedLoading) {
			clazz.classLock.unlock();
		}
	}
	
	/**
	 Adds a call site to {#callSites} if it is unique.
	**/
	public void addReferencingAttribute(BT_Attribute att) {
		if(!cls.getRepository().factory.trackClassReferences) {
			return;
		}
		if(BT_Factory.multiThreadedLoading) {
			referencingAttributesLock.lock();
		}
		if(referencingAttributes == null) {
			referencingAttributes = new BT_AttributeVector();
			referencingAttributes.addElement(att);
		} else {
			BT_Attribute foundAtt = findReferencingAttribute(att);
			if(foundAtt == null) {
				referencingAttributes.addElement(att);
			}
		}
		if(BT_Factory.multiThreadedLoading) {
			referencingAttributesLock.unlock();
		}
	}
		
	BT_Attribute findReferencingAttribute(BT_Attribute att) {
		if(referencingAttributes != null) {
			for (int n = referencingAttributes.size() - 1; n >= 0; n--) {
				if (referencingAttributes.elementAt(n) == att) {
					return referencingAttributes.elementAt(n);
				}
			}
		}
		return null;
	}
	
	public void removeReferencingAttribute(BT_Attribute att) {
		if(referencingAttributes != null) {
			for (int n = referencingAttributes.size() - 1; n >= 0; n--) {
				if (referencingAttributes.elementAt(n) == att) {
					referencingAttributes.removeElementAt(n);
					return;
				}
			}
		}
	}
	
	/**
	 Adds a call site to {#callSites} if it is unique.
	**/
	public BT_MethodCallSite addCallSite(
		BT_MethodRefIns ins,
		BT_CodeAttribute caller) {
		if (!cls.repository.factory.buildMethodRelationships) {
			return null;
		}
		if(BT_Factory.multiThreadedLoading) {
			callSiteLock.lock();
		}
		BT_MethodCallSite result = findCallSite(ins);
		if(result == null) {
			result = new BT_MethodCallSite(caller, ins);
			callSites.addElement(result);
		}
		if(BT_Factory.multiThreadedLoading) {
			callSiteLock.unlock();
		}
		return result;
	}
	
	BT_MethodCallSite findCallSite(BT_MethodRefIns ins) {
		for (int n = callSites.size() - 1; n >= 0; n--) {
			if (callSites.elementAt(n).instruction == ins) {
				return callSites.elementAt(n);
			}
		}
		return null;
	}
	
	public void removeCallSite(BT_Ins ins) {
		for (int n = callSites.size() - 1; n >= 0; n--)
			if (callSites.elementAt(n).instruction == ins) {
				callSites.removeElementAt(n);
				return;
			}
	}
	

	public BT_MethodCallSite findCallSite(BT_Ins ins) {
		for (int n = callSites.size() - 1; n >= 0; n--)
			if (callSites.elementAt(n).instruction == ins)
				return callSites.elementAt(n);
		return null;
	}
	
	public void setSynchronized(boolean synch) {
		if(synch) {
			enableFlags(SYNCHRONIZED);
		} else {
			disableFlags(SYNCHRONIZED);
		}
	}

	/**
	 Returns true if this Method is a constructor.
	 I.e., if its name is "&lt;init&gt;".
	**/
	public boolean isConstructor() {
		return isContructorName(name);
	}
	
	public static boolean isContructorName(String name) {
		return name.equals(INITIALIZER_NAME);
	}

	/**
	 Returns true if this Method is a static initializer.
	 I.e., if its name is "&lt;clinit&gt;".
	**/
	public boolean isStaticInitializer() {
		return name.equals(STATIC_INITIALIZER_NAME);
	}

	/**
	 Returns true if this Method is a finalizer.
	 I.e., if its name is "finalize", it is not static and it has no arguments.
	**/
	public final boolean isFinalizer() {
		if(isStatic() || !name.equals("finalize")) {
			return false;
		}
		return getDescriptor().equals("()V");
	}
	
	/**
	 Returns true if this Method is not a class method (not static).
	 initializers, or a constructor.
	**/
	public boolean isInstanceMethod() {
		return !isConstructor() && !isStatic();
	}

	/**
	 Returns true if this Method is a class (i.e., static) method.
	 Note that static initializers and constructors are not considered class
	 methods.
	**/
	public boolean isClassMethod() {
		return !isStaticInitializer() && isStatic();
	}

	/**
	 True if this method can be overridden by another method.
	 I.e., is not static, final, private, nor a constructor nor static-initializer, and not in a final class.
	**/
	public boolean canBeInherited() {
		return !isStatic()
			&& !isPrivate()
			&& !isConstructor()
			&& !isStaticInitializer()
			&& !isFinal()
			&& !cls.isFinal();
	}

	/**
	 Can this method override another method?
	 I.e., is it not static, private, nor a constructor nor static-initializer?
	**/
	public boolean canInherit() {
		return !isStatic()
			&& !isPrivate()
			&& !isConstructor()
			&& !isStaticInitializer();
	}

	/**
	 Returns the opcode to be used for calling this method.
	 Note that if invokevirtual is returned, the method may still be called
	 from a subclass method using invokespecial.
	**/
	public int getOpcodeForInvoke() {
		if (isConstructor())
			return opc_invokespecial;
		if (isStatic())
			return opc_invokestatic;
		if (isPrivate())
			return opc_invokespecial;
		if (cls.isInterface)
			return opc_invokeinterface;
		return opc_invokevirtual;
	}

	void resolve() throws BT_ClassWriteException {
		cls.pool.indexOfUtf8(name);
		cls.pool.indexOfUtf8(getDescriptor());
		resolveFlags();
		attributes.resolve(this, cls.pool);
	}

	/**
	 Writes this method to the given output stream. Called when a class is written.
	 To initialize the constant pool, {@link BT_Method#resolve} should first be called.
	 Note: stubs are not written
	**/
	/*
	         method_info {
	           u2 access_flags;
	           u2 name_index;
	           u2 descriptor_index;
	           u2 attributes_count;
	           attribute_info attributes[attributes_count];
	         }
	*/
	public void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException, BT_ClassWriteException {
		if (isStub())
			return;
		if (BT_Misc.overflowsUnsignedByte(getSignature().getArgsSize() + (isStatic() ? 0 : 1))) {/* section 4.10 of vmspec */
			throw new BT_ClassWriteException(Messages.getString("JikesBT.{0}_count_too_large_109", "parameters"));
		}
		
		dos.writeShort(getFlags());

		dos.writeShort(pool.indexOfUtf8(name)); // name_index
		dos.writeShort(pool.indexOfUtf8(getDescriptor()));
		// descriptor_index

		attributes.write(dos, pool);
	}

	/**
	 Prints the method including its code.
	
	 @param  printFlags  The sum of some of:
	   {@link BT_Misc#PRINT_NO_CODE} and
	   {@link BT_Misc#PRINT_ZERO_OFFSETS}.
	   Other bits are ignored.
	**/
	public void print(PrintStream ps, int printFlags, BT_SourceFile sourceFile) {
		boolean isAssemblerMode = (printFlags & BT_Misc.PRINT_IN_ASSEMBLER_MODE) != 0;
		BT_MethodSignature signature = getSignature();
		String keywordString = isAssemblerMode ? modifierString() : keywordModifierString();
		if(keywordString.length() > 0) {
			keywordString += " ";
		}
		ps.print("\t" + keywordString
				+ signature.returnType.name
				+ " "
				+ getName() + "(" + signature.toExternalArgumentString() + ")");
		
		BT_Class exceptions[] = getDeclaredExceptions();
		if (exceptions.length > 0) {
			ps.print(" throws ");
			for (int n = 0; n < exceptions.length; n++) {
				if (n > 0)
					ps.print(", ");
				ps.print(exceptions[n].fullName());
			}
		}
		

		if(isAbstract() || isNative()) {
			ps.println(";");
		}
		else 
			ps.println();
		
		
		// kidC implements parentI
		// kidC extends parentC
		// kidI extends parentI
		// Sort them to make file comparisons clearer ...
		StringVector sort = new StringVector();

		if (!isAssemblerMode && cls.getRepository().factory.buildMethodRelationships) {
			for (int n = 0; n < parents.size(); n++) {
				String rel =
					cls.isInterface == parents.elementAt(n).cls.isInterface
						? Messages.getString("JikesBT.Overrides_29")
						: Messages.getString("JikesBT.Implements_30");
				sort.addElement(
					"\t\t// "
						+ rel
						+ " "
						+ parents.elementAt(n).fullName());
			}
			sort.print(ps);
			sort.removeAllElements();

			for (int n = 0; n < kids.size(); n++) {
				String rel =
					cls.isInterface == kids.elementAt(n).cls.isInterface
						? Messages.getString("JikesBT.Overridden_34")
						: Messages.getString("JikesBT.Implemented_35");
				sort.addElement(
					"\t\t// "
						+ Messages.getString("JikesBT.{0}_by_{1}_38", new Object[] {rel, kids.elementAt(n).fullName()}));
			}
			sort.print(ps);
			sort.removeAllElements();

			for (int n = 0; n < inlaws.size(); n++)
				sort.addElement(
					"\t\t/"
						+ Messages.getString("JikesBT./_Inlaw_{0}__cause__{1}_40", 
							new Object[] {inlaws.elementAt(n).getOtherMethod(this).fullName(), inlaws.elementAt(n).getCls().fullName()}));
			sort.print(ps);
			sort.removeAllElements();
		}
		if (!isAssemblerMode) {/* assembler does not support reading attributes */
			for (int i = 0; i != attributes.size(); ++i) {
				BT_Attribute att = attributes.elementAt(i);
				if (!att.getName().equals(BT_CodeAttribute.ATTRIBUTE_NAME)) {
					att.print(ps, "\t");
				}
			}
		}
		
		
		if(isAbstract() || isNative()) {
			//ps.println(";"); now we print this above
		} else {
			ps.println("\t{");
			if ((printFlags & BT_Misc.PRINT_NO_CODE) == 0) // Not PRINT_NO_CODE
				if (code_ != null) {
					try {
						code_.print(ps, 
								(printFlags & (BT_Misc.PRINT_ZERO_OFFSETS|BT_Misc.PRINT_IN_ASSEMBLER_MODE)),
								sourceFile);
					} catch(BT_CodeException e) {
						cls.getRepository().factory.noteCodeException(e);
					}
				} else {
					ps.println(Messages.getString("JikesBT._t_t//_method_has_no_code_segment____46"));
				}
			ps.println("\t}");
		}
	}
	/**
	 An abbreviation of {@link BT_Method#print(PrintStream,int) print(ps,0)}.
	**/
	public void print(PrintStream ps, BT_SourceFile sourceFile) {
		print(ps, 0, sourceFile);
	}

	public void print(PrintStream ps) {
		print(ps, 0, null);
	}
	
	protected BT_MethodCallSiteVector getReferencingCallSites() {
		return callSites;
	}
	
	public void printReferences(ReferenceSelector selector) {
		BT_MethodCallSiteVector accessors = getReferencingCallSites();
		for(int j=0; j<accessors.size(); j++) {
			BT_MethodCallSite accessor = accessors.elementAt(j);
			BT_Method from = accessor.getFrom();
			if(selector.selectReference(this, from, accessor.from)) {
				selector.printReference(this, from, accessor.getInstruction().getOpcodeName());
			}
		}
		BT_AttributeVector atts = referencingAttributes;
		if(atts != null) {
			for(int j=0; j<atts.size(); j++) {
				BT_Attribute att = atts.elementAt(j);
				BT_AttributeOwner owner = att.getOwner();
				BT_Item owningItem = owner.getEnclosingItem();
				if(selector.selectReference(this, owningItem, att)) {
					selector.printReference(this, owningItem, att.getName());
				}
			}
		}
	}
	
	/**
	 Get the fully qualified name of this item.
	
	 @return The name of the item in Java language format
	   (e.g., constructor "java.lang.String", or class initializer
	   "java.lang.String.<clinit>", or normal method
	   "java.lang.String.compareToIgnoreCase").
	**/
	public String fullName() {
		if (isConstructor())
			return cls.name;
		else
			return cls.name + "." + name;
	}
	
	
	//TODO get the "source" name and signature
	//here is an example signature: <A::Ljava/lang/annotation/Annotation;>(Ljava/lang/Class<TA;>;)TA;
	//this is the signature from java.lang.Class.getAnnotation(java.lang.Class) return java.lang.annotation
	//or in the javadoc: 
	//public <A extends Annotation> A getAnnotation(Class<A> annotationClass)
	
	//ok, I think we need to get from 
	//(Ljava/lang/Class;)Ljava/lang/Annotation;
	//to
	//<A extends Annotation> (Class<A>)A
	//using
	//<A::Ljava/lang/annotation/Annotation;>(Ljava/lang/Class<TA;>;)TA;
	//or something like that 
	
	//another example:
	//public static Class<?> forName(String className)
	//has signature
	//(Ljava/lang/String;)Ljava/lang/Class<*>;
	
//	public String getSourceUseName() {
//	
//}
//	public String getSourceQualifiedName() {
//		
//	}
//	
//	public String getSourceSignature() {
//		
//	}
	
	/** 
	 * @return string of the form methodName(methodSignature)
	 */
	public String qualifiedName() {
		return name + '(' + getSignature().toExternalArgumentString() + ')';
	}
	
	/**
	 * 
	 * @return method name as it appears in class files, see the java virtual machine spec for details
	 */
	public String internalName() {
		return name + getDescriptor();
	}
	
	/** 
	 * @return string of the form packageName.className.methodName(methodSignature), 
	 */
	public String useName() {
		return fullName() + '(' + getSignature().toExternalArgumentString() + ')';
	}

	StringBuffer flagString(StringBuffer s, short flags, boolean keywordsOnly, boolean modifiersOnly) {
		if ((flags & SYNCHRONIZED) != 0) {
			s.append(SYNCHRONIZED_NAME);
			s.append(' ');
		}
		if(!keywordsOnly) {
			if ((flags & BRIDGE) != 0) {
				s.append(BRIDGE_NAME);
				s.append(' ');
			}
			if ((flags & VARARGS) != 0) {
				s.append(VARARGS_NAME);
				s.append(' ');
			}
		}
		return s;
	}
	
	/**
	 A short description of this object for use in debugging.
	**/
	//   Just identifies the method
	//   (see <a href=../jikesbt/doc-files/ProgrammingPractices.html#toString>toString</a>).
	//   To get more information, you may want to use:
	//   <br> o.signature.returnType.name + " " + o.useName()
	public String toString() {
		return useName();
	}

	/**
	 Replaces the contents of this method by the given method.
	**/
	// stub classes can now be replaced by classes that are read.
	void replaceContents(BT_Method other) {
		super.replaceContents(other);
		setCode(other.code_);
		methodType = other.methodType;
		// Note: callSites, inlaws_, kids_, parents_ should not be replaced because these relations
		// will not change.
	}

	public BT_Method copyMethodTo(BT_Class destClass, boolean makeStatic) {
		if(isNative()) {
			return null;
		}
		if(isAbstract() && makeStatic) {
			//cannot make an abstract method static
			return null;
		}
		if(isSynchronized() && !isStatic() && makeStatic) {
			//the synchronization will break
			return null;
		}
		
		boolean isClassInitializer = isStaticInitializer();
		boolean isConstructor = isConstructor();
		if (isConstructor || isClassInitializer) {
			// Do not create a duplicate method if the code assigns to final fields
			BT_AccessorVector accessedFields = code_.accessedFields;
			int index = accessedFields.size();
			while (--index >= 0) {
				BT_Accessor accessor = accessedFields.elementAt(index);
				if (!accessor.isFieldRead() && accessor.getTarget().isFinal()) {
					return null;		
				}	
			}
		}
	
		//TODO remove stuff that is particular to devirtualization:
		//-the flags: only worry about synchronized and synthetic, the rest stay the same as before
		//-the choice of name
		//-assigning to final fields check
		//-maybe the synchronization check, perhaps could add an argument for that
		//-the null check
		
		// Create a new method
		BT_MethodSignature signature = getSignature();
		BT_ClassVector newMethodArgs = (BT_ClassVector) signature.types.clone();
		if (makeStatic && !isStatic())	{
			// Insert the this pointer.
			newMethodArgs.insertElementAt(cls, 0);
		}
		BT_MethodSignature newMethodSignature = BT_MethodSignature.create(signature.returnType, newMethodArgs, destClass.getRepository());
		String newMethodName;
		if (isClassInitializer)	{
			newMethodName = "$clinit";	
		} else if(isConstructor) {
			newMethodName = BT_Class.classNameWithoutPackage(cls.getName()) + "$init";
		} else {
			newMethodName = getName() +  "_";
		}
		
		while (destClass.findInheritedMethod(newMethodName, newMethodSignature, true) != null) {
			newMethodName += "_";
		}
		
		short newFlags = (short) (BT_Method.SYNTHETIC | 
			(getFlags() & (BT_Method.STRICT | BT_Method.SYNCHRONIZED | BT_Method.ABSTRACT)));
		if(makeStatic) {
			newFlags |= BT_Method.STATIC;
		}
		if (cls.equals(destClass)) {
			newFlags |= (getFlags() & PERMISSION_FLAGS);
		} else {
			newFlags |= BT_Method.PUBLIC;
		}
		
		
		BT_Method newMethod = BT_Method.createMethod(destClass, newFlags, newMethodSignature, newMethodName);
		copyExceptionsAttribute(newMethod);
		
		if(isAbstract() || (code_ == null)) {
			return newMethod;
		}
		
		BT_CodeAttribute newCode = (BT_CodeAttribute) code_.clone();
		newMethod.setCode(newCode);
		
		// insert a null check instruction sequence at the start of the static body, if required.
		if (!isStatic() && makeStatic) {
			BT_BasicBlockMarkerIns blockMarker;
			BT_Ins first = newCode.getFirstInstruction();
			if(!first.isBlockMarker()) {
				blockMarker =  BT_Ins.make();
				newCode.insertInstructionAt(blockMarker, 0);
			} else {
				blockMarker = (BT_BasicBlockMarkerIns) first;
			}
			newCode.insertInstructionsAt(
				new BT_Ins [] {
					BT_Ins.make(BT_Ins.opc_aload, 0),
					new BT_JumpOffsetIns(BT_Ins.opc_ifnonnull, -1, blockMarker),
					BT_Ins.make(BT_Ins.opc_aconst_null),
					BT_Ins.make(BT_Ins.opc_athrow) },
				0);
			
		}
		newCode.resetCachedCodeInfo();
		return newMethod;
	}

	public void copyExceptionsAttribute(BT_Method toMethod) {
		BT_ExceptionsAttribute fromExceptionsAttribute = 
			(BT_ExceptionsAttribute) attributes.getAttribute(BT_ExceptionsAttribute.ATTRIBUTE_NAME);
		if (fromExceptionsAttribute != null) {
			BT_ExceptionsAttribute toExceptionsAttribute = (BT_ExceptionsAttribute) fromExceptionsAttribute.clone();
			toMethod.attributes.addElement(toExceptionsAttribute);
		}
	}
	
	private void clearBody() {
		BT_CodeAttribute code = getCode();
		if(code != null) {
			//code.removeAllInstructions(); was this
			//code.attributes.removeAllElements(); and this
			
			code.remove();
		} else {
			//ensure that the method is not abstract or native
			disableFlags((short)(ABSTRACT | NATIVE));
		}
	}
	
	public void replaceBodyWithMethodCall(BT_Method toMethod) {
		replaceBodyWithMethodCall(toMethod, toMethod.getDeclaringClass(), false);
	}
	
	public void replaceBodyWithSetField(BT_Field toField, BT_Class throughClass) {
		replaceBodyWithFieldAccessor(toField, throughClass, false);
	}
	
	public void replaceBodyWithGetField(BT_Field toField, BT_Class throughClass) {
		replaceBodyWithFieldAccessor(toField, throughClass, true);
	}
	
	/**
	 * The arguments to the get field are the signature arguments
	 * @param toField
	 * @param throughClass
	 * @param isGet
	 */
	private void replaceBodyWithFieldAccessor(BT_Field toField, BT_Class throughClass, boolean isGet) {
		clearBody();
		boolean isStatic = toField.isStatic();
		int insCount = (isStatic ? 0 : 1) + (isGet ? 2 : 3);
		BT_Ins ins[] = new BT_Ins[insCount];
		insCount = 0;
		int localNumber;
		if (!isStatic) {	
			ins[insCount++] = BT_Ins.make(throughClass.getOpcodeForLoadLocal(), 0);
			localNumber = throughClass.getSizeForLocal();
		} else {
			localNumber = 0;
		}
		int opcode;
		if(!isGet) {
			BT_Class fieldType = toField.getFieldType();
			ins[insCount++] = BT_Ins.make(fieldType.getOpcodeForLoadLocal(), localNumber);
			localNumber += fieldType.getSizeForLocal();
			opcode = isStatic ? BT_Ins.opc_putstatic : BT_Ins.opc_putfield;
		} else {
			opcode = isStatic ? BT_Ins.opc_getstatic : BT_Ins.opc_getfield;
		}
		ins[insCount++] = BT_Ins.make(opcode, toField, throughClass);
		ins[insCount++] = BT_Ins.make(getSignature().returnType.getOpcodeForReturn());
		setCode(new BT_CodeAttribute(ins, getVersion()));
	}
	
	/**
	 * Make this method call the given method with the equivalent signature.
	 * This method maps each argument starting from 0 to the args required for the method call.
	 * @param toMethod
	 */
	public void replaceBodyWithMethodCall(BT_Method toMethod, BT_Class throughClass, boolean makeSpecial) {
		clearBody();
		//BT_MethodSignature signature = getSignature();
		BT_MethodSignature toSignature = toMethod.getSignature();
		//TODO verify the signatures match ok - 
	
		// Flush the body of the virtual method and
		// have this virtual method call the new static method
		
		int insCount = (toMethod.isStatic() ? 0 : 1) + toSignature.types.size() + 2;
		BT_Ins ins[] = new BT_Ins[insCount];
		insCount = 0;
		
		// Pass the "this" pointer to non-static methods
		int localNumber = 0;
		if (!toMethod.isStatic()) {	
			ins[insCount++] = BT_Ins.make(throughClass.getOpcodeForLoadLocal(),localNumber);
			localNumber += throughClass.getSizeForLocal();
		}
				
		for (int n = 0; n < toSignature.types.size(); n++) {
			BT_Class argType = toSignature.types.elementAt(n);
			ins[insCount++] = BT_Ins.make(argType.getOpcodeForLoadLocal(), localNumber);
			localNumber += argType.getSizeForLocal();
		}
		
		if(makeSpecial && (toMethod.isStatic() || toMethod.isAbstract())) {
			//TODO also verify that the target is either a constructor or a super class or a method in the current class
			throw new IllegalArgumentException();
		}
		int invokeOpcode = makeSpecial ? 
				BT_Ins.opc_invokespecial : toMethod.getOpcodeForInvoke();
		
		ins[insCount++] = BT_Ins.make(invokeOpcode, toMethod, throughClass);
		ins[insCount++] = BT_Ins.make(toSignature.returnType.getOpcodeForReturn());

		setCode(new BT_CodeAttribute(ins, getVersion()));
		addDeclaredExceptions(toMethod.getDeclaredExceptions());
	}
	
	
	
	public boolean callsMethod(BT_Method targetMethod, boolean strictly, boolean indirectly) {
		return callsMethod(targetMethod, strictly, indirectly, new BT_HashedMethodVector());
	}
	
	public boolean isRecursive(boolean strictly, boolean indirectly) {
		return callsMethod(this, strictly, indirectly);
	}
	
	private boolean callsMethod(BT_Method targetMethod, boolean strictly, boolean indirectly, BT_MethodVector lookedAt) {
		BT_CodeAttribute code = getCode();
		if(code == null) {
			return false;
		}
		BT_MethodCallSiteVector calledMethods = code.calledMethods;
		for (int j = calledMethods.size() - 1; j >= 0; j--) {
			BT_MethodCallSite site = calledMethods.elementAt(j);
			BT_Method target = site.getTarget();
			if(target.isTarget(targetMethod, strictly, indirectly, lookedAt)) {
				return true;
			}
		}
		return false;
	}
	
	protected boolean checkKids(BT_Method targetMethod, boolean indirectly, BT_MethodVector lookedAt) {
		//TODO here we should check both overriding and implementing methods, so we need the related method map,
		//which means we need to override this method in the subclass
		BT_MethodVector targetKids = getKids();
		for(int i=0; i<targetKids.size(); i++) {
			BT_Method targetKid = targetKids.elementAt(i);
			targetKid.isTarget(targetMethod, false, indirectly, lookedAt);
		}
		return false;
	}
	
	private boolean isTarget(BT_Method targetMethod, boolean strictly, boolean indirectly, BT_MethodVector lookedAt) {
		if(lookedAt.contains(this)) {
			return false;
		}
		lookedAt.addElement(this);
		if(equals(targetMethod)) {
			return true;
		}
		if(indirectly) {
			if(callsMethod(targetMethod, strictly, indirectly, lookedAt)) {
				return true;
			}
		}
		if(!strictly && !isStatic() && !isPrivate() /* look at the kids */) {
			if(!targetMethod.isStatic() || indirectly /* the kids will not match if the targetMethod is static */) {
				if(checkKids(targetMethod, indirectly, lookedAt)) {
					return true;
				}
			}
		}
		return false;
	}
}