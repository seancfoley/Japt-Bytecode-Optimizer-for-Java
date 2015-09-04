package com.ibm.ive.tools.japt;

import java.util.HashSet;

import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassVector;
import com.ibm.jikesbt.BT_Field;
import com.ibm.jikesbt.BT_Member;
import com.ibm.jikesbt.BT_Method;
import com.ibm.jikesbt.BT_MethodSignature;
import com.ibm.jikesbt.BT_Repository;

/**
 *
 * A generic comparator for specifying fields and methods
 * @author sfoley
 */
public abstract class MemberSelector {
	
	/**
	 * @return true if the member has been specified by this MemberComparator
	 * @param comparingClass the class through which a reference has been specified
	 * @param member either a member of the comparingClass or of a parent class or interface, 
	 * it can be reached through a reference to comparingClass
	 */
	public abstract boolean isSelected(BT_Class referencedClass, BT_Method member);
	
	/**
	 * @return true if the member has been specified by this MemberComparator
	 * @param comparingClass the class through which a reference has been specified
	 * @param member either a member of the comparingClass or of a parent class or interface, 
	 * it can be reached through a reference to comparingClass
	 */
	public abstract boolean isSelected(BT_Class referencedClass, BT_Field member);
	
	/**
	 * @return true if the class might conceivably have declared a member specified by this MemberComparator.
	 * This can prevent calling isSelected(BT_Class comparingClass, BT_Member member) on all members of a class when
	 * a match is not possible.  This default implementation returns true.
	 * @param comparingClass the class through which a reference has been specified
	 */
	public boolean isSelected(BT_Class declaringClass) {
		return true;
	}
	
	/**
	 * if isSelected(BT_Class, BT_Method) returns false for all methods in a subclass of this class, then 
	 * override this method to return false for better performance.  This method determines
	 * if each individual BT_Method will be checked by isSelected(BT_Class, BT_Method).  
	 * This method is implemented to return true.
	 * @return true
	 */
	public boolean selectsMethods() {
		return true;
	}
	
	/**
	 * if isSelected(BT_Class, BT_Field) returns false for all fields in a subclass of this class, then 
	 * override this method to return false for better performance.  This method determines
	 * if each individual BT_Field will be checked by isSelected(BT_Class, BT_Field).  
	 * This method is implemented to return true.
	 * @return true
	 */
	public boolean selectsFields() {
		return true;
	}
	
	/**
	 * Returns whether this is a resolvable selector.  A non-resolvable selector
	 * will only search the declared members of a given class for specified methods and
	 * fields.  A resolvable selector will also search parent classes and interfaces for inherited
	 * methods and fields.
	 * 
	 * @return whether or not specified members can be inherited.  If false, then all
	 * calls to isSelected will be restricted to those members declared by the given class argument.
	 * If true, then calls to isSelected will include inherited members.
	 */
	public boolean isResolvable() {
		//return false;
		return true;
	}
	
	
	/**
	 * @return true if the member is accessible from the subclass or subinterface comparingClass
	 */
	boolean isAccessibleFrom(BT_Class comparingClass, BT_Member member) {
		if(member.isPrivate()) {
			return member.getDeclaringClass().equals(comparingClass);
		} else if(member.isDefaultAccess()) {
			return member.getDeclaringClass().isInSamePackage(comparingClass);
		}
		return true;	
	}
	
	/**
	 * a selector that specifies public and protected resolvable members of a class
	 */
	public static final MemberSelector nonPrivateSelector = new MemberSelector() {
		public boolean isSelected(BT_Class clazz, BT_Field member) {
			return !member.isPrivate();
		}
		
		public boolean isSelected(BT_Class clazz, BT_Method member) {
			return !member.isPrivate();
		}
	};
	
	/**
	 * a selector that specifies public and protected resolvable members of a class
	 */
	public static final MemberSelector publicProtectedSelector = new MemberSelector() {
		public boolean isSelected(BT_Class clazz, BT_Field member) {
			return member.isPublic() || member.isProtected();
		}
		
		public boolean isSelected(BT_Class clazz, BT_Method member) {
			return member.isPublic() || member.isProtected();
		}
	};
	
