package javax.realtime;

public class ProcessingGroupParameters implements Cloneable {
	private AsyncEventHandler overrunHandler;
	private AsyncEventHandler missHandler;

	public ProcessingGroupParameters(HighResolutionTime start,
			RelativeTime period, RelativeTime cost, RelativeTime deadline,
			AsyncEventHandler overrunHandler, AsyncEventHandler missHandler) {
		setCostOverrunHandler(overrunHandler);
		setDeadlineMissHandler(missHandler);
	}

	public AsyncEventHandler getCostOverrunHandler() {
		return overrunHandler;
	}

	public AsyncEventHandler getDeadlineMissHandler() {
		return missHandler;
	}

	public void setCostOverrunHandler(AsyncEventHandler handler) {
		this.overrunHandler = handler;
	}

	public void setDeadlineMissHandler(AsyncEventHandler handler) {
		this.missHandler = handler;
	}

}
