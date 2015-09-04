/*
 * Created on Oct 4, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.jikesbt;

public class BT_ClassVersion implements Comparable {
	
	
	public static final int JAVA_8_VERSION = 52;//Java 8 versions: 52.0 or below
	public static final int JAVA_7_VERSION = 51;//Java 7 versions: 51.0 or below
	public static final int JAVA_6_VERSION = 50;//Java 6 versions: 50.0 or below
	public static final int JAVA_5_VERSION = 49;//java 5 versions: 49.0 or below
	public static final int JAVA_4_VERSION = 48;//java 1.4.2 versions: 48.0 or below
	
	/**
	 * classes whose major version if greater than or equal to this version must have 
	 * stackmap attributes in their code attributes.
	 */
	public static final int STACKMAP_COMPULSORY_VERSION = JAVA_7_VERSION;
	
	/**
	 * classes whose major version if greater than or equal to this version should have 
	 * stackmap attributes in their code attributes.
	 */
	public static final int STACKMAP_BENEFICIAL_VERSION = JAVA_6_VERSION;
	
	/**
	 * Prior to Java 5 only the synthetic attribute was available.
		For java 6 and up both are supported
													
	 VM Spec 4.8.7: 
	 A class member that does not appear in the source code must be marked using a Synthetic
		attribute, or else it must have its ACC_SYNTHETIC bit set.
		
	 * Classes whose major version if greater than or equal to this version should use
	 * the synthetic flag instead of the synthetic attribute.
	 */
	public static final int SYNTHETIC_FLAG_VERSION = JAVA_5_VERSION;
	
	/**
	 * classes whose major version if greater than or equal to this version must have 
	 * no jsrs (no jsr or jsr_w opcode).
	 */
	public static final int NO_JSR_VERSION = JAVA_7_VERSION;
	
	/**
	 * classes created within JikesBT will be given this default version unless otherwise specified.
	 */
	public static final int DEFAULT_MAJOR_VERSION = JAVA_8_VERSION, DEFAULT_MINOR_VERSION = 0; 
	
	public static final BT_ClassVersion LATEST = new BT_ClassVersion();
	public static final BT_ClassVersion JAVA_8 = new BT_ClassVersion(JAVA_8_VERSION);//Java 8 versions: 52.0 or below
	public static final BT_ClassVersion JAVA_7 = new BT_ClassVersion(JAVA_7_VERSION);//Java 7 versions: 51.0 or below
	public static final BT_ClassVersion JAVA_6 = new BT_ClassVersion(JAVA_6_VERSION);//Java 6 versions: 50.0 or below
	public static final BT_ClassVersion JAVA_5 = new BT_ClassVersion(JAVA_5_VERSION);//java 5 versions: 49.0 or below
	public static final BT_ClassVersion JAVA_4 = new BT_ClassVersion(JAVA_4_VERSION);//java 1.4.2 versions: 48.0 or below
	public static final BT_ClassVersion JAVA_3 = new BT_ClassVersion(47);//java 1.4.2 versions: 48.0 or below
	public static final BT_ClassVersion JAVA_2 = new BT_ClassVersion(46);//java 1.4.2 versions: 48.0 or below
	
	public static final BT_ClassVersion STACKMAP_COMPULSORY = new BT_ClassVersion(STACKMAP_COMPULSORY_VERSION);
	
	public static final BT_ClassVersion STACKMAP_BENEFICIAL = new BT_ClassVersion(STACKMAP_BENEFICIAL_VERSION);
	
	int majorVersion;
	int minorVersion;
	
	public static BT_ClassVersion getVersion(String value) {
		BT_ClassVersion requestedClassVersion;
		if(value.equalsIgnoreCase("latest")) {
			requestedClassVersion = BT_ClassVersion.LATEST;
		} else if(value.equals("8") || value.equals("1.8")) {
			requestedClassVersion = BT_ClassVersion.JAVA_8;
		} else if(value.equals("7") || value.equals("1.7")) {
			requestedClassVersion = BT_ClassVersion.JAVA_7;
		} else if(value.equals("6") || value.equals("1.6")) {
			requestedClassVersion = BT_ClassVersion.JAVA_6;
		} else if(value.equals("5") || value.equals("1.5")) {
			requestedClassVersion = BT_ClassVersion.JAVA_5;
		} else if(value.equals("4") || value.equals("1.4")) {
			requestedClassVersion = BT_ClassVersion.JAVA_4;
		} else if(value.equals("3") || value.equals("1.3")) {
			requestedClassVersion = BT_ClassVersion.JAVA_3;
		} else if(value.equals("2") || value.equals("1.2")) {
			requestedClassVersion = BT_ClassVersion.JAVA_2;
		} else if(value.equals("1.1")) {
			requestedClassVersion = new BT_ClassVersion(45, 65535);
		} else if(value.equals("1.0.2")) {
			requestedClassVersion = new BT_ClassVersion(45, 3);
		} else {
			requestedClassVersion = new BT_ClassVersion(value);
		}
		return requestedClassVersion;
	}
		
	public BT_ClassVersion(String s) throws NumberFormatException {
		int dotIndex = s.indexOf('.');
		switch(dotIndex) {
			case 0:
				throw new NumberFormatException();
			case -1:
				minorVersion = 0;
				majorVersion = Integer.parseInt(s);
				break;
			default:
				minorVersion = Integer.parseInt(s.substring(dotIndex + 1));
				majorVersion = Integer.parseInt(s.substring(0, dotIndex));
				break;
		}
	}
	
	public BT_ClassVersion(int majorVersion) {
		this(majorVersion, 0);
	}
	
	public BT_ClassVersion(int majorVersion, int minorVersion) {
		this.majorVersion = majorVersion;
		this.minorVersion = minorVersion;
	}
	
	public BT_ClassVersion() {
		this(DEFAULT_MAJOR_VERSION, DEFAULT_MINOR_VERSION);
	}
	
	public int compareTo(Object other) {
		BT_ClassVersion otherVersion = (BT_ClassVersion) other;
		if(isHigherThan(otherVersion)) {
			return 1;
		}
		if(otherVersion.isHigherThan(this)) {
			return -1;
		}
		return 0;
	}
	
	public boolean isHigherThan(BT_ClassVersion other) {
		return majorVersion > other.majorVersion || 
			(majorVersion == other.majorVersion && minorVersion > other.minorVersion);
	}
	
	public boolean packageAnnotationIsSynthetic() {
		return majorVersion >= JAVA_6_VERSION;
	}
	
	public boolean interfacesAbstract() {
		return majorVersion >= JAVA_6_VERSION;
	}
	
	/**
	 * @return whether there exists the method java.lang.Throwable.initCause(Ljava.lang.Throwable;)Ljava.lang.Throwable;
	 */
	public boolean hasThrowableInitCause() {
		return majorVersion >= JAVA_4_VERSION;
	}
	
	public String getStringBuilderClass() {
		if(majorVersion >= JAVA_5_VERSION) {
			return BT_Repository.JAVA_LANG_STRING_BUILDER;
		}
		return BT_Repository.JAVA_LANG_STRING_BUFFER;
	}
	
	public boolean mustHaveStackMaps() {
		return majorVersion >= STACKMAP_COMPULSORY_VERSION;
	}
	
	public boolean shouldHaveStackMaps() {
		return majorVersion >= STACKMAP_BENEFICIAL_VERSION;
	}
	
	public boolean olderCLDCStackMaps() {
		return majorVersion <= JAVA_4_VERSION;
	}
	
	public boolean mustInlineJSRs() {
		return majorVersion >= NO_JSR_VERSION;
	}
	
	public boolean invokeSpecialSemanticsMandatory() {
		return majorVersion >= JAVA_8_VERSION;
	}
	
	public boolean invokeSpecialSemantics() {
		return majorVersion >= JAVA_5_VERSION;
	}
	
	public boolean canLDCClassObject() {
		return majorVersion >= JAVA_5_VERSION;
	}
	
	public boolean interfacesMustBeAbstract() {
		return majorVersion >= JAVA_5_VERSION;
	}
	
	public boolean canUseSyntheticFlag() {
		return majorVersion >= SYNTHETIC_FLAG_VERSION;
	}
	
	public String toString() {
		return majorVersion + "." + minorVersion;
	}
	
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o instanceof BT_ClassVersion) {	
			BT_ClassVersion other = (BT_ClassVersion) o;
			return other.majorVersion == majorVersion && other.minorVersion == minorVersion;
		}
		return false;
	}
	
	public boolean exceeds(BT_ClassVersion other) {
		return majorVersion > other.majorVersion || 
				(majorVersion == other.majorVersion && minorVersion > other.minorVersion);
	}
}