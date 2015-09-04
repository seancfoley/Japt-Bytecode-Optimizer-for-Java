package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */


/**
 Thrown when there are two classes with the same name in the repository.
 * @author IBM
**/
public final class BT_DuplicateClassException extends BT_Exception {

	BT_Class oldC_;
	public BT_Class getOld() {
		return oldC_;
	}

	public BT_DuplicateClassException(String explanation, BT_Class oldC) {
		super(explanation);
		if (BT_Base.CHECK_JIKESBT && oldC == null)
			BT_Base.assertFailure(Messages.getString("JikesBT.old_is_null_1"));
		oldC_ = oldC;
	}
}
