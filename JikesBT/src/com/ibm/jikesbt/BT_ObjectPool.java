package com.ibm.jikesbt;

import com.ibm.jikesbt.BT_PopulatedObjectCell.ParameterObject;
import com.ibm.jikesbt.BT_PopulatedObjectCell.ReceivedObject;

public class BT_ObjectPool {
	ReceivedObject receivedObjects[] = new ReceivedObject[1000];
	ParameterObject paramObjects[] = new ParameterObject[10];
	
	private static final int PARAM_OBJECT = 1;
	private static final int STACK_OBJECT = 2;
	
	public ReceivedObject getReceivedObject(int instructionIndex) {
    	if(instructionIndex >= receivedObjects.length) {
    		receivedObjects = (ReceivedObject[]) resize(receivedObjects, instructionIndex + 1, STACK_OBJECT);
    	}
    	ReceivedObject obj = receivedObjects[instructionIndex];
    	if(obj == null) {
    		receivedObjects[instructionIndex] = obj = new ReceivedObject(instructionIndex);
    	}
    	return obj;
    }
    
    public ParameterObject getParamObject(int paramIndex) {
    	if(paramIndex >= paramObjects.length) {
    		paramObjects = (ParameterObject[]) resize(paramObjects, paramIndex + 1, PARAM_OBJECT);
    	}
    	ParameterObject obj = paramObjects[paramIndex];
    	if(obj == null) {
    		paramObjects[paramIndex] = obj = new ParameterObject(paramIndex);
    	}
    	return obj;
    }
	
	private Object[] resize(Object old[], int newSize, int type) {
		Object newArray[];
		switch(type) {
			case STACK_OBJECT:
				newArray = new ReceivedObject[newSize];
				break;
			case PARAM_OBJECT:
				newArray = new ParameterObject[newSize];
				break;
			default: 
				throw new IllegalArgumentException();
		}
		System.arraycopy(old, 0, newArray, 0, old.length);
		return newArray;
	}	
}
