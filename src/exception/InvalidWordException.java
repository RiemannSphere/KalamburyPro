package exception;

/**
 * Thrown to indicate that invalid word has been passed.
 * 
 * @author Piotr Ko³odziejski
 */
public class InvalidWordException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidWordException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}

	public InvalidWordException(String errorMessage) {
		super(errorMessage);
	}

}
