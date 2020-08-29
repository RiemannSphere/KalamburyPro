package exception;

public class GameIntegrityViolationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public GameIntegrityViolationException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}
	
	public GameIntegrityViolationException(String errorMessage) {
		super(errorMessage);
	}
	
}
