package java.lang;

public class RuntimeException extends Exception {
	public RuntimeException () {
		super();
	}
	
	public RuntimeException (String detailMessage) {
		super(detailMessage);
	}
	
	public RuntimeException (String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
	
	public RuntimeException (Throwable throwable) {
		super(throwable);
	}
}
