package javax.realtime;


public class LTMemory extends ScopedMemory
{
	
	public LTMemory(long size) {
		super(size);
	}
	
	public LTMemory(long size, Runnable logic) {
		super(size,logic);
	}
	
	public LTMemory(long initial, long maximum) {
		super(initial, maximum, true);
	}
	
	public LTMemory(long initial, long maximum, Runnable logic) {
		super(initial, maximum, logic, true);
	}
	
	public LTMemory(SizeEstimator size) {
		super(size);
	}
	
	public LTMemory(SizeEstimator size, java.lang.Runnable logic) {
		super(size,logic);
	}
	
	public LTMemory(SizeEstimator initial, SizeEstimator maximum) {
		super(initial, maximum);
	}
	
	public LTMemory(SizeEstimator initial, SizeEstimator maximum, java.lang.Runnable logic) {
		super(initial, maximum, logic);
	}
}
