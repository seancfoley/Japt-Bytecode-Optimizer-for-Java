package javax.realtime;

public final class ImmortalMemory extends MemoryArea {

	private static final ImmortalMemory mem = new ImmortalMemory();

	private ImmortalMemory() {}

	public static ImmortalMemory instance() {
		return mem;
	}

	public void executeInArea(Runnable logic)  {
		executeInNonScope(logic);
	}
}
