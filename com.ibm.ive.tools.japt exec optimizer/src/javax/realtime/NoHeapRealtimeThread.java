package javax.realtime;

public class NoHeapRealtimeThread extends RealtimeThread {

	public NoHeapRealtimeThread(SchedulingParameters scheduling, MemoryArea area){
		this(scheduling, null, null, area, null, null);
	}

	public NoHeapRealtimeThread(SchedulingParameters scheduling, ReleaseParameters release, MemoryArea area) {
		this(scheduling, release, null, area, null, null);
	}

	public NoHeapRealtimeThread(SchedulingParameters scheduling, ReleaseParameters release,
			MemoryParameters memory, MemoryArea area, ProcessingGroupParameters group,
			Runnable logic) {
		super((scheduling), (release), (memory),
				(area), (group), (logic));
	}

	public void start(){
		super.start();
	}
}
