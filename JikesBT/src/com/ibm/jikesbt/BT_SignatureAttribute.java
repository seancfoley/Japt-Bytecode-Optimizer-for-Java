/*
 * Created on Feb 7, 2006
 *
 * The enclosing method attribute is found in the attributes table
 * of the class file structure.
 */
package com.ibm.jikesbt;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ibm.jikesbt.BT_Repository.LoadLocation;

public class BT_SignatureAttribute extends BT_Attribute {
	private String signature;
			
	/**
	 The name of this attribute.
	**/
	public static final String ATTRIBUTE_NAME = "Signature";

	BT_SignatureAttribute(byte data[], BT_ConstantPool pool, BT_Item item, LoadLocation loadedFrom)
		throws BT_AttributeException {
		super(item, loadedFrom);
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			DataInputStream dis = new DataInputStream(bais);
			try {
				signature = pool.getUtf8At(dis.readUnsignedShort());
			} catch(BT_ConstantPoolException e) {
				throw new BT_AttributeException(ATTRIBUTE_NAME, e);
			}
		} catch(IOException e) {
			throw new BT_AttributeException(ATTRIBUTE_NAME, e);
		}
	}
	
	public String getName() {
		return ATTRIBUTE_NAME;
	}
	
	public String toString() {
		return Messages.getString("JikesBT.{0}_size_{1}_4", 
				new String[] {ATTRIBUTE_NAME, "2"}); 
	}
	
	public void resolve(BT_ConstantPool pool) {
		pool.indexOfUtf8(ATTRIBUTE_NAME);
		pool.indexOfUtf8(signature);
	}

	void write(DataOutputStream dos, BT_ConstantPool pool) throws IOException {
		dos.writeShort(pool.indexOfUtf8(ATTRIBUTE_NAME)); // attribute_name_index
		dos.writeInt(2); // attribute_length
		dos.writeShort(pool.indexOfUtf8(signature));
	}

	public void print(java.io.PrintStream ps, String prefix) {
		ps.println(Messages.getString("JikesBT.{0}Signature__{1}__4", 
				new Object[] {prefix, signature}));
	}
}
