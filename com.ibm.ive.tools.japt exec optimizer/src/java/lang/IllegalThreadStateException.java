package java.lang;

public class IllegalThreadStateException extends IllegalArgumentException {
	public IllegalThreadStateException () {
		super();
	}
	
	public IllegalThreadStateException (String detailMessage) {
		super(detailMessage);
	}
}
