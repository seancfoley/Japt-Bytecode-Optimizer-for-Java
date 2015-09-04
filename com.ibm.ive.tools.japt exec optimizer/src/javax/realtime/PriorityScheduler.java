package javax.realtime;

public class PriorityScheduler extends Scheduler {

	private static PriorityScheduler baseScheduler = new PriorityScheduler();

	protected PriorityScheduler() {
	}

	public static PriorityScheduler instance() {
		return baseScheduler;
	}

	public void fireSchedulable(Schedulable schedulable) {
		((AsyncEventHandler) schedulable).getAndIncrementPendingFireCount();
		((RealtimeThread) schedulable).start();
	}

	static PriorityScheduler getDefaultPriorityScheduler() {
		return instance();
	}
}

