package com.ibm.jikesbt;


/**
 Represents the relation between a class and a referencing JVM instruction 
 such as instanceof, checkcast, ldc, ldc_w, new, anewarray and multianewarray.
 
 See {@link BT_Class#addCreationSite} and {@link BT_MethodCallSite}.
 @author sfoley
**/
public class BT_ClassReferenceSite extends BT_ItemReference {
	
	/**
	 The referencing instruction.  Either a BT_ClassRefIns, BT_ConstantStringIns or a BT_ConstantClassIns.
	**/
	final public BT_Ins instruction;

	BT_ClassReferenceSite(BT_CodeAttribute referencer, BT_ClassRefIns in1) {
		this(referencer, in1, in1.getTarget());
	}

	BT_ClassReferenceSite(BT_CodeAttribute referencer, BT_ConstantStringIns in1, BT_Class javaLangString) {
		this(referencer, (BT_Ins) in1, javaLangString);
	}
	
	BT_ClassReferenceSite(BT_CodeAttribute referencer, BT_ConstantClassIns in1, BT_Class javaLangClass) {
		this(referencer, (BT_Ins) in1, javaLangClass);
	}
	
	private BT_ClassReferenceSite(BT_CodeAttribute referencer, BT_Ins ins, BT_Class target) {
		super(referencer);
		this.instruction = ins;
		//this.target = target;
	}

	public BT_Class getTarget() {
		if(instruction.isClassRefIns()) {
			BT_ClassRefIns ins = (BT_ClassRefIns) instruction;
			return ins.getTarget();
		}
		if(instruction.isLoadConstantClassIns()) {
			BT_ConstantClassIns ins = (BT_ConstantClassIns) instruction;
			return ins.getJavaLangClass();
		}
		BT_ConstantStringIns ins = (BT_ConstantStringIns) instruction;
		return ins.getJavaLangString();
	}
	
	public BT_Class getClassTarget() {
		return getTarget();
	}
		
	public BT_Ins getInstruction() {
		return instruction;
	}
	
	public boolean isCreationSite() {
		return false;
	}
	
	/**
	 * 
	 * @return whether this reference refers to a class.
	 */
	public boolean isClassReference() {
		return true;
	}

}
