package com.ibm.ive.tools.japt.obfuscation;

import java.util.*;
import com.ibm.jikesbt.*;
import com.ibm.ive.tools.japt.*;


/**
 * @author sfoley
 *
 */
public class MultipleClassNameGenerator {

	List list;
	BT_ClassVector classes;
	
	public class ListEntry {
		int frequency;
		String entry;
		
		ListEntry(String s) {
			this.frequency = 1;
			this.entry = s;
		}
		
		public boolean equals(Object o) {
			if(o instanceof ListEntry) {
				ListEntry other = (ListEntry) o;
				return other.entry.equals(entry);
			}
			return false;
		}
	}
	
	private void addEntry(HashMap entries, String string) {
		if(entries.containsKey(string)) {
			((ListEntry) entries.get(string)).frequency++;
		}
		else {
			entries.put(string, new ListEntry(string));
		}
	}
	
	public MultipleClassNameGenerator(BT_ClassVector classes, boolean addMemberNames, boolean reuseStrings) throws BT_ClassFileException {
		this.classes = classes;
		HashMap entries = new HashMap();
		for(int j=0; j<classes.size(); j++) {
			BT_Class clazz = classes.elementAt(j);
			if(reuseStrings) {
				LinkedList stringList = ConstantPoolNameGenerator.getStringList(clazz);
				for(int i=1; i<stringList.size(); i++) {
					String string = (String) stringList.get(i);
					addEntry(entries, string);
				}
			}
			
			if(addMemberNames) {
				//now try method and field names
				HashSet namesFound = new HashSet();
				BT_MethodVector methods = clazz.getMethods();
				for(int k=0; k<methods.size(); k++) {
					BT_Method method = methods.elementAt(k);
					String string = method.getName();
					if(!namesFound.contains(string)) {
						namesFound.add(string);
						if(!Identifier.isValidJavaIdentifier(string)) {
							continue;
						}
						addEntry(entries, string);
					}

				}
				
				BT_FieldVector fields = clazz.getFields();
				for(int k=0; k<fields.size(); k++) {
					BT_Field field = fields.elementAt(k);
					String string = field.getName();
					if(!namesFound.contains(string)) {
						addEntry(entries, string);
						if(!Identifier.isValidJavaIdentifier(string)) {
							continue;
						}
						namesFound.add(string);
					}
				}
			}
			
		
		}
		
		list = new ArrayList(entries.size());
		list.addAll(entries.values());
		
		//the list is sorted by shortest UTF8 length and then by frequency
		Collections.sort(list, new Comparator() {
				public int compare(Object o1, Object o2) {
					ListEntry l1 = (ListEntry) o1;
					ListEntry l2 = (ListEntry) o2;
					String s1 = l1.entry;
					String s2 = l2.entry;
					int s1Length = getUTF8Length(s1);
					int s2Length = getUTF8Length(s2);
					if(s1Length > s2Length) {
						return -1;
					}
					else if(s1Length < s2Length) {
						return 1;
					}
					else {
						return l1.frequency - l2.frequency;
					}
				}
				
				public boolean equals(Object obj) {
					return obj.getClass().equals(getClass());
				}
			}
		);
		
	}
	
	private static int getUTF8Length(String string) {
		return UTF8Converter.convertToUtf8(string).length;
	}
	
	/**
	 * take a look at the next name without dispensing it
	 */
	public String peekName() {
		if(!list.isEmpty()) {
			return ((ListEntry) list.get(0)).entry;
		}
		return null;
	}
	
	/**
	 * dispense the next name
	 */
	public String getName() {
		if(!list.isEmpty()) {
			return ((ListEntry) list.remove(0)).entry;
		}
		return null;
	}
	
	/**
	 * dispense the next entry (name and frequency)
	 */
	public ListEntry getEntry() {
		if(!list.isEmpty()) {
			return (ListEntry) list.remove(0);
		}
		return null;
	}
	
	/**
	 * take a look at the next entry (name and frequency) without dispensing it
	 */
	public ListEntry peekEntry() {
		if(!list.isEmpty()) {
			return (ListEntry) list.get(0);
		}
		return null;
	}

}
