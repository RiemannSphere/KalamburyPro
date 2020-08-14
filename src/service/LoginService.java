package service;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Date;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.management.Query;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import model.Credentials;
import model.Password;
import model.User;

public class LoginService implements AutoCloseable {

	private EntityManagerFactory emf;
	private EntityManager em;
	
	private final String secret = "2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b";
	private final Long EXPIRATION_TIME_MILLIS = 1000l * 60 * 60 * 24 * 7;

	private final String OWNERS = "Piotr & Maciek";

	public static LoginService instance;
	
	private LoginService() {
		
	}
	
	public static LoginService getInstance() {
		if (instance == null) {
			instance = new LoginService();
			instance.initPersistence();
		}
		return instance;
	}
	
	private void initPersistence() {
		try {
			emf = Persistence.createEntityManagerFactory("postgres");
			em = emf.createEntityManager();
		} catch (Exception e) {
			System.err.println("Login Service init Entity Manager failed.");
			e.printStackTrace();
		}
	}
	
	public ResponseBuilder defaultHeaders(ResponseBuilder rb) {
		return rb.header("Access-Control-Allow-Origin", "*");
	}

	public String createJwt(String username) {
		Algorithm algorithm = Algorithm.HMAC256(secret);
		try {
			return JWT.create().withIssuer("auth0").withClaim("username", username)
					.withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MILLIS))
					.withClaim("owner", OWNERS).sign(algorithm);
		} catch (JWTCreationException e) {
			System.err.println("JWT creation error.");
			return null;
		}
	}

	public boolean verifyJwt(String jwtToken) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(secret);
			JWTVerifier verifier = JWT.require(algorithm).withIssuer("auth0").build();
			DecodedJWT jwt = verifier.verify(jwtToken);
			if (!jwt.getClaim("owner").asString().equals(OWNERS))
				throw new JWTVerificationException("Token invalid");
			return true;
		} catch (JWTVerificationException e) {
			return false;
		}
	}

	public boolean userExistsInDb(String username) {
		TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
		try {
			query.setParameter("username", username).getSingleResult();
		} catch(NoResultException e) {
			return false;
		}
		return true;
	}

	/**
	 * @return secure random 16-byte salt
	 */
	private byte[] salt() {
		SecureRandom rand = new SecureRandom();
		byte[] salt = new byte[16];
		rand.nextBytes(salt);
		return salt;
	}

	/**
	 * @param password
	 * @param salt
	 * @return secure hash for salted password
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	private byte[] pbkdf2(String password, byte[] salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		return factory.generateSecret(spec).getEncoded();
	}

	public void createNewAccount(Credentials user) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
		byte[] salt = salt();
		byte[] hash = pbkdf2(user.getPassword(), salt); 
		
		em.getTransaction().begin();
		
		// Create User Entity
		User newAccount = new User();
		newAccount.setUsername(user.getUsername());
		newAccount.setPoints(0);
		
		// Store User
		em.persist(newAccount);
		
		// Create Password Entity
		Password password = new Password();
		password.setHash(new String(hash, "UTF-8"));
		password.setSalt(new String(salt, "UTF-8"));
		password.setUser(newAccount);
		
		// Store Password
		em.persist(password);

		em.getTransaction().commit();
	}
	
	@Override
	public void close() throws Exception {
		em.close();
		emf.close();
	}

}
