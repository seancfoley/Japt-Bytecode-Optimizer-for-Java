package java.lang;


public class AnnotationFormatError extends Error {
	public AnnotationFormatError(String detailMessage) {
		super(detailMessage);
	}
	
	public AnnotationFormatError(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
	
	public AnnotationFormatError(Throwable throwable) {
		super(throwable);
	}
}
