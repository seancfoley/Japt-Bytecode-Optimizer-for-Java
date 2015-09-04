package com.ibm.ive.tools.japt.memoryAreaCheck;

import com.ibm.ive.tools.japt.reduction.ita.AllocationContext;
import com.ibm.ive.tools.japt.reduction.ita.Clazz;
import com.ibm.ive.tools.japt.reduction.ita.ContextProperties;
import com.ibm.ive.tools.japt.reduction.ita.GenericObject;
import com.ibm.ive.tools.japt.reduction.ita.PropagatedObject;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_Method;

public class TypeProperties {
	
	ClassProperties classProperties;
	ContextProperties contextProperties;
	public final AllocationContext IMMORTAL;
	public final AllocationContext SCOPED;
	public final AllocationContext GENERIC;
	
	public final ThreadType REGULAR_THREAD = new ThreadType() {
		public BT_Class canReference(AllocationContext context) {
			if(context == SCOPED) {
				return classProperties.illegalThreadStateException;
			}
			return null;
		}
		
		public String toString() {
			return "regular thread";
		}
		
		public boolean isRealTime() {
			return false;
		}
		
		public boolean isNoHeapRealTime() {
			return false;
		}
	};
	
	public final ThreadType REAL_TIME_THREAD = new ThreadType() {
		public BT_Class canReference(AllocationContext context) {
			return null;
		}
		
		public String toString() {
			return "javax.realtime.RealtimeThread";
		}
		
		public boolean isRealTime() {
			return true;
		}
		
		public boolean isNoHeapRealTime() {
			return false;
		}
	};
	
	public final ThreadType NO_HEAP_REAL_TIME_THREAD = new ThreadType() {
		public BT_Class canReference(AllocationContext context) {
			if(context == contextProperties.HEAP) {
				return classProperties.memoryAccessError;
			}
			return null;
		}
		
		public String toString() {
			return "javax.realtime.NoHeapRealtimeThread";
		}
		
		public boolean isRealTime() {
			return true;
		}
		
		public boolean isNoHeapRealTime() {
			return true;
		}
	};
	
	
	TypeProperties(ClassProperties props, ContextProperties contextProperties) {
		this.classProperties = props;
		this.contextProperties = contextProperties;
		GENERIC = new AllocationContext(contextProperties.allocationContextCount++) {
			public BT_Class canBeReferencedBy(AllocationContext other) {
				return null;
			}
			
			public String toString() {
				return "generic memory";
			}
		};
		
		SCOPED = new AllocationContext(contextProperties.allocationContextCount++) {
			public BT_Class canBeReferencedBy(AllocationContext other) {
				if(this != other) {
					return classProperties.illegalAssignmentError;
				}
				return null;
			}
			
			public String toString() {
				return "scoped memory";
			}
		};
		
		IMMORTAL = new AllocationContext(contextProperties.allocationContextCount++) {
			public BT_Class canBeReferencedBy(AllocationContext other) {
				return null;
			}
			
			public String toString() {
				return "immortal memory";
			}
		};
		
	}
	
	AllocationContext switchesMemoryArea(PropagatedObject invokedObject, BT_Method method) {
		if(method.equals(classProperties.scopedEnter) || method.equals(classProperties.scopedEnterRunnable) || method.equals(classProperties.scopedExecuteInArea)) {
			return SCOPED;
		}
		if(method.equals(classProperties.heapExecuteInArea)) {
			return contextProperties.HEAP;
		}
		if(method.equals(classProperties.immortalExecuteInArea)) {
			return IMMORTAL;
		}
		if(method.equals(classProperties.memEnter) || method.equals(classProperties.memEnterRunnable) || method.equals(classProperties.memExecuteInArea)) {
			if(classProperties.heapClass.isInstance(invokedObject.getType().getUnderlyingType())) {
				return contextProperties.HEAP;
			}
			if(classProperties.immortalClass.isInstance(invokedObject.getType().getUnderlyingType())) {
				return IMMORTAL;
			}
			if(classProperties.scopedClass.isInstance(invokedObject.getType().getUnderlyingType())) {
				return SCOPED;
			}
			/* for other memory areas, the context does not change */
		}
		return null;
	}
	
	AllocationContext convert(PropagatedObject imaObject) {
		Clazz type = imaObject.getType();
		BT_Class under = type.getUnderlyingType();
		if(classProperties.heapClass.isInstance(under)) {
			return contextProperties.HEAP;
		} else if(classProperties.immortalClass.isInstance(under)) {
			return IMMORTAL;
		} else if(classProperties.scopedClass.isInstance(under)) {
			return SCOPED;
		} else if(classProperties.memClass.isInstance(under) 
				|| (imaObject.isGeneric() && ((GenericObject) imaObject).isGenericInstanceOf(classProperties.memClass))) {
			return GENERIC;
		} else {
			return null;
		}
	}
	
	ThreadType convertToThreadType(Clazz type) {
		BT_Class under = type.getUnderlyingType();
		if(classProperties.noHeapRealtimeThread.isInstance(under)) {
			return NO_HEAP_REAL_TIME_THREAD;
		} else if(classProperties.realtimeThread.isInstance(under)) {
			return REAL_TIME_THREAD;
		} else if(classProperties.javaLangThread.isInstance(under)) {
			return REGULAR_THREAD;
		} else {
			return null;
		}
	}
}
