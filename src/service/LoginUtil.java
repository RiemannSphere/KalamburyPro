package service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import exception.GameIntegrityViolationException;

/**
 * Methods used during authentication
 * 
 * @author Piotr Ko³odziejski
 */
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
	 * Creates and signs a JWT for given user. Appends additional claims like
	 * expiration date or owner.
	 * 
	 * @param username name for which create the token
	 * @param secret   secret for signing the token
	 * @param expTime  in milliseconds
	 * @param owners   of an app
	 * @return signed token
	 */
	public String createJwt(String username, String secret, long expTime, String owners) {
		Algorithm algorithm = Algorithm.HMAC256(secret);
		try {
			return JWT.create().withIssuer("auth0").withClaim("username", username)
					.withExpiresAt(new Date(System.currentTimeMillis() + expTime)).withClaim("owner", owners)
					.sign(algorithm);
		} catch (JWTCreationException e) {
			System.err.println("JWT creation error.");
			return null;
		}
	}

	/**
	 * Verifies given token based on secret key, chosen algorithm and owner
	 * 
	 * @param jwtToken token to be verified
	 * @param secret   secret key
	 * @param owners   owners of an app
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
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Decodes JWT, extracts username
	 * 
	 * @param jwtToken token
	 * @return username
	 * @throws GameIntegrityViolationException when token is invalid
	 */
	public String extractUsernameFromToken(String jwtToken) throws GameIntegrityViolationException {
		try {
			DecodedJWT jwt = JWT.decode(jwtToken);
			return jwt.getClaim("username").asString();
		} catch (JWTDecodeException e) {
			throw new GameIntegrityViolationException("Invalid Token!", e);
		}
	}

	/**
	 * Generates secure random array of bytes.
	 * 
	 * @return 16-byte salt
	 */
	public byte[] salt() {
		SecureRandom rand = new SecureRandom();
		byte[] salt = new byte[16];
		rand.nextBytes(salt);
		return salt;
	}

	/**
	 * Generates cryptographic salted hash for given password using PBKDF2
	 * algorithm.
	 * 
	 * @param password password
	 * @param salt     salt
	 * @return secure hash for salted password
	 * @throws GameIntegrityViolationException when pbkdf2 was implemented
	 *                                         incorrectly
	 */
	public byte[] pbkdf2(String password, byte[] salt) throws GameIntegrityViolationException {
		try {
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			return factory.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new GameIntegrityViolationException("Authentication internal error!", e);
		}
	}

	/**
	 * Produces hash for given password (using salt from db) and compares it to the
	 * has from db.
	 * 
	 * @param username user to be authenticated
	 * @param hash     hashed password taken from db
	 * @param salt     salt associated to the user, taken from db
	 * @param password password to be checked
	 * @return true if user has been authenticated
	 */
	public boolean isUserAuthenticated(String username, byte[] hash, byte[] salt, String password) {
		// Produce hash for given password using salt from db
		byte[] hashGenerated = pbkdf2(password, salt);
		// Compare hashes
		return Arrays.equals(hashGenerated, hash);
	}

	/**
	 * Appends default headers to the server response.
	 * 
	 * @param rb response to which append the headers
	 * @return response with appended headers
	 */
	public ResponseBuilder defaultHeaders(ResponseBuilder rb) {
		return rb.header("Access-Control-Allow-Origin", "*");
	}

}
