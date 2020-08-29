package db;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import exception.GameIntegrityViolationException;

/**
 * This class is responsible for interacting with table containing app
 * constances in database.
 * 
 * @author Piotr Ko³odziejski
 */
public class AppDictionaryService implements AutoCloseable {

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
	public long getExpirationTime() throws GameIntegrityViolationException {
		try {
			return Long.parseLong(getValueForKey("EXP_TIME_MILLIS"));
		} catch (NumberFormatException e) {
			throw new GameIntegrityViolationException("Expiration time is not of type long. Error in db.", e);
		}
	}

	/**
	 * @return number of retries
	 * @throws GameIntegrityViolationException key does not exist or is not unique,
	 *                                         also when value is not an integer
	 */
	public int getNumberOfRetries() throws GameIntegrityViolationException {
		try {
			return Integer.parseInt(getValueForKey("NUM_OF_RETRY"));
		} catch (NumberFormatException e) {
			throw new GameIntegrityViolationException("Number of retries is not an integer. Error in db.", e);
		}
	}

	/**
	 * @return retry frequency in milliseconds
	 * @throws GameIntegrityViolationException key does not exist or is not unique
	 */
	public int getRetryFrequency() throws GameIntegrityViolationException {
		try {
			return Integer.parseInt(getValueForKey("RETRY_FREQ_IN_MILLIS"));
		} catch (NumberFormatException e) {
			throw new GameIntegrityViolationException("Retry frequency is not an integer. Error in db.", e);
		}
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

	@Override
	public void close() throws Exception {
		db.close();
	}

}
