package service;

import java.util.Date;

import javax.ws.rs.core.Response.ResponseBuilder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class LoginService {

	private static final String secret = "2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b";
	private static final Long EXPIRATION_TIME_MILLIS = 1000l * 60 * 60 * 24 * 7;
	
	private static final String OWNERS = "Piotr & Maciek";
	
	public static ResponseBuilder defaultHeaders(ResponseBuilder rb) {
		return rb.header("Access-Control-Allow-Origin", "*");
	}
	
	public static String createJwt(String username) {
		Algorithm algorithm = Algorithm.HMAC256(secret);
		try {
			return JWT.create()
			        .withIssuer("auth0")
			        .withClaim("username", username)
			        .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MILLIS))
			        .withClaim("owner", OWNERS)
			        .sign(algorithm);	
		}catch(JWTCreationException e) {
			System.err.println("JWT creation error.");
			return null;
		}
	}
	
	public static boolean verifyJwt(String jwtToken) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(secret);
			JWTVerifier verifier = JWT.require(algorithm)
			        .withIssuer("auth0")
			        .build();
			DecodedJWT jwt = verifier.verify(jwtToken);
			if(!jwt.getClaim("owner").asString().equals(OWNERS))
				throw new JWTVerificationException("Token invalid");
			return true;
		} catch (JWTVerificationException e){
		    return false;
		}
	}
	
}
