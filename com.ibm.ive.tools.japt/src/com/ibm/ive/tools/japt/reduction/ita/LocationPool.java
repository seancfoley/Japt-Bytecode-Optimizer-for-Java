package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation.ParameterLocation;
import com.ibm.ive.tools.japt.reduction.ita.MethodInvocationLocation.StackLocation;

public class LocationPool {
	StackLocation stackLocations[][] = new StackLocation[1000][15];
	ParameterLocation paramsLocations[] = new ParameterLocation[10];
	
	private static final int INSTRUCTION_INDEX = 0;
	private static final int PARAM_INDEX = 1;
	private static final int STACK_INDEX = 2;
	
	/**
     * 
     */
    public LocationPool() {}
    
    public ParameterLocation getParamLocation(int paramIndex) {
    	if(paramIndex >= paramsLocations.length) {
    		paramsLocations = (ParameterLocation[]) resize(paramsLocations, paramIndex + 1, PARAM_INDEX);
    	}
    	ParameterLocation location = paramsLocations[paramIndex];
    	if(location == null) {
    		paramsLocations[paramIndex] = location = new ParameterLocation(paramIndex);
    	}
    	return location;
    }
    
	public StackLocation getStackLocation(int instructionIndex, int stackIndex) {
		if(instructionIndex >= stackLocations.length) {
			stackLocations = (StackLocation[][]) resize(stackLocations, instructionIndex + 1, INSTRUCTION_INDEX);
    	}
		StackLocation[] locations = stackLocations[instructionIndex];
		if(locations == null) {
			stackLocations[instructionIndex] = locations = new StackLocation[stackIndex + 1];
		} else if(stackIndex >= locations.length) {
			stackLocations[instructionIndex] = locations = (StackLocation[]) resize(locations, stackIndex + 1, STACK_INDEX);
		}
		StackLocation location = locations[stackIndex];
		if(location == null) {
			locations[stackIndex] = location = new StackLocation(instructionIndex, stackIndex);
		}
		return location;
    }
	
	private Object[] resize(Object old[], int newSize, int type) {
		Object newArray[];
		switch(type) {
			case INSTRUCTION_INDEX:
				newArray = new StackLocation[newSize][];
				break;
			case STACK_INDEX:
				newArray = new StackLocation[newSize];
				break;
			case PARAM_INDEX:
				newArray = new ParameterLocation[newSize];
				break;
			default: 
				throw new IllegalArgumentException();
		}
		System.arraycopy(old, 0, newArray, 0, old.length);
		return newArray;
	}	
}