	/**
	 * a selector that specifies public and protected non-resolvable methods of a class
	 */
	public static final MemberSelector publicProtectedNonResolvableMethodSelector = new MemberSelector() {
		public boolean isResolvable() {
			return false;
		}
		
		public boolean isSelected(BT_Class clazz, BT_Field member) {
			return false;
		}
		
		public boolean isSelected(BT_Class clazz, BT_Method member) {
			return member.isPublic() || member.isProtected();
		}
	};
	
	
	/**
	 * a selector that specifies public non-static resolvable members of a class
	 */
	public static final MemberSelector publicNonStaticMethodSelector = new MemberSelector() {		
		public boolean isSelected(BT_Class clazz, BT_Field member) {
			return false;
		}
		
		public boolean isSelected(BT_Class clazz, BT_Method member) {
			return !member.isStatic() && member.isPublic();
		}
		
		public boolean selectsFields() {
			return false;
		}
	};
	
	/**
	 * a selector that specifies public non-static resolvable members of a class
	 */
	public static final MemberSelector publicNonStaticNonResolvableMethodSelector = new MemberSelector() {
		
		public boolean isSelected(BT_Class clazz, BT_Method member) {
			return !member.isStatic() && member.isPublic();
		}
		
		public boolean isSelected(BT_Class clazz, BT_Field member) {
			return false;
		}
		
		public boolean isResolvable() {
			return false;
		}
		
		public boolean selectsFields() {
			return false;
		}
	};
	
	/**
	 * a selector that specifies all members of a class, not including subclasses and subinterfaces
	 */
	public static final MemberSelector allNonResolvableMethodSelector = new MemberSelector() {
		public boolean isResolvable() {
			return false;
		}
		
		public boolean isSelected(BT_Class clazz, BT_Method member) {
			return true;
		}
		
		public boolean isSelected(BT_Class clazz, BT_Field member) {
			return false;
		}
	};
	
	/**
	 * a selector that specifies all members of a class, not including subclasses and subinterfaces
	 */
	public static final MemberSelector privateResolvableSelector = new MemberSelector() {
		public boolean isResolvable() {
			return false;
		}
		
		public boolean isSelected(BT_Class clazz, BT_Method member) {
			return member.isPrivate();
		}
		
		public boolean isSelected(BT_Class clazz, BT_Field member) {
			return member.isPrivate();
		}
	};
	
	/**
	 * a selector that specifies all members of a class, not including subclasses and subinterfaces
	 */
	public static final MemberSelector privatePackageResolvableSelector = new MemberSelector() {
		public boolean isResolvable() {
			return false;
		}
		
		public boolean isSelected(BT_Class clazz, BT_Method member) {
			return member.isPrivate() || member.isDefaultAccess();
		}
		
		public boolean isSelected(BT_Class clazz, BT_Field member) {
			return member.isPrivate() || member.isDefaultAccess();
		}
	};
	
	/**
	 * a selector that specifies all members of a class, including those visible in subclasses ans subinterfaces
	 */
	public static final MemberSelector allSelector = new MemberSelector() {
		public boolean isSelected(BT_Class clazz, BT_Method member) {
			return isAccessibleFrom(clazz, member);
		}
		
		public boolean isSelected(BT_Class clazz, BT_Field member) {
			return isAccessibleFrom(clazz, member);
		}
	};
	
	/**
	 * a selector that specifies the single resolved main method of a class
	 */
	public static final MemberSelector mainMethodSelector = new MemberSelector() {
		private boolean wasFound;
		
		public boolean isSelected(BT_Class comparingClass, BT_Method member) {
			if(!wasFound && member.isStatic() && member.getName().equals("main")) {
				BT_MethodSignature sig = member.getSignature();
				BT_ClassVector types = sig.types;
				if(types.size() == 1 && types.elementAt(0).getName().equals(BT_Repository.JAVA_LANG_STRING_ARRAY)) {
					return wasFound = true;
				}
			}
			return false;
		}
			
		public boolean isSelected(BT_Class comparingClass, BT_Field member) {
			return false;
		}
		
		public boolean selectsFields() {
			return false;
		}
	};
	
	/**
	 * a selector that specifies that the member name must conform to a pattern.
	 * Each string is matched only once (ie if the pattern matches the method name ABC then any further
	 * method named ABC will not be a match)
	 */	
	public static class PatternSelector extends ResolvingSelector {
		PatternString pattern;
		
		public PatternSelector(PatternString pattern, 
				boolean resolvable, boolean matchMethods, boolean matchFields) {
			super(resolvable, matchMethods, matchFields);
			this.pattern = pattern;
		}
		
