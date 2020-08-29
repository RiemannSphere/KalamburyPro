package db;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import exception.GameIntegrityViolationException;
import model.Password;
import model.User;
import service.LoginUtil;

/**
 * This class is responsible for interacting with table containing all ever
 * signed up users in database.
 * 
 * @author Maciej Szaba³a
 *
 */
public class UserService implements AutoCloseable {

	private LoginUtil loginUtil = LoginUtil.getInstance();
	private Database db;

	private static UserService instance;

	private UserService() {
		this.db = Database.getInstance();
	}

	/**
	 * Implementation of the singleton pattern. Creates UserService object.
	 * 
	 * @return instance of UserService
	 */
	public static UserService getInstance() {
		if (instance == null)
			instance = new UserService();
		return instance;
	}

	/**
	 * @param username username
	 * @return user object
	 * @throws GameIntegrityViolationException user does not exist or is not unique
	 */
	public User getUserByUsername(String username) throws GameIntegrityViolationException {
		try {
			return db.em().createQuery("SELECT u from User u WHERE u.username = :username", User.class)
					.setParameter("username", username).getSingleResult();
		} catch (NoResultException e) {
			throw new GameIntegrityViolationException("User has not been signed up! Cannot mark as active.", e);
		} catch (NonUniqueResultException e) {
			throw new GameIntegrityViolationException("User is not unique!", e);
		}
	}

	/**
	 * @param username username
	 * @return true if user exists in db
	 * @throws GameIntegrityViolationException in case user is not unique in db
	 */
	public boolean userExistsInDb(String username) throws GameIntegrityViolationException {
		try {
			db.em().createQuery("SELECT u from User u WHERE u.username = :username", User.class)
					.setParameter("username", username).getSingleResult();
			return true;
		} catch (NoResultException e) {
			return false;
		} catch (NonUniqueResultException e) {
			throw new GameIntegrityViolationException("User is not unique!", e);
		}
	}

	/**
	 * Creates new account for given user. Saves username in a database. Generates
	 * salt for given user and stores it in db. Hashes the password and stores it in
	 * db.
	 * 
	 * @param username username
	 * @param password password
	 * @throws GameIntegrityViolationException if pbkdf2 was implemented incorrectly
	 */
	public void createNewUser(String username, String password) throws GameIntegrityViolationException {
		byte[] salt = loginUtil.salt();
		byte[] hash = loginUtil.pbkdf2(password, salt);

		db.em().getTransaction().begin();

		// Create User Entity
		User newAccount = new User();
		newAccount.setUsername(username);
		newAccount.setPoints(0);

		// Store User
		db.em().persist(newAccount);

		// Create Password Entity
		Password pass = new Password();
		pass.setHash(hash);
		pass.setSalt(salt);
		pass.setUser(newAccount);

		// Store Password
		db.em().persist(pass);

		db.em().getTransaction().commit();
	}

	@Override
	public void close() throws Exception {
		db.close();
	}

}
