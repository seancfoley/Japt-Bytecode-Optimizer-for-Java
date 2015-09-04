package javax.realtime;

public class AsyncEventHandler implements Schedulable {
	private MemoryParameters memoryParms;
	private SchedulingParameters schedulingParms;
	private ReleaseParameters releaseParms;
	private ProcessingGroupParameters procGroupParms;
	private MemoryArea initialMemoryArea;
	private final Runnable logic;
	boolean nonheap;

	
	AsyncEventHandler associatedDeadlineMissHandler = null;
	
	public AsyncEventHandler() {
		this(null, null, null, null, null, false, null);
	}

	
	public AsyncEventHandler(boolean nonheap) {
		this(null, null, null, null, null, nonheap, null);
	}

	public AsyncEventHandler(boolean nonheap, Runnable logic) {
		this(null, null, null, null, null, nonheap, logic);
	}

	public AsyncEventHandler(Runnable logic) {
		this(null, null, null, null, null, false, logic);
	}

	public AsyncEventHandler(SchedulingParameters scheduling,
			ReleaseParameters release, MemoryParameters memory,
			MemoryArea area, ProcessingGroupParameters group, boolean nonheap) {
		this(scheduling, release, memory, area, group, nonheap, null);
	}

	public AsyncEventHandler(SchedulingParameters scheduling,
			ReleaseParameters release, MemoryParameters memory,
			MemoryArea area, ProcessingGroupParameters group,
			java.lang.Runnable logic) {
		this(scheduling, release, memory, area, group, false, logic);
	}

	public AsyncEventHandler(SchedulingParameters scheduling,
			ReleaseParameters release, MemoryParameters memory,
			MemoryArea area, ProcessingGroupParameters group, boolean nonheap,
			Runnable logic) {
		setSchedulingParameters(scheduling);
		setReleaseParameters(release);
		this.nonheap = nonheap;
		setMemoryParameters(memory);
		setProcessingGroupParameters(group);
		initialMemoryArea = area;
		this.logic = logic;
	}

	public void handleAsyncEvent() {
		if (logic != null) {
			logic.run();
		}
	}

	public final void run() {
		releaseParms.missHandler.getAndIncrementPendingFireCount();
		releaseParms.overrunHandler.getAndIncrementPendingFireCount();
		try {
			handleAsyncEvent();
		}
		catch (Throwable e) {
		}
	}

	public MemoryArea getMemoryArea() {
		return initialMemoryArea;
	}

	
	public MemoryParameters getMemoryParameters() {
		return memoryParms;
	}

	public ReleaseParameters getReleaseParameters() {
		return releaseParms;
	}

	public SchedulingParameters getSchedulingParameters() {
		return schedulingParms;
	}

	public ProcessingGroupParameters getProcessingGroupParameters() {
		return procGroupParms;
	}

	
	public void setMemoryParameters(MemoryParameters memory) {
		memoryParms = memory;
	}

	protected int getAndIncrementPendingFireCount() {
		RealtimeThread server;
		if(nonheap) {
			if(initialMemoryArea == null) {
				server = new NoHeapRealtimeThread(schedulingParms, null);
			} else {
				server = new NoHeapRealtimeThread(schedulingParms, releaseParms, memoryParms, initialMemoryArea, procGroupParms, logic);
			}
		} else {
			if(initialMemoryArea == null) {
				server = new RealtimeThread(schedulingParms, releaseParms);
			} else {
				server = new RealtimeThread(schedulingParms, releaseParms, memoryParms, initialMemoryArea, procGroupParms, logic);
			}
		}
		server.start();	
		return 0;
	}

	public void setReleaseParameters(ReleaseParameters release) {
		releaseParms = release;
	}

	public void setScheduler(Scheduler scheduler,
			SchedulingParameters scheduling, ReleaseParameters release,
			MemoryParameters memory, ProcessingGroupParameters group) {
		setSchedulingParameters(scheduling);
		setReleaseParameters(release);
		setMemoryParameters(memory);
		setProcessingGroupParameters(group);
	}

	public void setSchedulingParameters(SchedulingParameters scheduling) {
		schedulingParms = scheduling;
	}

	
	public void setProcessingGroupParameters(ProcessingGroupParameters group) {
		procGroupParms = group;
	}
	

}
