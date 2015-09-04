/*
 * Created on Oct 20, 2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt;

import java.util.ArrayList;
import java.util.HashSet;

import com.ibm.ive.tools.japt.MemberActor.ClassActor;
import com.ibm.ive.tools.japt.MemberActor.MemberCollectorActor;
import com.ibm.ive.tools.japt.MultipleObjectIdentifier.FieldIdentifier;
import com.ibm.ive.tools.japt.MultipleObjectIdentifier.InvalidSpecException;
import com.ibm.ive.tools.japt.MultipleObjectIdentifier.ObjectAndFieldsIdentifier;
import com.ibm.ive.tools.japt.MultipleObjectIdentifier.ObjectIdentifier;
import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_FieldVector;

public class IdentifierArguments {
//	format is Object.f1{}f2{}f3{},Object2.g1{},Object3
	public static class MultipleObjectSpec {
		public final ObjectSpec objectSpecs[];
		
		MultipleObjectSpec(ObjectSpec ospecs[]) {
			this.objectSpecs = ospecs;
		}
		
		boolean isEmpty() {
			for(int i=0; i<objectSpecs.length; i++) {
				if(!objectSpecs[i].isEmpty()) {
					return false;
				}
			}
			return true;
		}
	}
	
	//Object.f1{}f2{}f3{} or Object2.g1{} or Object3 with wildcards in play
	public static class ObjectSpec {
		private static final ObjectType[] emptySpecs = new ObjectType[0];
    	public final ObjectType typeSpecs[];
    	
    	ObjectSpec() {
    		this(emptySpecs);
    	}
    	
    	ObjectSpec(ObjectType typeSpecs[]) {
    		this.typeSpecs = typeSpecs;
    	}
    	
    	boolean isEmpty() {
    		return typeSpecs.length == 0;
		}
    }
    
    //Object3
    public static class ObjectType {
    	private static final ObjectField[] emptyNestedSpecs 
    		= new ObjectField[0];
    	public final BT_Class clazz;
    	public final ObjectField fields[];
    	
    	private ObjectType(BT_Class clazz) {
    		this(clazz, emptyNestedSpecs);
    	}
    	
    	private ObjectType(BT_Class clazz, ObjectField fields[]) {
    		this.clazz = clazz;
    		this.fields = fields;
    	}
    }
    
    //f1{} or f2{} or f3{} or g1{}
	public static class ObjectField {
    	public final BT_Field field;//the field can be inherited
    	public final MultipleObjectSpec spec;
    	
    	private ObjectField(BT_Field field, MultipleObjectSpec spec) {
    		this.field = field;
    		this.spec = spec;
    	}
    }
    
	private static class FieldSet {
		final MultipleObjectSpec fieldSpec;
		BT_FieldVector fields;
		
		FieldSet(MultipleObjectSpec m) {
			fieldSpec = m;
		}
	}
   
	//this field represents the identifier prior to evaulation
	final MultipleObjectIdentifier spec;
	
	IdentifierArguments(String s) throws InvalidSpecException {
		spec = MultipleObjectIdentifier.parse(s);
	}
	
	public MultipleObjectSpec evaluate(JaptRepository repository) 
			throws InvalidIdentifierException {
		return getSpec(repository, spec);
    }
    
	private static MultipleObjectSpec getSpec(JaptRepository repository, MultipleObjectIdentifier m) 
			throws InvalidIdentifierException {
		ObjectIdentifier objectIdents[] = m.objectIdents;
		ObjectSpec ospecs[] = new ObjectSpec[objectIdents.length];
		for(int i=0; i<objectIdents.length; i++) {
			ospecs[i] = getSpec(repository, objectIdents[i]);
		}
    	return new MultipleObjectSpec(ospecs);
    }
    
    //TODO might return a 0-length spec
    private static ObjectSpec getSpec(JaptRepository repository, ObjectIdentifier o) throws
    		InvalidIdentifierException {
    	if(!o.hasFields()) {
    		return getSimpleObjectSpec(repository, o.identifier);
    	}
    	ObjectAndFieldsIdentifier objectAndFieldsIdent = (ObjectAndFieldsIdentifier) o;
		MultipleObjectSpec firstFieldSpec = 
			getSpec(repository, objectAndFieldsIdent.initialFieldContents);
		Identifier fieldIdentifier = objectAndFieldsIdent.identifier;
		
		
	    class FieldReferenceActor extends ClassActor {
		   class FieldReference {
				BT_Field field;
				BT_Class throughClass;
				
				FieldReference(BT_Class thruClass, BT_Field field) {
					this.field = field;
					this.throughClass = thruClass;
				}
			}   
		   		
	   		ArrayList fieldReferences = new ArrayList();
			
			public void actOnClass(BT_Class referencedClass) {}
					
			public void actOnField(BT_Class referencedClass, BT_Field field) {
				fieldReferences.add(new FieldReference(referencedClass, field));
			}
			
			public FieldReference[] getReferences() {
				return (FieldReference[]) 
					fieldReferences.toArray(new FieldReference[fieldReferences.size()]);
			}
		}
	   
		FieldReferenceActor actor = new FieldReferenceActor();
		
		//TODO when we find fields, inaccessible fields are not found, and duplicates are not found (ie
		//fields in super classes with the same names as fields in subclasses.
		//Also keep in mind that we will end up treating static fields differently.
		repository.findFields(fieldIdentifier, actor);
		
		FieldReferenceActor.FieldReference[] firstFieldReferences = actor.getReferences();
		if(firstFieldReferences.length > 0) {
			ArrayList finalList = new ArrayList();
			if(!objectAndFieldsIdent.hasMultipleFields()) {
				if(firstFieldSpec.isEmpty()) {
					HashSet set = new HashSet();
					for(int i=0; i<firstFieldReferences.length; i++) {
						BT_Class clazz = firstFieldReferences[i].throughClass;
						if(set.contains(clazz)) {
							continue;
						}
						set.add(clazz);
						ObjectType specWithFields = new ObjectType(clazz);
						finalList.add(specWithFields);
					}
				} else {
					for(int i=0; i<firstFieldReferences.length; i++) {
						BT_Field field = firstFieldReferences[i].field;
						BT_Class clazz = firstFieldReferences[i].throughClass;
						ObjectField nestedSpec1 = new ObjectField(field, firstFieldSpec);
						ObjectType specWithFields = 
							new ObjectType(clazz, new ObjectField[] {nestedSpec1});
						finalList.add(specWithFields);
					}
				}
			} else {
				FieldIdentifier sets[] = objectAndFieldsIdent.fields;
				FieldSet fieldSets[] = new FieldSet[sets.length];
	    		for(int i=0; i<sets.length; i++) {
	    			//TODO the spec might have empty contents
	    			//if this happends, we should readjust the fieldSets array
	    			//BUT also have to adjust the sets array!!
	    			MultipleObjectSpec multiSpec = getSpec(repository, sets[i].fieldContents);
	    			fieldSets[i] = new FieldSet(multiSpec);
	    		}
	    		
				ObjectField fieldArray[] = new ObjectField[sets.length];
				RecurseState state = new RecurseState();
				state.currentSelection = fieldArray;
				state.finalList = finalList;
				for(int i=0; i<firstFieldReferences.length; i++) {
					BT_Field field = firstFieldReferences[i].field;
					BT_Class clazz = firstFieldReferences[i].throughClass;
					int totalSets = 0;
					for(int j=0; j<sets.length; j++) {
						MemberCollectorActor memberActor = new MemberCollectorActor(null, new BT_FieldVector());
						MemberSelector.PatternSelector spec = 
							new MemberSelector.PatternSelector(
									sets[j].field.getPattern(), true, false, true);
						memberActor.actOn(clazz, spec);
						if(memberActor.fields.size() > 0) {
							fieldSets[j].fields = memberActor.fields;
							totalSets++;
						} else {
							fieldSets[j].fields = null;
						}
					}
					FieldSet adjustedFieldSets[];
					if(totalSets < fieldSets.length && fieldSets.length > 0) {
						adjustedFieldSets = new FieldSet[totalSets];
						for(int k=fieldSets.length - 1; k>=0; k--) {
							if(fieldSets[k] != null) {
								adjustedFieldSets[--totalSets] = fieldSets[k];
							}
						}
					} else {
						adjustedFieldSets = fieldSets;
					}
					state.sets = adjustedFieldSets;
					state.referencedClass = clazz;
					state.firstField = firstFieldSpec.isEmpty() ? null : new ObjectField(field, firstFieldSpec);
					if(state.firstField == null && adjustedFieldSets.length == 0) {
						finalList.add(new ObjectType(clazz));
					} else {
						createObjectFieldsObjects(state, 0);
						addSpecs(state, 0);
					}
				}
			}
			//TODO either check when adding to the finalList list for duplicates or check now and remove duplicates
			//need an equals method in ObjectType for that
			//probably best to encapsulate the arraylist in a class and add the check methods there
			ObjectType classSpecs[] = (ObjectType[]) 
				finalList.toArray(new ObjectType[finalList.size()]);
			ObjectSpec spec = new ObjectSpec(classSpecs);
			return spec;
		}
		//TODO: ignore the fields, just try to find the classes?
		return new ObjectSpec();
    }

	private static ObjectSpec getSimpleObjectSpec(JaptRepository repository, Identifier classIdentifier) 
			throws InvalidIdentifierException {
		BT_ClassVector classes = repository.findClasses(classIdentifier, false);
		ObjectType classSpecs[] = new ObjectType[classes.size()];
		for(int i=0; i<classes.size(); i++) {
			classSpecs[i] = new ObjectType(classes.elementAt(i));
		}
		ObjectSpec spec = new ObjectSpec(classSpecs);
		return spec;
	}
    
    static class RecurseState {
    	BT_Class referencedClass;
		ObjectField firstField;
		FieldSet sets[];
		ObjectField currentSelection[];
		ArrayList finalList;
		ObjectField objectFields[][];
    }
    
    private static void createObjectFieldsObjects(RecurseState state, int index) {
    	if(index < state.sets.length) {
    		BT_FieldVector fields = state.sets[index].fields;
    		state.objectFields[index] = new ObjectField[fields.size()];
    		for(int i=0; i<fields.size(); i++) {
    			//TODO test whether all object types in spec (state.sets[index].fieldSpec)
				//can be stored in the given field (fields.elementAt(i)), and if not we
				//should readjust the spec
				//If none can be stored in field, we should readjust the size of the objectField array (state.objectFields[index])
				//This is probably a little tricky to get right because currentSelection and and other things referred to be index
				//then also need adjusting (here and in addSpecs below) 
				state.objectFields[index][i] 
					= new ObjectField(fields.elementAt(i), state.sets[index].fieldSpec);
				createObjectFieldsObjects(state, index + 1);
    		}
    	}
    }
    
    private static void addSpecs(RecurseState state, int index) {
    	if(index < state.sets.length) {
    		for(int i=0; i<state.objectFields[index].length; i++) {
    			state.currentSelection[index] = state.objectFields[index][i];
    			addSpecs(state, index + 1);
    		}
    	} else {
    		ObjectType classSpec = constructSelectedSpec(state);
    		state.finalList.add(classSpec);
    	}
    }

	private static ObjectType constructSelectedSpec(RecurseState state) {
		ObjectField currentSelection[] = state.currentSelection;
		int length = currentSelection.length;
		ObjectField specs[];
		if(state.firstField != null) {
			specs = new ObjectField[++length];
			specs[0] = state.firstField;
		} else {
			specs = new ObjectField[length];
		}
		for(int k = currentSelection.length; k > 0;) {
			specs[--length] = currentSelection[--k];
		}
		return new ObjectType(state.referencedClass, specs);
	}

}

