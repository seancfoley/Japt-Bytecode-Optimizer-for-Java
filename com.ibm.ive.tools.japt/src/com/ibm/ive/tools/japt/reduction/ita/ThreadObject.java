package com.ibm.ive.tools.japt.reduction.ita;

import java.util.ArrayList;

public class ThreadObject extends ConstructedObject {
	public final Method runMethod;
	ArrayList runContexts; //contains objects of type CallingContext
	boolean started;
	
	protected ThreadObject(Clazz type, AllocationContext context) {
		super(type, context);
		Repository rep = getRepository();
		Clazz threadClass = rep.getJavaLangThread();
		runMethod = threadClass.getMethod(rep.getClassProperties().threadRun);
		addDefaultRunContext();
	}
	
	public void addDefaultRunContext() {
		Repository rep = getRepository();
		addRunContext(rep.getPropagationProperties().provider.getJavaLangThreadContext());
	}
	
	public void startThread() {
		if(started) {
			return;
		}
		started = true;
		if(runContexts != null) {
			for(int i=0; i<runContexts.size(); i++) {
				CallingContext context = (CallingContext) runContexts.get(i);
				startThread(context);
			}
		}
		//TODO need to handle uncaught exeption handlers in the thread object and thread class
	}
	
	public void addRunContext(CallingContext context) {
		boolean isNew = (runContexts == null);
		if(isNew) {
			runContexts = new ArrayList(1);
			runContexts.add(context);
		} else if(isNew = !runContexts.contains(context)){
			runContexts.add(context);
		} else {
			return;
		}
		if(started) {
			startThread(context);
		}
	}
	
	private void startThread(CallingContext context) {
		Clazz threadClass = getType();
		VirtualMethodInvoke virtualInvoker = new VirtualMethodInvoke(runMethod.getRepository(), context, runMethod, threadClass);
		virtualInvoker.invokeVirtualMethod(this);
	}
}
