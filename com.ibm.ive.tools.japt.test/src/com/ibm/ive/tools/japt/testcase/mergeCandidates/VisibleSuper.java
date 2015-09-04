package com.ibm.ive.tools.japt.testcase.mergeCandidates;

public class VisibleSuper {
	
	void access() {
		System.out.println("done");
	}
	
	public static class Sub1 extends HiddenSuper {}

	public static class Sub2 extends HiddenSuper {}
}

class HiddenSuper extends VisibleSuper {}


