package com.ibm.ive.tools.japt;


import com.ibm.ive.tools.japt.PatternString.PatternStringTriple;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassFileException;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;
import com.ibm.jikesbt.BT_HashedClassVector;
import com.ibm.jikesbt.BT_HashedFieldVector;
import com.ibm.jikesbt.BT_HashedMethodVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Repository;

/**
 * @author sfoley
 */
public class JaptClass extends BT_Class {

	/**
	 * Constructor for JaptClass.
	 * @param repository
	 */
	public JaptClass(BT_Repository repository) {
		super(repository);
	}
	
	public void acquireClassLock() {
		super.acquireClassLock();
	}
	
	public void releaseClassLock() {
		super.releaseClassLock();
	}
	
	/**
	 * Return the set of all superinterfaces of this class or interface, including the
	 * class itself if an interface.
	 */
	public BT_ClassVector getImplementedInterfaces() {
		JaptClass superClass = (JaptClass) getSuperClass();
		BT_ClassVector implementedInterfaces = 
			(superClass == null) ? 
			new BT_HashedClassVector() :
			superClass.getImplementedInterfaces();
		if(isInterface()) {
			implementedInterfaces.addElement(this);
		}
		// Next, count and collect the set of interfaces implemented/extended directly by this class/interface.
		BT_ClassVector parents = getParents();
		for (int i = 0; i < parents.size(); i++) {
			JaptClass parent = (JaptClass) parents.elementAt(i);
			if (!parent.isInterface()) {
				continue;
			}
			BT_ClassVector parentInterfaces = parent.getImplementedInterfaces();
			implementedInterfaces.addUnique(parent);
			for (int j = 0; j < parentInterfaces.size(); j++) {
				implementedInterfaces.addUnique(parentInterfaces.elementAt(j));
			}
		}

		return implementedInterfaces;
	}
	
	/**
	 * @return true if the class implements or the interface extends java.lang.Cloneable
	 */
	public boolean isCloneable() {
		return (getImplementedInterfaces().findClass(BT_Repository.JAVA_LANG_CLONEABLE) != null);
	}
	
	/**
	 * @return true if the class implements or the interface extends java.io.Serializable
	 */
	public boolean isSerializable() {
		return (getImplementedInterfaces().findClass(BT_Repository.JAVA_IO_SERIALIZABLE) != null);
	}
	
	/**
	 * @return true if the class implements or the interface extends java.io.Externalizable
	 */
	public boolean isExternalizable() {
		return (getImplementedInterfaces().findClass(BT_Repository.JAVA_IO_EXTERNALIZABLE) != null);
	}

	
	public void remove() {
		super.remove();		
		RelatedClassCollector collector = ((JaptRepository) repository).removeRelatedClassCollector(this);
		if(collector != null) {
			collector.visitClasses(new RelatedClassCollector.RelatedClassVisitor() {
				public void visit(BT_Class clazz, int relation) {
					((JaptRepository) repository).getRelatedClassCollector(clazz).reset();
				}
			});
		}
		((JaptRepository) repository).removeAccessorMethodGenerator(this);
	}
	
	protected void dereference() throws BT_ClassFileException {
		try {
			super.dereference();
		} finally {
			((JaptFactory) repository.factory).noteClassDereferenced(this);
		}
	}
	
	public BT_FieldVector findFields(Identifier identifier) throws InvalidIdentifierException {
		BT_FieldVector result = new BT_HashedFieldVector();
		PatternString name = identifier.getPattern();
		for(int i=0; i<fields.size(); i++) {
			BT_Field field = fields.elementAt(i);
			if(name.isMatch(field.getName())) {
				result.addElement(field);
			}
		}
		return result;
	}

	public BT_MethodVector findMethods(Identifier identifier) throws InvalidIdentifierException {
		BT_MethodVector result = new BT_HashedMethodVector();
		PatternStringTriple triple = identifier.splitAsMethodNameIdentifier();
		PatternString name = triple.first;
		PatternString sigString = triple.second;
		//triple.third is the return type
		for(int i=0; i<methods.size(); i++) {
			BT_Method method = methods.elementAt(i);
			if(name.isMatch(method.getName())) {
				BT_MethodSignature sig = method.getSignature();
				if(sigString.isMatch(sig.toExternalArgumentString()) //java language style
						|| sigString.isMatch(sig.toParameterString())) {
					result.addElement(method);
				}
			}
		}
		return result;
	}

}
