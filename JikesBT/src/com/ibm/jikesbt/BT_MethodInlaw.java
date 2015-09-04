package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Represents a pair of
 {@link <a href=../jikesbt/doc-files/glossary.html#inlaws>inlaw</a>} methods
 and the class that caused the relationship.

 Whether objects of this class are generated and maintained depends on the setting
 of {@link BT_Factory#buildMethodRelationships}.

 * @author IBM
**/
public final class BT_MethodInlaw extends BT_Base /**implements Comparable**/
{

	BT_MethodInlaw(BT_Method m1, BT_Method m2, BT_Class c) {
		if (CHECK_JIKESBT && m1 == m2)
			assertFailure(Messages.getString("JikesBT.Inlaw_to_itself__{0}_1", m1));
		if (CHECK_JIKESBT && m1.isParentOf(m2))
			assertFailure(Messages.getString("JikesBT.{0}_isParentOf_{1}_2", new Object[] {m1, m2}));
		if (CHECK_JIKESBT && m1.isKidOf(m2))
			assertFailure(Messages.getString("JikesBT.{0}_isKidOf_{1}_3", new Object[] {m1, m2}));
		if (CHECK_JIKESBT && !m1.canBeInherited())
			assertFailure(Messages.getString("JikesBT.{0}_cannot_be_inherited_4", m1));
		if (CHECK_JIKESBT && !m2.canBeInherited())
			assertFailure(Messages.getString("JikesBT.{0}_cannot_be_inherited_4", m2));
		method1_ = m1;
		method2_ = m2;
		cls_ = c;
		tempMark_ = curMark_;
	}

	final BT_Method method1_;

	final BT_Method method2_;

	/**
	 Gets the method that is an inlaw of parameter "otherM".
	**/
	public BT_Method getOtherMethod(BT_Method otherM) {
		if (CHECK_USER && otherM != method1_ && otherM != method2_)
			assertFailure(
				otherM
					+ Messages.getString("JikesBT.{0}_does_not_match_either_{1}_nor_{2}_6",
						new Object[] {method1_, method2_}));
		return otherM != method1_ ? method1_ : method2_;
	}

	final BT_Class cls_;

	/**
	 Gets the class that caused the relationship.
	 I.e., the class that is a descendent of both
	**/
	public BT_Class getCls() {
		return cls_;
	}

	/**
	 For general use for marking pairs.
	 See {@link BT_MethodInlaw#curMark_}.
	**/
	int tempMark_ = 0;

	/**
	 The value that {@link BT_MethodInlaw#tempMark_} is currently being assigned and
	 compared to.
	**/
	static int curMark_ = 0;

	/**
	 @return  0 if the pairs contain the same methods and class;
	   otherwise -1 or 1 in an arbitrary but consistent fashion.
	**/
	public int compareTo(Object o) {
		BT_MethodInlaw that = (BT_MethodInlaw) o;

		int thisMin;
		int thisMax;
		if (this.method1_.hashCode() < this.method2_.hashCode()) {
			thisMin = this.method1_.hashCode();
			thisMax = this.method2_.hashCode();
		} else {
			thisMin = this.method2_.hashCode();
			thisMax = this.method1_.hashCode();
		}

		int thatMin;
		int thatMax;
		if (that.method1_.hashCode() < that.method2_.hashCode()) {
			thatMin = that.method1_.hashCode();
			thatMax = that.method2_.hashCode();
		} else {
			thatMin = that.method2_.hashCode();
			thatMax = that.method1_.hashCode();
		}

		if (thisMin < thatMin)
			return -1;
		if (thisMin > thatMin)
			return 1;
		if (thisMax < thatMax)
			return -1;
		if (thisMax > thatMax)
			return 1;
		if (this.cls_.hashCode() < that.cls_.hashCode())
			return -1;
		if (this.cls_.hashCode() > that.cls_.hashCode())
			return 1;
		return 0;
	}

	public String toString() {
		return Messages.getString("JikesBT.{0}___{1}_cause__{2}_9",
			new Object[] {method1_, method2_, cls_});
	}
}
