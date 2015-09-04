package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

/**
 Models the "InnerClasses" attribute of the {@link BT_Class} that
 describes how inner-classes are named.
 *
 * Every CONSTANT_Class_info entry in the constant_pool table that
 * represents a class or interface C that is not a package member must
 * have exactly one corresponding entry in the classes array.
 *
 * If a class has members that are classes or interfaces, its
 * constant_pool table (and hence its InnerClasses attribute) must refer
 * to each such member, even if that member is not otherwise mentioned by
 * the class. These rules imply that a nested class or interface member
 * will have InnerClasses information for each enclosing class and for
 * each immediate member.
 *
 * Each classes array entry contains the following four items:
 *
 * inner_class_info_index -- The value of the inner_class_info_index item
 *       must be zero or a valid index into the constant_pool table. The
 *       constant_pool entry at that index must be a CONSTANT_Class_info
 *       (§4.4.1) structure representing C. The remaining items in the
 *       classes array entry give information about C.
 *
 * outer_class_info_index -- If C is not a [package?] member, the value of the
 *       outer_class_info_index item must be zero. Otherwise, the value of
 *       the outer_class_info_index item must be a valid index into the
 *       constant_pool table, and the entry at that index must be a
 *       CONSTANT_Class_info (§4.4.1) structure representing the class or
 *       interface of which C is a [package?] member.
 *
 * inner_name_index -- If C is anonymous, the value of the
 *       inner_name_index item must be zero. Otherwise, the value of the
 *       inner_name_index item must be a valid index into the
 *       constant_pool table, and the entry at that index must be a
 *       CONSTANT_Utf8_info (§4.4.7) structure that represents the
 *       original simple name of C, as given in the source code from which
 *       this class file was compiled.
 *
 * inner_class_access_flags -- The value of the inner_class_access_flags
 *       item is a mask of flags used to denote access permissions to and
 *       properties of class or interface C as declared in the source code
 *       from which this class file was compiled. It is used by compilers
 *       to recover the original information when source code is not
 *       available.
 * <pre>
 * @author IBM
**/

/* 
                InnerClasses_attribute {
                       u2 attribute_name_index;
                       u4 attribute_length;
                       u2 number_of_classes;
                       {
                         u2 inner_class_info_index;                     // a class's encoded shortName
                         u2 outer_class_info_index;                     // its defining scope
                         u2 inner_name_index;                           // its simple shortName
                         u2 inner_class_access_flags;                   // access_flags bitmask
                       } classes[number_of_classes]
                     }
*/
public final class BT_InnerClassesAttribute extends BT_Attribute {

	/**
	 The name of this attribute.
	**/
	public static final String ATTRIBUTE_NAME = "InnerClasses";
	
	
	public String getName() {
		return ATTRIBUTE_NAME;
	}

	/**
	 Descriptions of the class's inner classes.
	**/
	public Description[] inners; // (Description is a nested class)

