package exception;

public class InvalidWordException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidWordException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}
	
	public InvalidWordException(String errorMessage) {
		super(errorMessage);
	}
	
}
