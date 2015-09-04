package com.ibm.ive.tools.japt.testcase;

import java.io.IOException;

/*
 * This class contains methods with jsrs, nested jsrs, catch blocks and various configurations of these.
 * This is particularly designed for testing jsr inling, but it is also beneficial to test all japt transformations
 * on methods with jsrs.
 */
public class TestJSR {

	public static void main(String[] args) {
		try {
			basic();
		} catch(IOException e) {
			System.out.println(e);
		}
		basic2();
		try {
			System.out.println(scenario());
		} catch(Throwable t) {
			System.out.println(t);
		}
		nested();
		System.out.println(count());
	}
	
	public static void basic() throws IOException {
        System.out.println("Start basic");
        try {
            throw new IOException();
        } catch (RuntimeException e) {
            System.out.println("Catch any runtime");
        } finally {
            System.out.println("In finally");
        }
        System.out.println("basic end");
    }
	
	public static void basic2() {
        System.out.println("Start basic2");
        try {
            throw new IOException();
        } catch (IOException e) {
            System.out.print("Caught: ");
            System.out.println(e);
        } finally {
            System.out.println("In finally");
        }
        System.out.println("basic2 end");
    }
	
	private static int scenario() {
		int i = 0;
    	try {
                System.out.println( "i: " + ++i );
                return i;
        } catch(Exception e) {
                System.out.println( "catch" );
                return 2;
        } finally {
                System.out.println( "finally" );
                try {
                	throwRuntime();
                } finally {
                	clean();
                }
        }
	}
	
	private static void throwRuntime() {
		throw new RuntimeException( "error" );
	}
	
	private static void clean() {
		System.out.println( "clean" );
	}
	
	static void doThrow(RuntimeException e) {
		throw e;
	}
	
	/* test subroutines nested inside handlers and handlers nested inside subroutines */
	private static void nested() {
		try {
			try {
				System.out.print("1. Entered ");
				try {
					System.out.print("three ");
					try {
						System.out.println("handlers.");
					} finally {
						RuntimeException e = new RuntimeException("runtime exception");
						System.out.print("2. Threw " + e.getMessage());
						doThrow(e);
						try {
							System.out.print("Should never reach here ");
						} finally {
							System.out.println("or here.");
						}
					}
				} catch(RuntimeException e) {
					System.out.println(" and caught " + e.getMessage() + '.');
				} finally {
					System.out.print("3. Moved into finally, ");
					try {
						System.out.print("entered another handler ");
					} finally {
						System.out.println("and executed the finally clause.");
					}
				}
				RuntimeException e = new IllegalArgumentException("another runtime exception");
				System.out.println("4. Threw " + e.getMessage() + '.');
				doThrow(e);
			} finally {
				System.out.print("5. Passed through outer finally, ");
				try {
					System.out.print("and entered ");
					try {
						System.out.println("another handler.");
					} finally {
						System.out.println("6. Passed through innermost finally.");
					}	
				} finally {
					System.out.print("7. Passed through another finally ");
					try {
						System.out.print("which brought us to another handler ");
					} finally {
						System.out.println("and another finally.");
					}
				}
			}
		} catch(IllegalArgumentException e) {
			System.out.print("8. Caught " + e.getMessage());
		} finally {
			System.out.println(" and executed last finally.");
		}
	}
	
	/* Very similar to nested, but has just a single instruction inside most blocks */
	private static int count() {
		/* it takes just a single instruction to increment a local */
		int i=0;
		try {
			try {
				i++;
				try {
					i++;
					try {
						i++;
					} finally {
						RuntimeException e = new RuntimeException("runtime exception");
						i++;
						doThrow(e);
						try {
							i++; //should never reach here
						} finally {
							i++; //or here
						}
					}
				} catch(RuntimeException e) {
					i++;
				} finally {
					i++;
					try {
						i++;
					} finally {
						i++;
					}
				}
				RuntimeException e = new IllegalArgumentException("another runtime exception");
				i++;
				doThrow(e);
			} finally {
				try {
					i++;
					try {
						i++;
					} finally {
						i++;
					}	
				} finally {
					i++;
					try {
						i++;
					} finally {
						i++;
					}
				}
			}
		} catch(IllegalArgumentException e) {
			i++;
		} finally {
			i++;
		}
		i++;
		return i;
	}
}
