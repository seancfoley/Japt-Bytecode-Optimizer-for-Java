package javax.realtime;


public class AsyncEvent {

	AsyncEventHandler handler;

	/**
	 * Create a new AsyncEvent object.
	 */
	public AsyncEvent() {
	}

	
	public void addHandler(AsyncEventHandler handler) {
		this.handler = handler;
	}

	
	public void setHandler(AsyncEventHandler handler) {
		addHandler(handler);
	}

	public void fire() {
		handler.getAndIncrementPendingFireCount();
	}
}
