package exception;

/**
 * Thrown in case of internal app error or inconsistency on database level.
 * 
 * @author Piotr Kołodziejski
 */
public class GameIntegrityViolationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public GameIntegrityViolationException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}

	public GameIntegrityViolationException(String errorMessage) {
		super(errorMessage);
	}

}
