package db;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import exception.GameIntegrityViolationException;
import model.Password;

/**
 * This class is responsible for interacting with table containing passwords in
 * database.
 * 
 * @author Maciej Szaba³a
 *
 */
public class PasswordService implements AutoCloseable {
	private Database db;

	private static PasswordService instance;

	private PasswordService() {
		this.db = Database.getInstance();
	}

	/**
	 * Implementation of the singleton pattern. Creates PasswordService object.
	 * 
	 * @return instance of PasswordService
	 */
	public static PasswordService getInstance() {
		if (instance == null)
			instance = new PasswordService();
		return instance;
	}

	public Password getPasswordForUser(String username) {
		try {
			return db.em().createQuery("SELECT p FROM Password p WHERE p.user.username = :username", Password.class)
					.setParameter("username", username).getSingleResult();
		} catch (NoResultException e) {
			throw new GameIntegrityViolationException("Password does not exist!", e);
		} catch (NonUniqueResultException e) {
			throw new GameIntegrityViolationException("Password is not unique!", e);
		}
	}

	@Override
	public void close() throws Exception {
		db.close();
	}
}
