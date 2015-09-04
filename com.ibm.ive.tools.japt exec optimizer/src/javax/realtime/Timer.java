package javax.realtime;



public abstract class Timer extends AsyncEvent {

	protected Timer(HighResolutionTime time, Clock clock,
			AsyncEventHandler handler) {
	}

	public void start() {
		start(false);
	}

	public void start(boolean disabled) {
		super.fire();
	}

	public void fire() {
		super.fire();
	}

	public void addHandler(AsyncEventHandler handler) {
		this.handler = handler;
	}
	
	public void setHandler(AsyncEventHandler handler) {
		addHandler(handler);
	}
}
