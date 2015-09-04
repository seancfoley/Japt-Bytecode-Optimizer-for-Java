package javax.realtime;

public abstract class ReleaseParameters implements Cloneable {
	AsyncEventHandler overrunHandler;
	AsyncEventHandler missHandler;
	
	protected ReleaseParameters() {
		this(null, null, null, null);
	}

	protected ReleaseParameters(RelativeTime cost, RelativeTime deadline,
			AsyncEventHandler overrunHandler, AsyncEventHandler missHandler) {
		setCostOverrunHandler(overrunHandler);
		setDeadlineMissHandler(missHandler);
	}

	public void setCostOverrunHandler(AsyncEventHandler handler) {
		overrunHandler = handler;
	}

	public void setDeadlineMissHandler(AsyncEventHandler handler) {
		missHandler = handler;
	}
	
	public AsyncEventHandler getCostOverrunHandler() {
		return overrunHandler;
	}

	public AsyncEventHandler getDeadlineMissHandler() {
		return missHandler;
	}
}
