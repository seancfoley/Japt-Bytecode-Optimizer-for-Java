package javax.realtime;


public class RealtimeThread extends Thread implements Schedulable {

	private MemoryArea initialMemoryArea;
	private MemoryParameters memoryParms;
	private SchedulingParameters schedulingParms;
	private ReleaseParameters releaseParms;
	private ProcessingGroupParameters procGroupParms;
	private Scheduler scheduler;

	AsyncEventHandler associatedDeadlineMissHandler = null;
	
	public RealtimeThread() {
		this(null, null, null, null, null, null);
	}

	public RealtimeThread(SchedulingParameters scheduling) {
		this(scheduling, null, null, null, null, null);
	}

	public RealtimeThread(SchedulingParameters scheduling,
			ReleaseParameters release) {
		this(scheduling, release, null, null, null, null);
	}

	public RealtimeThread(SchedulingParameters scheduling,
			ReleaseParameters release, MemoryParameters memory,
			MemoryArea area, ProcessingGroupParameters group, Runnable logic) {
		super(logic, "RealtimeThread-");
		MemoryArea currentArea = getCurrentMemoryArea();
		if(area == null) {
			area = currentArea;
		}
		setSchedulingParameters(scheduling);
		setProcessingGroupParameters(group);
		setMemoryParameters(memory);
		setReleaseParameters(release);
		initialMemoryArea = area;
	}

	public static MemoryArea getCurrentMemoryArea() {
		return null;
	}

	public MemoryArea getMemoryArea() {
		return initialMemoryArea;
	}

	public MemoryParameters getMemoryParameters() {
		return memoryParms;
	}

	public ProcessingGroupParameters getProcessingGroupParameters() {
		return procGroupParms;
	}

	public ReleaseParameters getReleaseParameters() {
		return releaseParms;
	}

	public Scheduler getScheduler() {
		return scheduler;
	}

	public SchedulingParameters getSchedulingParameters() {
		return schedulingParms;
	}

	public void setMemoryParameters(MemoryParameters memory) {
		memoryParms = memory;
	}

	public void setProcessingGroupParameters(ProcessingGroupParameters group) {
		procGroupParms = group;
	}

	public void setReleaseParameters(ReleaseParameters release) {
		releaseParms = release;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
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

	public void start() {
		super.start();
		associatedDeadlineMissHandler.getAndIncrementPendingFireCount();
	}
}
