package java.lang;

public class IllegalAccessError extends IncompatibleClassChangeError {
	public IllegalAccessError() {}
	
	public IllegalAccessError(String detailMessage) {
		super(detailMessage);
	}
}
