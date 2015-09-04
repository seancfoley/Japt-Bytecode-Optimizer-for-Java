package com.ibm.ive.tools.japt.reduction.ita;

import com.ibm.jikesbt.BT_MethodSignature;

public abstract class MethodInvocationLocation { /* Note that we cache these objects in LocationPool objects */
	
	abstract boolean isParameter();
	
	public abstract int hashCode();
	
	static class StackLocation extends MethodInvocationLocation {
		
		public final int instructionIndex;
		
		/* 
		 * 0 is the top of the stack, 1 is just below, and so on.  Doubles and longs count for two.
		 * Therefore stackTopIndex 1 corresponds to an object on the top of the stack.
		 */
		public final int stackTopIndex; 
		
		/** 
		 * Use this constructor for any stack location at any instruction in the method. 
		 */
		StackLocation(int instructionIndex, int stackTopIndex) {
			this.instructionIndex = instructionIndex;
			this.stackTopIndex = stackTopIndex;
		}
		
		boolean isParameter() {
			return false;
		}
		
		static int getTopCellIndex() {
			return 1;
		}
		
		static int getNextToTopCellIndex() {
			return 2;
		}
		
		static int getSecondFromTopCellIndex() {
			return 3;
		}
		
		static int getInvokedCellIndex(BT_MethodSignature sig) {
			return sig.getArgsSize() + 1;
		}
		
		static int getParamCellIndex(BT_MethodSignature sig, int paramIndex) {
			return sig.getArgsSize(paramIndex, true);
		}
		
		public int hashCode() {
			return ((instructionIndex + 1) << 5) + stackTopIndex;
		}
		
		public boolean equals(Object o) {
			if(o instanceof StackLocation) {
				StackLocation other = (StackLocation) o;
				return instructionIndex == other.instructionIndex
					&& stackTopIndex == other.stackTopIndex;
			}
			return false;
		}
		
		public String toString() {
			String stackLoc;
			switch(stackTopIndex) {
				case 1:
					stackLoc = "top of stack";
					break;
				default:
					stackLoc = (stackTopIndex - 1) + " below top of stack";
					break;
			}
			return stackLoc + " at instruction index " + instructionIndex;
		}
	}
	
	static class ParameterLocation extends MethodInvocationLocation {
		
		public final int paramIndex;
		
		/**
		 * Use this constructor to correspond to a method argument.
		 * @param paramIndex the parameter index.  Doubles and longs do not count as two.
		 */
		ParameterLocation(int paramIndex) {
			this.paramIndex = paramIndex;
		}
		
		/** 
		 * Use this constructor for the "this" argument in a method invocation. 
		 */
		ParameterLocation() {
			this(0);
		}
		
		boolean isParameter() {
			return true;
		}
		
		public int hashCode() {
			return paramIndex;
		}
		
		public boolean equals(Object o) {
			if(o instanceof ParameterLocation) {
				ParameterLocation other = (ParameterLocation) o;
				return paramIndex == other.paramIndex;
			}
			return false;
		}
		
		public String toString() {
			return "parameter " + paramIndex;
		}
	}
}
