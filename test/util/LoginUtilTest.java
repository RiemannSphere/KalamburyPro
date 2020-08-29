package util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import exception.GameIntegrityViolationException;
import service.LoginUtil;

/**
 * 
 * @author Maciej Szaba³a
 *
 */
class LoginUtilTest {

	/**
	 * extractUsernameFromToken tests
	 */
	String jwtToken;

	@Test
	public void extractUsernameFromTokenCompareTrue() {
		LoginUtil lu = LoginUtil.getInstance();
		jwtToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJvd25lciI6IlBpb3RyICYgTWFjaWVrIiwiaXNzIjoiYXV0aDAiLCJleHAiOjE1OTkyMzIzMTYsInVzZXJuYW1lIjoiYWFhIn0.kVr3GloFhGIQgs8L79z1iR6OPLDMAP5OezxInWNxn8I";

		assertEquals(lu.extractUsernameFromToken(jwtToken), "aaa");
	}

	@Test
	public void extractUsernameFromTokenCompareFalse() {
		assertThrows(AssertionFailedError.class, () -> {
			LoginUtil lu = LoginUtil.getInstance();
			jwtToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJvd25lciI6IlBpb3RyICYgTWFjaWVrIiwiaXNzIjoiYXV0aDAiLCJleHAiOjE1OTkyMzIzMTYsInVzZXJuYW1lIjoiYWFhIn0.kVr3GloFhGIQgs8L79z1iR6OPLDMAP5OezxInWNxn8I";

			assertEquals(lu.extractUsernameFromToken(jwtToken), "bbb");

		});

	}

	@Test
	public void extractUsernameFromTokenNull() {
		assertThrows(NullPointerException.class, () -> {
			LoginUtil lu = LoginUtil.getInstance();
			jwtToken = null;

			lu.extractUsernameFromToken(jwtToken);

		});

	}

	@Test
	public void extractUsernameFromTokenBlank() {
		assertThrows(GameIntegrityViolationException.class, () -> {
			LoginUtil lu = LoginUtil.getInstance();
			jwtToken = "";

			lu.extractUsernameFromToken(jwtToken);

		});

	}

}
