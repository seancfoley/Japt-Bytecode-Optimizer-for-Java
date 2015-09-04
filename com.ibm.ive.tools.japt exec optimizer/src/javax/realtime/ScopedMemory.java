package javax.realtime;

public abstract class ScopedMemory extends MemoryArea {
	
	Object portal;
	
	public ScopedMemory(long size) {
		this(size, size, true);
	}

	public ScopedMemory(long size, Runnable logic) {
		this(size, size, logic, true);
	}

	public ScopedMemory(SizeEstimator size) {
		this(size, size);
	}

	public ScopedMemory(SizeEstimator size, Runnable logic) {
		this(size, size, logic);
	}

	ScopedMemory(long initial, long maximum, boolean create) {
		super(initial);
	}

	ScopedMemory(long initial, long maximum, Runnable logic, boolean create) {
		super(initial, logic);
	}

	ScopedMemory(SizeEstimator initial, SizeEstimator maximum) {
		super(initial);
	}

	ScopedMemory(SizeEstimator initial, SizeEstimator maximum, Runnable logic) {
		super(initial, logic);
	}

	public void enter() {
		enterPrivate();
	}

	public void enter(Runnable logic) {
		enterPrivate(logic);
	}

	void enterPrivate(Runnable logic) {
		enterPushed(logic);
	}

	private void runLogicBoundary(Runnable logic) {
		try {
			logic.run();
		} catch (Throwable e) {//TODO this loads Throwable
			throw buildBoundaryErrorInOuterArea(e);
		}
	}

	public void executeInArea(Runnable logic)  {
		executeInPushedArea(logic);
	}

	private ThrowBoundaryError buildBoundaryErrorInOuterArea(Throwable t) {
		return new ThrowBoundaryError();
	}

	public Object getPortal() {
		return portal;
	}

	public void setPortal(Object object) {
		this.portal = object;
	}

	public void joinAndEnter() throws InterruptedException {
		joinAndEnter(logic);
	}
	
	public void joinAndEnter(HighResolutionTime time) throws InterruptedException {
		joinAndEnter(logic, time);
	}

	void runEnterLogic(Runnable logic) {
		runLogicBoundary(logic);
	}

	public void joinAndEnter(Runnable logic) throws InterruptedException {
		enterPushed(logic);
	}
	
	public void joinAndEnter(Runnable logic, HighResolutionTime time) throws InterruptedException {
		joinAndEnter(logic, time);
	}
}
