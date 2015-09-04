package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 A resource to be added to a jar file when a {@link BT_Repository} is written using 
 its save method.

 * @author IBM
**/
public class BT_JarResource implements BT_Opcodes {

	/**
	 * The name of the resource as it appears in the jar file, for example
	 * "com/ibm/jikesbt/splashLogo.jpg". 
	 */
	public String name;
	
	/**
	 * The contents of the resouce
	 */
	public byte contents[];
	
	/**
	 * create a new BT_JarResource
	 * @param name the name of the resource
	 * @param contents the bytes to be written out
	 */
	public BT_JarResource(String name, byte contents[]) {
		this.name = name;
		this.contents = contents;
	}
	
}
