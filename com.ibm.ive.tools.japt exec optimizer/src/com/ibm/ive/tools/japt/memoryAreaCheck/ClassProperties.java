package com.ibm.ive.tools.japt.memoryAreaCheck;

import com.ibm.ive.tools.japt.JaptRepository;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_Repository;

public class ClassProperties {
	final BT_Class realtimeThread;
	final BT_Class noHeapRealtimeThread;
	final BT_Class javaLangThread;
	final BT_Class heapClass;
	final BT_Class immortalClass;
	final BT_Class scopedClass;
	final BT_Class memClass;
	final BT_Class illegalThreadStateException;
	final BT_Class memoryAccessError;
	final BT_Class illegalAssignmentError;
	final BT_Method realtimeThreadStart;
	final BT_Method noHeapRealtimeThreadStart;
	final BT_Method scopedEnter;
	final BT_Method scopedEnterRunnable;
	final BT_Method scopedExecuteInArea;
	final BT_Method memEnter;
	final BT_Method memEnterRunnable;
	final BT_Method memExecuteInArea;
	final BT_Method heapExecuteInArea;
	final BT_Method immortalExecuteInArea;
	final BT_Method memNewInstance;
	final BT_Method memNewInstanceCon;
	final BT_Method memNewArray;
	
	ClassProperties(JaptRepository repo) {
		javaLangThread = repo.findJavaLangThread();
		realtimeThread = repo.linkTo("javax.realtime.RealtimeThread");
		noHeapRealtimeThread = repo.linkTo("javax.realtime.NoHeapRealtimeThread");
		memClass = repo.linkTo("javax.realtime.MemoryArea");
		heapClass = repo.linkTo("javax.realtime.HeapMemory");
		immortalClass = repo.linkTo("javax.realtime.ImmortalMemory");
		scopedClass = repo.linkTo("javax.realtime.ScopedMemory");
		illegalThreadStateException = repo.linkTo(BT_Repository.JAVA_LANG_ILLEGAL_THREAD_STATE_EXCEPTION);
		memoryAccessError = repo.linkTo("javax.realtime.MemoryAccessError");
		illegalAssignmentError = repo.linkTo("javax.realtime.IllegalAssignmentError");
		realtimeThreadStart = realtimeThread.findInheritedMethod("start", repo.basicSignature);
		noHeapRealtimeThreadStart = noHeapRealtimeThread.findInheritedMethod("start", repo.basicSignature);
		BT_Class runnableClass = repo.linkTo(BT_Repository.JAVA_LANG_RUNNABLE);
		BT_ClassVector args = new BT_ClassVector(1);
		args.addElement(runnableClass);
		BT_MethodSignature runnableSig = BT_MethodSignature.create(repo.getVoid(), args, repo);
		scopedEnter = scopedClass.findInheritedMethod("enter", repo.basicSignature);
		scopedEnterRunnable = scopedClass.findInheritedMethod("enter", runnableSig);
		scopedExecuteInArea = scopedClass.findInheritedMethod("executeInArea", runnableSig);
		memEnter = memClass.findInheritedMethod("enter", repo.basicSignature);
		memEnterRunnable = memClass.findInheritedMethod("enter", runnableSig);
		memExecuteInArea = memClass.findInheritedMethod("executeInArea", runnableSig);
		heapExecuteInArea = heapClass.findInheritedMethod("executeInArea", runnableSig);
		immortalExecuteInArea = immortalClass.findInheritedMethod("executeInArea", runnableSig);
		args.removeAllElements();
		args.addElement(repo.findJavaLangClass());
		BT_MethodSignature newInstanceSig = BT_MethodSignature.create(repo.findJavaLangObject(), args, repo);
		memNewInstance = memClass.findInheritedMethod("newInstance", newInstanceSig);
		args.removeAllElements();
		args.addElement(repo.linkTo(BT_Repository.JAVA_LANG_REFLECT_CONSTRUCTOR));
		args.addElement(repo.findJavaLangObject().getArrayClass());
		BT_MethodSignature newInstanceConSig = BT_MethodSignature.create(repo.findJavaLangObject(), args, repo);
		memNewInstanceCon = memClass.findInheritedMethod("newInstance", newInstanceConSig);
		args.removeAllElements();
		args.addElement(repo.findJavaLangClass());
		args.addElement(repo.getInt());
		BT_MethodSignature newArraySig = BT_MethodSignature.create(repo.findJavaLangObject(), args, repo);
		memNewArray = memClass.findInheritedMethod("newArray", newArraySig);
	}
}
