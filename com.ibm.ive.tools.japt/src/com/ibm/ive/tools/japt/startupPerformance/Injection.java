/*
 * Created on Nov 3, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.startupPerformance;

import com.ibm.ive.tools.japt.Logger;
import com.ibm.jikesbt.BT_CodeAttribute;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Ins;
import com.ibm.jikesbt.BT_Method;

public class Injection implements Comparable {
	final CodeInjection parameters;
	
	public Injection(CodeInjection params) {
		this.parameters = params;
	}
	
	public int compareTo(Object o) {
		return parameters.compareTo(((Injection) o).parameters);
	}
	
	public String toString() {
		return parameters.toString();
	}
	
	void log(BT_CodeAttribute code, Messages messages, Logger logger) {}
	
	void logParseable(Messages messages, Logger logger) {}
	
	/**
	 * returns the change in the number of instructions in the method.
	 * @return
	 */
	void inject() {
		parameters.inject();
	}
	
	static class ReturnTypeInjection extends Injection {
		public ReturnTypeInjection(CodeInjection params) {
			super(params);
		}
		
		void logParseable(Messages messages, Logger logger) {
			BT_Ins ins = parameters.newInstructions[0];
			messages.OPTIMIZED_RETURN_SIMPLE.log(logger, Integer.toString(ins.byteIndex));
		}
		
		void log(BT_CodeAttribute code, Messages messages, Logger logger) {
			BT_Ins ins = parameters.newInstructions[0];
			int lineNumber = code.findLineNumber(ins);
			BT_Method method = code.getMethod();
			if(lineNumber > 0) {
				messages.OPTIMIZED_RETURN_AT_LINE.log(logger, 
						new Object[] {method.useName(), Integer.toString(lineNumber), Integer.toString(ins.byteIndex)});
			} else {
				messages.OPTIMIZED_RETURN.log(logger, 
						new Object[] {method.useName(), Integer.toString(ins.byteIndex)});
			}
		}
	}
	
	static class ThrowInjection extends Injection {
		
		public ThrowInjection(CodeInjection params) {
			super(params);
		}
			
		void logParseable(Messages messages, Logger logger) {
			BT_Ins ins = parameters.newInstructions[0];
			messages.OPTIMIZED_THROW_SIMPLE.log(logger, Integer.toString(ins.byteIndex));
		}
		
		
		void log(BT_CodeAttribute code, Messages messages, Logger logger) {
			BT_Ins ins = parameters.newInstructions[0];
			int lineNumber = code.findLineNumber(ins);
			BT_Method method = code.getMethod();
			if(lineNumber > 0) {
				messages.OPTIMIZED_THROW_AT_LINE.log(logger, 
						new Object[] {method.useName(), Integer.toString(lineNumber), Integer.toString(ins.byteIndex)});
			} else {
				messages.OPTIMIZED_THROW.log(logger, 
						new Object[] {method.useName(), Integer.toString(ins.byteIndex)});
			}
		}
	}
	
	static class MethodArgInjection extends Injection {
		final BT_Method invokedMethod;
		final int invokeIndex;
		
		public MethodArgInjection(BT_Method invokedMethod, int invokeIndex, CodeInjection params) {
			super(params);
			this.invokedMethod = invokedMethod;
			this.invokeIndex = invokeIndex;
		}
		
		void logParseable(Messages messages, Logger logger) {
			BT_Ins ins = parameters.newInstructions[0];
			messages.OPTIMIZED_METHOD_SIMPLE.log(logger, Integer.toString(ins.byteIndex));
		}
		
		void log(BT_CodeAttribute code, Messages messages, Logger logger) {
			BT_Ins ins = parameters.newInstructions[0];
			int lineNumber = code.findLineNumber(ins);
			BT_Method method = code.getMethod();
			if(lineNumber > 0) {
				messages.OPTIMIZED_METHOD_AT_LINE.log(logger, 
						new Object[] {method.useName(), Integer.toString(lineNumber), Integer.toString(ins.byteIndex), invokedMethod.useName()});
			} else {
				messages.OPTIMIZED_METHOD.log(logger, 
						new Object[] {method.useName(), Integer.toString(ins.byteIndex), invokedMethod.useName()});
			}
		}
		
		public String toString() {
			return super.toString() + " for invocation of " + invokedMethod.useName() + " at instruction index " + invokeIndex;
		}
	}
	
	static class FieldTypeInjection extends Injection {
		final BT_Field field;
		final int accessIndex;
		
		public FieldTypeInjection(BT_Field field, int accessIndex, CodeInjection params) {
			super(params);
			this.field = field;
			this.accessIndex = accessIndex;
		}
		
		void logParseable(Messages messages, Logger logger) {}
		
		void log(BT_CodeAttribute code, Messages messages, Logger logger) {
			BT_Ins ins = parameters.newInstructions[0];
			int lineNumber = code.findLineNumber(ins);
			BT_Method method = code.getMethod();
			if(lineNumber > 0) {
				messages.OPTIMIZED_FIELD_AT_LINE.log(logger, 
						new Object[] {method.useName(), Integer.toString(lineNumber), Integer.toString(ins.byteIndex), field.useName()});
			} else {
				messages.OPTIMIZED_FIELD.log(logger, 
						new Object[] {method.useName(), Integer.toString(ins.byteIndex), field.useName()});
			}
		}
		
		public String toString() {
			return super.toString() + " for access to " + field.useName() + " at instruction index " + accessIndex;
		}
	}

	

}
