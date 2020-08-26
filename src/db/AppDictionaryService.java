package db;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import exception.GameIntegrityViolationException;

public class AppDictionaryService {

	private Database db = Database.getInstance();

	private static AppDictionaryService instance;

	private AppDictionaryService() {
	}

	/**
	 * Implementation of the singleton pattern. Creates AppDictionaryService object.
	 * 
	 * @return instance of AppDictionaryService
	 */
	public static AppDictionaryService getInstance() {
		if (instance == null)
			instance = new AppDictionaryService();
		return instance;
	}

	/**
	 * @return secret to sign JWT
	 * @throws GameIntegrityViolationException key does not exist or is not unique
	 */
	public String getSecret() throws GameIntegrityViolationException {
		return getValueForKey("SECRET");
	}

	/**
	 * @return owners of an app
	 * @throws GameIntegrityViolationException key does not exist or is not unique
	 */
	public String getOwners() throws GameIntegrityViolationException {
		return getValueForKey("OWNERS");
	}

	/**
	 * @return expiration time of a JWT in milliseconds
	 * @throws GameIntegrityViolationException key does not exist or is not unique
	 */
	public String getExpirationTime() throws GameIntegrityViolationException {
		return getValueForKey("EXP_TIME_MILLIS");
	}

	/**
	 * 
	 * @param key key in dictionary table
	 * @return value for given key in dictionary table
	 * @throws GameIntegrityViolationException key does not exist or is not unique
	 */
	private String getValueForKey(String key) throws GameIntegrityViolationException {
		try {
			return db.em().createQuery("SELECT dic.value FROM AppDictionary dic WHERE dic.key = :key", String.class)
					.setParameter("key", key).getSingleResult();
		} catch (NoResultException e) {
			throw new GameIntegrityViolationException("Key does not exist in the dictionary!", e);
		} catch (NonUniqueResultException e) {
			throw new GameIntegrityViolationException("Key is not unique in the dictionary!", e);
		}
	}

}
