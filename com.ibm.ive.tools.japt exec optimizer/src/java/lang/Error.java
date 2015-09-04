package java.lang;

public class Error extends Throwable {
	public Error () {
		super();
	}
	
	public Error (String detailMessage) {
		super(detailMessage);
	}
	
	public Error (String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
	
	public Error (Throwable throwable) {
		super(throwable);
	}

}
