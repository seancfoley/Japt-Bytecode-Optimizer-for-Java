package java.lang;

public class UnsupportedClassVersionError extends java.lang.ClassFormatError {
	public UnsupportedClassVersionError(){
		super();
	}
	
	public UnsupportedClassVersionError(String detailMessage){
		super(detailMessage);
	}
}
