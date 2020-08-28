package db;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import exception.GameIntegrityViolationException;
import model.Password;

public class PasswordService {
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
		}  catch (NonUniqueResultException  e) {
			throw new GameIntegrityViolationException("Password is not unique!", e);
		}
	}
	
}
