package service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import exception.GameIntegrityViolationException;

public class LoginUtil {

	private static LoginUtil instance;

	private LoginUtil() {
	}

	/**
	 * Implementation of the singleton pattern. Creates LoginUtil object.
	 * 
	 * @return instance of LoginUtil
	 */
	public static LoginUtil getInstance() {
		if (instance == null)
			instance = new LoginUtil();
		return instance;
	}
	
	/**
	 * Verifies given token based on secret key, chosen algorithm and owner
	 * @param jwtToken token to be verified
	 * @param secret secret key
	 * @param owners owners of an app
	 * @return true if token is valid, false otherwise
	 */
	public boolean verifyJwt(String jwtToken, String secret, String owners) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(secret);
			JWTVerifier verifier = JWT.require(algorithm).withIssuer("auth0").build();
			DecodedJWT jwt = verifier.verify(jwtToken);
			if (!jwt.getClaim("owner").asString().equals(owners))
				throw new JWTVerificationException("Owner of a token is invalid.");
			return true;
		} catch (JWTVerificationException e) {
			return false;
		}
	}
	
	/**
	 * Decodes JWT, extracts username
	 * @param jwtToken token
	 * @return username
	 * @throws GameIntegrityViolationException when token is invalid
	 */
	public String extractUsernameFromToken(String jwtToken) throws GameIntegrityViolationException {
		try {
		    DecodedJWT jwt = JWT.decode(jwtToken);
		    return jwt.getClaim("username").asString();
		} catch (JWTDecodeException e){
		    e.printStackTrace();
		    throw new GameIntegrityViolationException("Invalid Token!", e);
		}
	}

}
