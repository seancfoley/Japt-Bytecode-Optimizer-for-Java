package javax.realtime;


public abstract class MemoryArea {

	Runnable logic;

	MemoryArea() {
		this(0);
	}

	protected MemoryArea(long size) {
		this(size, null);
	}

	protected MemoryArea(SizeEstimator size) {
		this(size, null);
	}

	protected MemoryArea(long size, Runnable logic) {
		this.logic = logic;
	}

	protected MemoryArea(SizeEstimator size, Runnable logic) {
		this.logic = logic;
	}

	public void enter() {
		enterPrivate();
	}

	void enterPrivate() {
		enterPrivate(logic);
	}
	
	public void enter(Runnable logic) {
		enterPrivate(logic);
	}

	void enterPrivate(Runnable logic) {
		enterPushed(logic);
	}

	void enterPushed(Runnable logic) {
		runEnterLogic(logic);
	}

	/**
	 * called to run the logic that was passed in when the memory area was created
	 */
	void runEnterLogic(Runnable logic) {
		logic.run();
	}

	public static MemoryArea getMemoryArea(Object object) {
		return null;
	}

//	public Object newArray(Class type, int number) {
//		return Array.newInstance(type, number);
//	}
	
	public native Object newArray(Class type, int number);
	
	//public native Object newInstance(Class type);
	
	public Object newInstance(Class type)
		throws IllegalAccessException, InstantiationException {
		return type.newInstance();
	}

	public Object newInstance(java.lang.reflect.Constructor constructor, Object[] args) throws IllegalAccessException,
		InstantiationException, java.lang.reflect.InvocationTargetException {
		return constructor.newInstance(args);
	}
	
//	public native Object newInstance(java.lang.reflect.Constructor constructor, Object[] args) throws IllegalAccessException,
//		InstantiationException, java.lang.reflect.InvocationTargetException;
	
	public void executeInArea(Runnable logic)  { /* this method is overridden in ScopedMemory */
		executeInNonScope(logic);
	}

	void executeInNonScope(Runnable logic) {
		executeInPushedArea(logic);
	}

	void executeInPushedArea(Runnable logic) {
		logic.run();
	}
}
