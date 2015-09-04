package javax.realtime;

public class BoundAsyncEventHandler extends AsyncEventHandler {

	RealtimeThread server;
	
	public BoundAsyncEventHandler() {
		this(null, null, null, null, null, false, null);
	}
	
	public BoundAsyncEventHandler(SchedulingParameters scheduling,
			ReleaseParameters release, MemoryParameters memory,
			MemoryArea area, ProcessingGroupParameters group, boolean nonheap,
			Runnable logic) {
		super(scheduling, release, memory, area, group, nonheap, logic);

		
		if(nonheap) {
			server = new NoHeapRealtimeThread(scheduling, release, memory, area, group, this);
		} else {
			server = new RealtimeThread(scheduling, release, memory, area, group, this);
		}
	}
	
	protected int getAndIncrementPendingFireCount() {
		server.start();	
		return 0;
	}
}
