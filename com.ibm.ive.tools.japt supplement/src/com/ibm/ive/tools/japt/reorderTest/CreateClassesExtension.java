/*
 * Created on Mar 30, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.reorderTest;

import com.ibm.ive.tools.commandLine.FlagOption;
import com.ibm.ive.tools.commandLine.Option;
import com.ibm.ive.tools.commandLine.ValueOption;
import com.ibm.ive.tools.japt.ClassPathEntry;
import com.ibm.ive.tools.japt.ExtensionException;
import com.ibm.ive.tools.japt.FormattedString;
import com.ibm.ive.tools.japt.Identifier;
import com.ibm.ive.tools.japt.InvalidIdentifierException;
import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.ive.tools.japt.Logger;
import com.ibm.ive.tools.japt.SyntheticClassPathEntry;
import com.ibm.ive.tools.japt.JaptMessage.ProgressMessage;
import com.ibm.ive.tools.japt.commandLine.CommandLineExtension;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_ClassVersion;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_MethodVector;
import com.ibm.jikesbt.BT_Repository;

public class CreateClassesExtension implements CommandLineExtension {
	
	//private FlagOption slow = new FlagOption("slow", "slow class order");
	private ValueOption numClasses = new ValueOption("numClasses", "number of classes in app");
	private ValueOption numPerGroup = new ValueOption("numPerEntry", "number of classes per class path entry");
	private FlagOption slow = new FlagOption("slow", "slow class order");
	
	private BT_MethodSignature basicSignature;
	private BT_MethodSignature simpleSignature;
	static String baseName = "Test";
	private ClassPathEntry entries[];
	private ProgressMessage clazzMessage = new ProgressMessage(this, new FormattedString("created class number {0}"));
	private static int STACK_OVERFLOW_LIMIT = 15000;
	//private static int STACK_OVERFLOW_LIMIT = 4;
	
	
	public Option[] getOptions() {
		return new Option[] {numClasses, numPerGroup, slow};
	}

	public void execute(JaptRepository repository, Logger logger)
			throws ExtensionException {
		//BT_Repository.loadClassesUsingReflection = true;
		BT_Class stringArrayClass = repository.forName(String.class.getName() + "[]");
		basicSignature = BT_MethodSignature.create(repository.getVoid(), new BT_ClassVector(new BT_Class[] {stringArrayClass}), repository);
		simpleSignature = BT_MethodSignature.create(repository.getVoid(), BT_ClassVector.emptyVector, repository);
		
		try {
			int count = Integer.parseInt(numClasses.getValue()) - 1;
			int numPerEntry = 0;
			if(numPerGroup.appears()) {
				numPerEntry = Integer.parseInt(numPerGroup.getValue());
				entries = new ClassPathEntry[((count + 1) + (numPerEntry - 1)) / numPerEntry];
			} else {
				entries = new ClassPathEntry[1];
			}
			for(int i=0; i<entries.length; i++) {
				repository.addExtendedClassPathEntry(
						entries[i] = new SyntheticClassPathEntry("synthetic" + (i + 1) + ".jar", true));
			}
			BT_ClassVersion version = new BT_ClassVersion(49, 0);
			BT_Method method = createTargetClass(repository, entries[entries.length - 1], count, baseName + count, version);
			BT_MethodVector targets = new BT_MethodVector();
			boolean isSlow = slow.isFlagged();
			int ordinal; 
			/* a number 0, 1, ..., count-1 indicating exactly where in the order the current class falls */
			for(int i=1; i<count; i++) {
				int counter = count - i;
				if(isSlow) {
					ordinal = SlowExecutionClassComparator.getOrdinal(counter, count);
				} else {
					//baseName, then ascending by number
					ordinal = counter;
				}
				ClassPathEntry entry = entries[(ordinal * entries.length) / count];
				String name = baseName + counter;
				if((i % STACK_OVERFLOW_LIMIT) == 0) {
					targets.insertElementAt(method, 0);
					method = createTargetClass(repository, entry, counter, name, version);
				} else {
					method = createClass(repository, entry, name, method, version);
				}
				if(i % 500 == 0) {
					clazzMessage.log(logger, Integer.toString(i));
				}
			}
			targets.insertElementAt(method, 0);
			ordinal = 0;
			ClassPathEntry entry = entries[(ordinal * entries.length) / count];
			String name = baseName;
			createClass(repository, entry, name, targets.toArray(), version);
			if(isSlow) {
				repository.sortClasses(new SlowExecutionClassComparator());
			} else {
				repository.sortClasses(new FastExecutionClassComparator());
			}
			
		} catch(InvalidIdentifierException e) {
			ExtensionException ext = new ExtensionException(this, e.toString());
			throw ext;
		}
		
	}
	
	BT_Ins nullIns = BT_Ins.make(BT_Ins.opc_aconst_null);
	BT_Ins returnIns = BT_Ins.make(BT_Ins.opc_return);
	BT_Ins instructions[] = new BT_Ins[] {nullIns, null, returnIns};
	
	
	private BT_Method createTargetClass(JaptRepository repository, ClassPathEntry entry, int count, String name, BT_ClassVersion version) throws InvalidIdentifierException {
		BT_Class systemClass = repository.forName(System.class.getName());
		BT_Class printStreamClass = repository.forName(java.io.PrintStream.class.getName());
		BT_Field systemOutField = systemClass.findFieldOrNull("out", printStreamClass);
		
		BT_Class stringClass = repository.findJavaLangString();
		BT_Method printlnMethod = printStreamClass.findMethodOrNull("println", 
				BT_MethodSignature.create(repository.getVoid(), new BT_ClassVector(new BT_Class[] {stringClass}), repository));	
		
		BT_Class clazz = repository.createInternalClass(new Identifier(name), entry, version);
		BT_Method method = BT_Method.createMethod(clazz, BT_Method.STATIC, basicSignature, "aMethod");
		BT_Ins getIns = BT_Ins.make(BT_Ins.opc_getstatic, systemOutField);
		BT_Ins ldcIns = BT_Ins.make(BT_Ins.opc_ldc, "reached last method call out of " + (count + 1), repository);
		BT_Ins invokeIns = BT_Ins.make(BT_Ins.opc_invokevirtual, printlnMethod);
		BT_Ins returnIns = BT_Ins.make(BT_Ins.opc_return);
		
		
		//0 JBgetstatic 29 java.lang.System.out Ljava.io.PrintStream;
		 //3 JBldc 35 (java.lang.String) "hello"
		 //5 JBinvokevirtual 37 java.io.PrintStream.println(Ljava.lang.String;)V
		 //8 JBreturn
		BT_CodeAttribute code = new BT_CodeAttribute(new BT_Ins[] {getIns, ldcIns, invokeIns, returnIns}, method.getVersion());
		method.setCode(code);
		return method;
	}
	
	private BT_Method createClass(JaptRepository repository, ClassPathEntry entry, String name, BT_Method target, BT_ClassVersion version) 
			throws InvalidIdentifierException {
		BT_Method method = createClassWithMethod(repository, entry, name, version);
		BT_Ins invokeIns = BT_Ins.make(BT_Ins.opc_invokestatic, target);
		instructions[1] = invokeIns;
		BT_CodeAttribute code = new BT_CodeAttribute(instructions, method.getVersion());
		method.setCode(code);
		return method;
	}
	
	private BT_Method createClass(JaptRepository repository, ClassPathEntry entry, String name, BT_Method targets[], BT_ClassVersion version) 
		throws InvalidIdentifierException {
		BT_Method method = createClassWithMethod(repository, entry, name, version);
		method.makeCodeSimplyReturn();
		BT_CodeAttribute code = method.getCode();
		BT_Ins instructions[] = new BT_Ins[2];
		for(int i=targets.length - 1; i>=0; i--) {
			instructions[0] = BT_Ins.make(BT_Ins.opc_aconst_null);
			instructions[1] = BT_Ins.make(BT_Ins.opc_invokestatic, targets[i]);
			code.insertInstructionsAt(instructions, 0);
		}
		return method;
	}
	
//	private BT_Method createClass(JaptRepository repository, ClassPathEntry entry, String name, BT_ClassVersion version) 
//			throws InvalidIdentifierException {
//		BT_Method method = createClassWithMethod(repository, entry, name, version);
//		//method.makeCodeSimplyReturn();
//		return method;
//	}
	
	private BT_Method createClassWithMethod(JaptRepository repository, ClassPathEntry entry, String name, BT_ClassVersion version)
			throws InvalidIdentifierException  {
		BT_Class clazz = repository.createInternalClass(new Identifier(name), entry, version);
		BT_Method method = BT_Method.createMethod(clazz, (short) (BT_Method.STATIC | BT_Method.PUBLIC), basicSignature, "main");
		createExtraMethods(12, clazz);
		return method;
	}
	
	private void createExtraMethods(int count, BT_Class inClass) {
		for(int i=1; i<=count; i++) {
			BT_Method method = BT_Method.createMethod(inClass, (short) 0, simpleSignature, "extra" + i);
			method.makeCodeSimplyReturn();
		}
	}
	
	public String getName() {
		return "Create classes extension";
	}

}