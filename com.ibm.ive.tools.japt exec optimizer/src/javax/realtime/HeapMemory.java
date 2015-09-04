package javax.realtime;

public final class HeapMemory extends MemoryArea {

	private static HeapMemory instance = new HeapMemory();

	private HeapMemory() {}

	public static HeapMemory instance() {
		return instance;
	}

	public void executeInArea(Runnable logic)  {
		logic.run();
	}

}
