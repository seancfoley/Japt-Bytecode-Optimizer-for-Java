package java.lang;

public class NoSuchMethodError extends IncompatibleClassChangeError {
	public NoSuchMethodError () {
		super();
	}
	
	public NoSuchMethodError (String detailMessage) {
		super(detailMessage);
	}
}
