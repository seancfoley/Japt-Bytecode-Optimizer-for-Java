package com.ibm.ive.tools.japt.reduction.ita;


public class ThreadStartInvocation extends SpecificMethodInvocation {

	private final Clazz threadClass;
	
	public ThreadStartInvocation(Method method, int depth, CallingContext context) {
		super(method, depth, context);
		Repository rep = getRepository();
		threadClass = rep.getJavaLangThread();
		
	}
	
	/**
	 * Determine if the argument is a thread object representing a thread 
	 * that was started by a call to this method.
	 * @param object
	 * @return
	 */
	boolean findNewThreadStarts(ReceivedObject object) {
		Repository rep = getRepository();
		PropagationProperties props = rep.getPropagationProperties();
		if(props.useIntraProceduralAnalysis()) {
			MethodInvocationLocation location = object.getLocation();
			MethodInvocationLocation paramLocation = rep.locationPool.getParamLocation(0);
			if(!paramLocation.equals(location)) {
				return false;
			}
		} 
		PropagatedObject obj = object.getObject();
		Clazz type = obj.getType();
		//if(!threadClass.mightBeInstance(type)) {//TODO do we want this?  do we want to be starting threads when we are not sure they are threads?
												//TODO if so, we will have to instantiate more GenericThreadObject and ThreadObject objects
		if(!threadClass.isInstance(type)) {
			return false;
		} 
		if(obj.isGeneric()) {
			GenericThreadObject threadObject = (GenericThreadObject) obj;
			threadObject.startThread();
		} else {
			ThreadObject threadObject = (ThreadObject) obj;
			threadObject.startThread();
		}
		return true;
	}
	
	protected void propagateNewObject(ReceivedObject obj) throws GenericInvocationException {
		findNewThreadStarts(obj);
		super.propagateNewObject(obj);
	}
}
