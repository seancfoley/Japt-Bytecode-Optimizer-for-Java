package java.lang;

public class AbstractMethodError extends IncompatibleClassChangeError {
	public AbstractMethodError() {}
	
	public AbstractMethodError(String detailMessage) {
		super (detailMessage);
	}
}