	/**
	 @param data  The part of the attribute value following "attribute_length"
	   from the class file.
	**/
	BT_InnerClassesAttribute(byte data[], BT_ConstantPool pool, BT_Class containingClass, LoadLocation loadedFrom)
		throws BT_AttributeException {
		super(containingClass, loadedFrom);
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			DataInputStream dis = new DataInputStream(bais);
			int nc = dis.readUnsignedShort(); // AKA number_of_classes
			if (data.length != 2 + 8 * nc)
				throw new BT_AttributeException(ATTRIBUTE_NAME,
					Messages.getString("JikesBT.{0}_attribute_length_2", ATTRIBUTE_NAME));
			inners = new Description[nc];
			try {
				for (int ic = 0; ic < nc; ++ic) { // Per inner class description
					inners[ic] = new Description();
					int iix = dis.readUnsignedShort(); // AKA inner_class_info_index
					int oix = dis.readUnsignedShort(); // AKA outer_class_info_index
					int nix = dis.readUnsignedShort(); // AKA inner_name_index
					inners[ic].flags = dis.readShort();
					// AKA inner_class_access_flags -- sign doesn't matter
					if (iix != 0) { // Not __
						String name = pool.getClassNameAt(iix, BT_ConstantPool.CLASS);
						//inners[ic].innerClass = pool.getRepository().linkTo(name);
						inners[ic].innerClassName = name;
					}
					if (oix != 0) { // Is a package member
						String name = pool.getClassNameAt(oix, BT_ConstantPool.CLASS);
						//inners[ic].outerClass = pool.getRepository().linkTo(name);
						inners[ic].outerClassName = name;
					}
					if (nix != 0) // Not anonymous
						inners[ic].shortName = pool.getUtf8At(nix);
				} // Per inner class description
			} catch(BT_ConstantPoolException e) {
				throw new BT_AttributeException(ATTRIBUTE_NAME, e);
			}
		} catch(IOException e) {
			throw new BT_AttributeException(ATTRIBUTE_NAME, e);
		}
	}
	
	public BT_Class getContainingClass() {
		return (BT_Class) getOwner();
	}
	
	void dereference(BT_Repository rep) {
		for (int ic = 0; ic < inners.length; ++ic) { // Per inner class description
			Description d = inners[ic]; 
			if(d.innerClassName != null) {
				d.innerClass = rep.forName(d.innerClassName);
				d.innerClassName = null;
				d.innerClass.addReferencingAttribute(this);
			}
			if(d.outerClassName != null) {
				d.outerClass = rep.forName(d.outerClassName);
				d.outerClassName = null;
				d.outerClass.addReferencingAttribute(this);
			}
		}
	}
	
	void removeReference(BT_Item reference) {
		boolean toRemove[] = new boolean[inners.length];
		int toRemoveCount = 0;
		for (int ic = 0; ic < inners.length; ++ic) { // Per inner class description
			Description d = inners[ic]; 
			if(d.innerClass == reference || d.outerClass == reference) {
				toRemove[ic] = true;
				toRemoveCount++;
			}
		}
		if(toRemoveCount == inners.length) {
			remove();
			return;
		}
		Description newInners[] = new Description[inners.length - toRemoveCount];
		for(int i=0, j=0; i<inners.length; i++) {
			if(!toRemove[i]) {
				newInners[j++] = inners[i];
			}
		}
		inners = newInners;
	}
	
	
	
	void remove() {
		for (int ic = 0; ic < inners.length; ++ic) { // Per inner class description
			Description d = inners[ic]; 
			if(d.innerClass != null) {
				d.innerClass.removeReferencingAttribute(this);
			}
			if(d.outerClass != null) {
				d.outerClass.removeReferencingAttribute(this);
			}
		}
		super.remove();
	}

	public void resolve(BT_ConstantPool pool) {
		pool.indexOfUtf8(getName());
		for (int ic = 0; ic < inners.length; ++ic) { // Per element
			if (inners[ic].innerClass != null)
				inners[ic].innerCPIx(pool);
			if (inners[ic].outerClass != null) // Is a package member
				inners[ic].outerCPIx(pool);
			if (inners[ic].shortName != null)
				inners[ic].nameCPIx(pool);
		} // Per element
	}

	
	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		//BT_Repository.debugRecentlyWrittenAttribute = this;
		dos.writeShort(pool.indexOfUtf8(getName()));
		// attribute_name_index
		dos.writeInt(2 + 8 * inners.length); // attribute_length
		dos.writeShort(inners.length); // number_of_classes
		for (int ic = 0; ic < inners.length; ++ic) { // Per element
			dos.writeShort(inners[ic].innerCPIx(pool));
			// inner_class_info_index
			dos.writeShort(inners[ic].outerCPIx(pool));
			// outer_class_info_index
			dos.writeShort(inners[ic].nameCPIx(pool)); // inner_name_index
			dos.writeShort(inners[ic].flags); // inner_class_access_flags
		} // Per element
	}

	public String toString() {
		return Messages.getString("JikesBT.{0}_size_{1}_4", new Object[] {ATTRIBUTE_NAME, Integer.toString(inners.length)}); 
	}

	public void print(java.io.PrintStream ps, String prefix) {
		ps.println(Messages.getString("JikesBT.{0}InnerClasses__4", prefix));
		for (int i = 0; i < inners.length; ++i) // Per element
			ps.println(prefix + "  " + inners[i]);
	}

	/**
	 A description of an inner class.
	**/
	public static class Description {

		String innerClassName;
		String outerClassName;
		
		public BT_Class innerClass;

		/**
		 Null iff the inner class is not a package member (i.e., is defined in a block
		 (e.g., in a method) or in a class that is not a package member).
		**/
		public BT_Class outerClass;

		/**
		 Null iff the inner class is anonymous (but perhaps some compilers make it "" then?).
		**/
		public String shortName;

		/**
		 * the flags as they appear in the source.  Note that this does not translate directly
		 * to the flags as they appear in the compiled class file.  Certain flags, such as static
		 * and private, can apply to an inner class in the source file but are not applicable to
		 * a compiled class file.
		 */
		public short flags; // (Sign bit doesn't matter)

		private int innerCPIx(BT_ConstantPool pool) {
			return pool.indexOfClassRef(innerClass);
		}

		private int outerCPIx(BT_ConstantPool pool) {
			return pool.indexOfClassRef(outerClass);
		}

		private int nameCPIx(BT_ConstantPool pool) {
			return pool.indexOfUtf8(shortName);
		}

		public String toString() {
			return Messages.getString("JikesBT.inner__{0}__outer__{1}__short__{2}__flags__{3}_6", 
				new Object[] {innerClass, outerClass, shortName, 
					innerClass.flagString(flags)
				});
		}
	}
}
