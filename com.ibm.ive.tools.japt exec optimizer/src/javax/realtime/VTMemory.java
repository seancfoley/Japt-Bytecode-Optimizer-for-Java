package javax.realtime;


public class VTMemory extends ScopedMemory
{
	public VTMemory(long size) {
		super(size);
	}

	public VTMemory(long size, Runnable logic) {
		super(size, logic);
	}

	public VTMemory(long initial, long maximum) {
		super(initial, maximum, true);
	}

	public VTMemory(long initial, long maximum, Runnable logic) {
		super(initial, maximum, logic, true);
	}

	public VTMemory(SizeEstimator size) {
		super(size);
	}

	public VTMemory(SizeEstimator size, java.lang.Runnable logic) {
		super(size, logic);
	}

	public VTMemory(SizeEstimator initial, SizeEstimator maximum) {
		super(initial, maximum);
	}

	public VTMemory(SizeEstimator initial, SizeEstimator maximum, java.lang.Runnable logic) {
		super(initial, maximum, logic);
	}
}
