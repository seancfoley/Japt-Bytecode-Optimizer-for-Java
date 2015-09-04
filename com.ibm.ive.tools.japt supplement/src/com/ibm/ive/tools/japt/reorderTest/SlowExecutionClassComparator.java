/*
 * Created on Dec 19, 2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.ive.tools.japt.reorderTest;

import com.ibm.jikesbt.BT_Class;
import com.ibm.jikesbt.BT_ClassComparator;

public class SlowExecutionClassComparator implements BT_ClassComparator {
	
	public int compare(BT_Class one, BT_Class two) {
		int callIndex1 = getCallIndex(one);
		int callIndex2 = getCallIndex(two);
		return compare(callIndex1, callIndex2);
	}

	/**
	 * @param callIndex1
	 * @param callIndex2
	 * @return
	 */
	private int compare(int callIndex1, int callIndex2) {
		boolean oneIsOdd = (callIndex1 % 2) != 0;
		boolean twoIsOdd = (callIndex2 % 2) != 0;
		if(oneIsOdd) {
			if(twoIsOdd) {
				return callIndex1 - callIndex2;
			}
			return 1;
		} else {
			if(twoIsOdd) {
				return -1;
			}
			return callIndex1 - callIndex2;
		}
	}
	
	//	basename, baseName2, baseName4, ..., baseName2n, baseName1, baseName3, ...., baseName(2n-1)
	//count == 6: 0, 2, 4, 1, 3, 5, so counter == 1 is mapped to index == 3 and counter == 4 is mapped to index == 2
	//count == 7: 0, 2, 4, 6, 1, 3, 5, so counter == 1 is mapped to index == 4 and counter == 6 is mapped to index == 3
	
	public static int getOrdinal(int callIndex, int totalClasses) {
		boolean isEven = (callIndex % 2) == 0;
		int number = (callIndex + 1) / 2;
		int half = ((totalClasses + 1) / 2) - 1;
		return isEven ? number : half + number;
	}
	
	static int getCallIndex(BT_Class clazz) {
		String name = clazz.getName();
		try {
			String count = name.substring(CreateClassesExtension.baseName.length());
			if(count.length() == 0) {
				return 0;
			}
			return Integer.parseInt(count);
		} catch(IndexOutOfBoundsException e) {
			return -1;
		} catch(NumberFormatException e) {
			return -1;
		}
	}

}
