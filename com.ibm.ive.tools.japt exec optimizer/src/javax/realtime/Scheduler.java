package javax.realtime;

public abstract class Scheduler {
	protected Scheduler() {
	}

	private static Scheduler defaultScheduler;

	public static Scheduler getDefaultScheduler() {
		return defaultScheduler;
	}

	public static void setDefaultScheduler(Scheduler scheduler) {
		defaultScheduler = scheduler;
	}
	
	public abstract void fireSchedulable(Schedulable schedulable);
}
