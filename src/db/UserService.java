package db;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import exception.GameIntegrityViolationException;
import model.User;

public class UserService {

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
	 * @param username
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
	
}
