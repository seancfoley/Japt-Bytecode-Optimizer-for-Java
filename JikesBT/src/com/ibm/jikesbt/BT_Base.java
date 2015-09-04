package com.ibm.jikesbt;

/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 1998, 2003
 * All rights reserved
 */

import java.io.OutputStream;
import java.io.PrintStream;

/**
 Simple utility static methods and fields commonly used by its subclasses.

 <p> This is public only so javadoc can find some things.
 It is especially subject to change, so don't depend on it.

 * @author IBM
**/
abstract class BT_Base {

	/**
	 A private compilation constant that determines whether BT_Base
	 should complain if it notices any callers doing tracing.
	 @see #CHECK_JIKESBT
	 @see #CHECK_USER
	 @see #traceln
	 @see #traceStream
	 @version _version 1_
	 @version _version 2_
	**/
	static final boolean ALLOW_TRACING = false;

	/**
	 A compilation constant used throughout JikesBT to determine whether
	 checks of JikesBT's internal behavior should be made.
	 Hopefully, whether this is true or false will only affect
	 performance.
	
	 <p> Note that since so many of JikesBT's fields are public, JikesBT
	 cannot really guarantee that they are set consisently, so if the
	 checks enabled by this constant find a problem, it may be due to
	 an application error.
	
	 <p> This is exposed (protected) only so javadoc will show it.
	 Applications shouldn't use it since it is likely to change.
	
	 @see #CHECK_USER
	 @see #CHECK_JIKESBT_THOROUGHLY
	**/
	static final boolean CHECK_JIKESBT = false;

	
	/**
	 A compilation constant used throughout JikesBT to determine
	 whether methods should quickly check that they were called
	 correctly, which can mean that the arguments are correct and also
	 that JikesBT's model is correct.
	 Hopefully, whether this is true or false will only affect performance.
	
	 <p> See <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>model consistency and checking</a>.
	
	 <p> Usage guideline:  Checks guarded with this constant should
	 not be much slower than comparing for string equality or calling
	 a trivial method.
	
	 <p> This is exposed (protected) only so javadoc will show it.
	 Applications shouldn't use it since it is likely to change.
	
	 @see #CHECK_JIKESBT
	 @see #CHECK_USER_THOROUGHLY
	**/
	protected static final boolean CHECK_USER = false;

	/**
	 Similar to {@link BT_Base#CHECK_USER} except that this controls slower checks.
	 Hopefully, whether this is true or false will only affect performance.
	
	 <p> See <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>model consistency and checking</a>.
	
	 <p> Usage guideline:  Checks guarded with this constant should be
	 slower than calling a trivial method.  E.g., this should be used
	 to guard searching a collection.
	
	 <p> This is exposed (protected) only so javadoc will show it.
	 Applications shouldn't use it since it is likely to change.
	
	 @see #CHECK_USER
	 @see #CHECK_JIKESBT_THOROUGHLY
	**/
	static final boolean CHECK_USER_THOROUGHLY = false;

	/**
	 Similar to {@link BT_Base#CHECK_JIKESBT} except that this controls slower checks.
	 Similar to {@link BT_Base#CHECK_USER_THOROUGHLY} except that if one of these checks
	 fails it is "certainly" a JikesBT bug.
	 Hopefully, whether this is true or false will only affect performance.
	
	 <p> See <a href=../jikesbt/doc-files/ProgrammingPractices.html#model_consistency>model consistency and checking</a>.
	
	 <p> Usage guideline:  Checks guarded with this constant should be
	 slower than calling a trivial method.  E.g., this should be used
	 to guard searching a collection.
	
	 <p> This is exposed (protected) only so javadoc will show it.
	 Applications shouldn't use it since it is likely to change.
	
	 @see #CHECK_JIKESBT
	 @see #CHECK_USER_THOROUGHLY
	**/
	static final boolean CHECK_JIKESBT_THOROUGHLY = false;

	/**
	 Can be set true while testing JikesBT to fail softly to support random testing.
	 True when ship;  false when run test 4.
	**/
	static final boolean FAIL_NORMALLY = true;

	/**
	 More portable than "\n" (at least on NT 4 where "\n" != cr-lf).
	 Just a convenient name for System.getProperties().getProperty("line.separator").
	**/
	public static String endl() {
		return endl_;
	}
	
	private static final String endl_ =
		System.getProperties().getProperty("line.separator");

