package service;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
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
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import model.ActiveUser;
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
				throw new JWTVerificationException("Owner of a token is invalid.");
			return true;
		} catch (JWTVerificationException e) {
			return false;
		}
	}
	
	public String extractUsernameFromToken(String jwtToken) {
		try {
		    DecodedJWT jwt = JWT.decode(jwtToken);
		    return jwt.getClaim("username").asString();
		} catch (JWTDecodeException e){
		    e.printStackTrace();
		}
		return "[INVALID]";
	}

	public boolean userExistsInDb(String username) {
		TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
		try {
			query.setParameter("username", username).getSingleResult();
		} catch (NoResultException e) {
			return false;
		}
		return true;
	}

	private Long getUserIdFromDb(String username) {
		User user = null;

		TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
		try {
			user = query.setParameter("username", username).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
		return user.getId();
	}

	private ActiveUser getActiveUserFromDb(String username) {
		ActiveUser user = null;

		TypedQuery<ActiveUser> query = em.createQuery("SELECT au FROM ActiveUser au WHERE au.user.username = :username",
				ActiveUser.class);
		try {
			user = query.setParameter("username", username).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
		return user;
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
	 * @throws UnsupportedEncodingException
	 */
	private byte[] pbkdf2(String password, byte[] salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
		KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		return factory.generateSecret(spec).getEncoded();
	}

	public void createNewAccount(Credentials user)
			throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
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
		password.setHash(hash);
		password.setSalt(salt);
		password.setUser(newAccount);

		// Store Password
		em.persist(password);

		em.getTransaction().commit();
	}

	public boolean auth(Credentials user)
			throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
		// Get user's id in db
		Long id = getUserIdFromDb(user.getUsername());

		// Get user's hash ad salt from db
		Password pass = getUserPasswordFromDb(id);

		// Produce hash for given password using salt from db
		byte[] hashGenerated = pbkdf2(user.getPassword(), pass.getSalt());

		// Compare hashes
		return Arrays.equals(hashGenerated, pass.getHash());
	}

	private Password getUserPasswordFromDb(Long id) {
		TypedQuery<Password> query = em.createQuery("SELECT p FROM Password p WHERE p.user = :user", Password.class);
		try {
			return query.setParameter("user", new User(id)).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	/**
	 * Save active user in database
	 * @param username
	 * @param chatSessionId
	 * @return false if something went wrong e.g. user does not exist, transaction
	 *         failed
	 */
	public boolean markUserAsActive(String username, String chatSessionId) {
		Long userId = getUserIdFromDb(username);
		if (userId == null || userId == 0l)
			return false;

		try {
			em.getTransaction().begin();

			// Create user object
			User user = new User();
			user.setId(userId);

			// Create active user entity
			ActiveUser activeUser = new ActiveUser();
			activeUser.setDrawing(false);
			activeUser.setChatSessionId(chatSessionId);
			activeUser.setUser(user);

			em.persist(activeUser);

			em.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Remove user from database
	 * @param username
	 * @return false is something went wrong
	 */
	public boolean markUserAsInactive(String username) {
		ActiveUser user = getActiveUserFromDb(username);

		if (user == null)
			return false;
		
		try {
			em.getTransaction().begin();
			em.remove(user);
			em.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void close() throws Exception {
		em.close();
		emf.close();
	}

}
