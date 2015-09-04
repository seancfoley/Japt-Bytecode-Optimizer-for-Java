package com.ibm.ive.tools.japt.obfuscation;


import com.ibm.jikesbt.*;
import com.ibm.ive.tools.japt.*;


/**
 * Determines which classes, methods, field and package names must be fixed as part of
 * name compression.
 * <p>
 * Names may not be changed under the following circumstances:<br>
 * - a class, method or field is dynamically accessed (e.g Class.forName())<br>
 * - serialized names, as well as methods and fields used by the VM in the serialization process<br>
 * - native methods, since the name of the native counterpart is derived from the method's name<br>
 * - static initializer names<br>
 * @author sfoley
 */
class StandardNameFixer {

	private NameHandler nameHandler;
 	private BT_ClassVector classes;
 	private JaptRepository repository;
 	
	
 	public StandardNameFixer(JaptRepository rep, BT_ClassVector classes, NameHandler nameHandler) {
		this.repository = rep;
		this.nameHandler = nameHandler;
		this.classes = classes;
	}
	
	static boolean isStandardMethod(BT_Method method) {
		return method.isStaticInitializer() || method.isConstructor() || method.isFinalizer();
	}
	
	void fixNames() {
		for(int i=0; i<classes.size(); i++) {
			int j;
			BT_Class clazz = classes.elementAt(i);
			BT_MethodVector methods = clazz.getMethods();
			if(repository.isInternalClass(clazz)) {
//				TODO fix classes accessed by non-internal classes
				for (j = 0; j < methods.size(); j++) {
					//TODO fix methods accessed by non-internal classes
					BT_Method method = methods.elementAt(j);
					
					/* now we check for additional reasons why the name might need to be fixed */
					if (isStandardMethod(method)) {
						nameHandler.freezeName(method); /* note there are no relatives of initializers or finalizers */
					}
					else if(method.isNative()) {
						nameHandler.fixName(method, "native method");
						nameHandler.fixName(clazz, "contains native method");
						BT_MethodSignature sig = method.getSignature();
						BT_ClassVector types = sig.types;
						BT_Class returnType = sig.returnType;
						for(int k=0; k<types.size(); k++) {
							BT_Class type = types.elementAt(k);
							//external classes have their name frozen elsewhere
							if(!type.isBasicTypeClass && repository.isInternalClass(type)) {
								nameHandler.fixName(type, "native method signature");
							}
						}
						//external classes have their name frozen elsewhere
						if(!returnType.isBasicTypeClass && repository.isInternalClass(returnType)) {
							nameHandler.fixName(returnType, "native method signature");
						}
					}
					else if(method.isStub()) {
						nameHandler.fixName(method, "stub method");
					}
				}
				
//				TODO fix fields accessed by non-internal classes
				BT_FieldVector fields = clazz.getFields();
				for (j = 0; j < fields.size(); j++) {
					BT_Field field = fields.elementAt(j);
					if (field.isStub()) {
						nameHandler.fixName(field, "stub field");
					}
				}
			}
			else { /* fix everything else */
				if(!clazz.isArray()) {
					nameHandler.freezeName(clazz);
				}
				//arrays have the clone method and the length field which must remain frozen
				for (j = 0; j < methods.size(); j++) {
					BT_Method method = methods.elementAt(j);
					nameHandler.freezeName(method);
				}
				BT_FieldVector fields = clazz.getFields();
				for (j = 0; j < fields.size(); j++) {
					nameHandler.freezeName(fields.elementAt(j));
				}	
			}
		}
		

			
	}
	
	


}