		public boolean isSelected(BT_Class clazz, BT_Method member) {
			if(pattern.isMatch(member.getName()) 
					&& !previouslyMatchedMethod(member)
					&& isAccessibleFrom(clazz, member)
					&& !isRestrictedResolution(clazz, member)) {
				noteMethodMatch(member);
				return true;
			}
			return false;
		}
		
		public boolean isSelected(BT_Class clazz, BT_Field member) {
			if(pattern.isMatch(member.getName()) 
					&& !previouslyMatchedField(member)
					&& isAccessibleFrom(clazz, member)) {
				noteFieldMatch(member);
				return true;
			}
			return false;
		}
	};

	public static class NameTypePatternSelector extends ResolvingSelector {
		PatternString namePattern, typePattern;
		
		public NameTypePatternSelector(PatternString nameString, PatternString typeString, 
				boolean resolvable, boolean matchMethods, boolean matchFields) {
			super(resolvable, matchMethods, matchFields);
			this.namePattern = nameString;
			this.typePattern = typeString;
		}
		
		public boolean isSelected(BT_Class clazz, BT_Method member) {
			if(namePattern.isMatch(member.getName())) {
				//we support both ways of listing the parameters, either VM specification style or java language style
				if(typePattern.isMatch(member.getSignature().toExternalArgumentString()) //java language style
					|| typePattern.isMatch(member.getSignature().toParameterString())) { //vm style
						if(isAccessibleFrom(clazz, member)
							&& !previouslyMatchedMethod(member)
							&& !isRestrictedResolution(clazz, member)) {
							noteMethodMatch(member);
							return true;
						}
				}
			}
			return false;
		}
		
		public boolean isSelected(BT_Class clazz, BT_Field member) {
			if(namePattern.isMatch(member.getName()) 
				&& typePattern.isMatch(member.getTypeName()) 
				&& !previouslyMatchedField(member)
				&& isAccessibleFrom(clazz, member)) {
				noteFieldMatch(member);
				return true;
			}
			return false;
		}
	}
	
	private static abstract class ResolvingSelector extends MemberSelector {
		//we do not match the same string twice, so we must keep track of previous matches
		private HashSet previouslyMatchedFields;
		private HashSet previouslyMatchedMethods;
		
		//directives for the behaviour of this selector
		boolean resolvable;
		boolean matchMethods;
		boolean matchFields;
		
		//flags for the user to check if a match was made
		boolean wasFound;
		boolean methodWasFound;
		boolean fieldWasFound;
		
		ResolvingSelector(
				boolean resolvable, 
				boolean matchMethods, 
				boolean matchFields) {
			if(!(matchMethods || matchFields)) {
				throw new IllegalArgumentException();
			}
			this.resolvable = resolvable;
			this.matchMethods = matchMethods;
			this.matchFields = matchFields;
		}
		
		boolean previouslyMatchedMethod(BT_Method member) {
			return previouslyMatchedMethods != null 
				&& previouslyMatchedMethods.contains(member.qualifiedName());
		}
		
		boolean previouslyMatchedField(BT_Field member) {
			return previouslyMatchedFields != null 
				&& previouslyMatchedFields.contains(member.getName());
		}
		
		void noteMethodMatch(BT_Method member) {
			if(previouslyMatchedMethods == null) {
				previouslyMatchedMethods = new HashSet();
			}
			previouslyMatchedMethods.add(member.qualifiedName());
			methodWasFound = wasFound = true;
		}
		
		void noteFieldMatch(BT_Field member) {
			if(previouslyMatchedFields == null) {
				previouslyMatchedFields = new HashSet();
			}
			previouslyMatchedFields.add(member.getName());
			fieldWasFound = wasFound = true;
		}
		
		public boolean isResolvable() {
			return resolvable;
		}
		
		public boolean selectsMethods() {
			return matchMethods;
		}
		
		public boolean selectsFields() {
			return matchFields;
		}
		
		/**
		 * @return true if the method cannot be resolved to a superclass or superinterface
		 * because it is a constructor or static initializer
		 */
		boolean isRestrictedResolution(BT_Class comparingClass, BT_Method method) {
			if(method.isConstructor() || method.isStaticInitializer()) {
				return !comparingClass.equals(method.getDeclaringClass());
			}
			return false;
		}
		
	}
	

}
