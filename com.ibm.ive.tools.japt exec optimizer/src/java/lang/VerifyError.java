package java.lang;

public class VerifyError extends LinkageError {
	public VerifyError () {
		super();
	}
	
	public VerifyError (String detailMessage) {
		super(detailMessage);
	}
}
