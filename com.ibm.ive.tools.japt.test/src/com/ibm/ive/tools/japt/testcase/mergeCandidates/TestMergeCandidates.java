/*
 * Created on Jan 19, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.testcase.mergeCandidates;

public class TestMergeCandidates {
	public static void main(String[] args) throws Throwable {
		Class visibleSuperClassClass = VisibleSuper.class;
		String visibleSuperClass = visibleSuperClassClass.getName();
		String pkg = visibleSuperClassClass.getPackage().getName() + '.';
		String appClass = pkg + "App";
		String hiddenSuperClass = pkg + "HiddenSuper";
		String subClass1 = visibleSuperClass + ".Sub1";
		String subClass2 = visibleSuperClass + ".Sub2";
		String classLoaderSource = "jarFile|dir";
		if(args.length == 0) {
			System.out.println("usage: " + TestMergeCandidates.class.getName() + " " + classLoaderSource + " [verbose]");
			System.out.println(
				"where " + classLoaderSource + " should be a jar file or directory containing the class file for class " + removePackageName(appClass) + ",\n" +
				"and silent indicates that the loader specialized loader should not report what it has loaded." + "\n\n" +
				"This test case attempts to load class " + removePackageName(appClass) + " in its own class loader." + "  " +
				"The loader forbids " + removePackageName(appClass) + " to see class " + removePackageName(hiddenSuperClass) + "." + "  " +
				"This is consistent with the way an OSGI loader might hide classes which are not exported by its parent." + "\n\n" +
				removePackageName(subClass1) + " and " + removePackageName(subClass2) + " are used in such a way that the verifier must merge them into a common superclass." + "  " +
				"If a flow analyzer attempts to merge the two classes to their common superclass " + removePackageName(hiddenSuperClass) + ", then " + 
				"verification will fail when the verifier attempts to load " + removePackageName(hiddenSuperClass) + "." + "  " +
				"Instead, the classes must be merged to " + removePackageName(visibleSuperClass) + ", the visible superclass of " + removePackageName(hiddenSuperClass) + ".\n");
			return;
		}
		boolean silent = args.length <= 1 || !args[1].equals("verbose");
		RestrictedClassLoader loader = new RestrictedClassLoader(args[0], new String[] {hiddenSuperClass}, silent);
		Class clazz = loader.findClass(appClass);
		try {
			clazz.newInstance();
			System.out.println((silent ? "" : loader.buffer.toString()) + "test passes");
		} catch(Error e) {
			System.out.println((silent ? "" : loader.buffer.toString()) + "test fails dues to error: " + e);
		}
	}
	
	static String removePackageName(String className) {
		return RestrictedClassLoader.removePackageName(className);
	}
}