	private static int threadStackDepth() {
		class LineCounter extends OutputStream {
			private int eolCount = 0;
			
			public void write(int b) {
				if (b == '\n') {
					++eolCount;
				}
			}
		}
		LineCounter counter = new LineCounter();
		(new Throwable()).printStackTrace(new PrintStream(counter));
		return counter.eolCount;
	}

	private static String traceLinePrefix() {
		int depth = threadStackDepth();
		String dS = "   " + depth;
		return dS.substring(dS.length() - 3)
			+ "  .  :   .  .  :  .  .  |  .  .  :  .  .  :  .  .  :".substring(
				0,
				depth % 25);
	}

	/**
	 Calls BT_Factory.factory.fatal, and if that returns (which is illegal), this prints a stackdump.
	**/
	static final void fatal(String message, Exception ex) {
	   new Exception(Messages.getString("JikesBT.fatal_JikesBT_Error___4")+message+" "+ex).printStackTrace();
	}
	/**
	 Calls BT_Factory.factory.fatal, and if that returns (which is illegal), this prints a stackdump.
	**/
	static final void fatal(String message) {
	   new Exception(Messages.getString("JikesBT.fatal_JikesBT_Error___4")+message).printStackTrace();
	}

	
	/**
	 Handles the second half of an assertion -- what to do when it fails.
	 Always causes a fatal error.
	 This form is a more efficient than the others since the assert
	 method is not called unless the desired condition has already
	 been found false and its arguments are not evaluated.
	
	 <p> This also allows simpler coding of some message text.
	 E.g., <code><pre>
	 *   if(CHECK_JIKESBT && ref!=null && ref.isBad) assert( ref + " is bad")
	 </pre></code> instead of <code><pre>
	 *   if(CHECK_JIKESBT) assert( ref==null || ! ref.isBad) assert( (ref!=null ? ref.toString() : "?") + " is bad")
	 </pre></code>
	
	 <p> E.g., <code><pre>
	 *   if( {@link BT_Base#CHECK_JIKESBT} && x!=4)  assert( "x is " + x);
	 </pre></code>
	**/
	protected static final void assertFailure(String message) {
		fatal(Messages.getString("JikesBT.Assertion_failure___8") + message);
	}

	/**
	 A soft version of {@link BT_Base#assert}
	 E.g., <code><pre>
	 *   if( {@link BT_Base#CHECK_JIKESBT} && x!=4)  expect( "x is " + x);
	 </pre></code>
	**/
	protected static final void expect(String message) {}

	/**
	 Only for debugging/development of JikesBT.
	**/
	static PrintStream traceStream() {
		// This test must be made only after BT_Factory.factory is set -- i.e., not in an initializer
		return ALLOW_TRACING
		        ? System.err
			: new PrintStream(new OutputStream() {
				public void write(int b) throws java.io.IOException {}
			});
	}

	/**
	 Internal -- only for debugging/development of JikesBT.
	**/
	static void traceln(String text) {
		traceStream().println("  t " + traceLinePrefix() + text);
	}

	/**
	 Must be in a BT_... class instead of BTI_... so shipped source will
	 compile (since BTI_... is not shipped).
	**/
	static void _queryJikesBTReadyToShip_want(
		String name,
		boolean got,
		boolean want) {
		System.out.println(
			"  * "
				+ ((got == want) ? Messages.getString("JikesBT.ok_____204") : Messages.getString("JikesBT.WRONG__205"))
				+ "  "
				+ name
				+ " = "
				+ got);
	}
	
	/**
	 Internal -- for JikesBT maintenance.
	**/
	static void queryJikesBTReadyToShip() {
		_queryJikesBTReadyToShip_want(
			"BT_Base.ALLOW_TRACING",
			BT_Base.ALLOW_TRACING,
			false);
		_queryJikesBTReadyToShip_want(
			"BT_Base.CHECK_JIKESBT",
			BT_Base.CHECK_JIKESBT,
			true);
		_queryJikesBTReadyToShip_want(
			"BT_Base.CHECK_JIKESBT_THOROUGHLY",
			BT_Base.CHECK_JIKESBT_THOROUGHLY,
			false);
		_queryJikesBTReadyToShip_want(
			"BT_Base.CHECK_USER",
			BT_Base.CHECK_USER,
			true);
		_queryJikesBTReadyToShip_want(
			"BT_Base.CHECK_USER_THOROUGHLY",
			BT_Base.CHECK_USER_THOROUGHLY,
			false);
		_queryJikesBTReadyToShip_want(
			"BT_Base.FAIL_NORMALLY",
			BT_Base.FAIL_NORMALLY,
			true);
	}

}
