package db;

import java.util.Random;

import exception.GameIntegrityViolationException;
import model.Word;
import service.GameUtil;

public class WordService {

	private Database db = Database.getInstance();

	private static WordService instance;

	private WordService() {
		this.db = Database.getInstance();
	}

	/**
	 * Implementation of the singleton pattern. Creates WordService object.
	 * 
	 * @return instance of WordService
	 */
	public static WordService getInstance() {
		if (instance == null)
			instance = new WordService();
		return instance;
	}
	
	public String getRandomWord() throws GameIntegrityViolationException {
		try {
			// Min ID
			Long minId = db.em().createQuery("SELECT MIN(w.id) FROM Word w", Long.class).getSingleResult();
			// Max ID
			Long maxId = db.em().createQuery("SELECT MAX(w.id) FROM Word w", Long.class).getSingleResult();

			Word word = null;
			int failCounter = 0;
			int maxFailes = 10;
			while (word == null && failCounter < maxFailes) {
				Long randId = (long) (new Random().nextDouble() * (maxId - minId + 1)) + minId;
				word = db.em().find(Word.class, randId);
				System.out.println("[" + failCounter + "] next word...");
				failCounter++;
			}
			if (word == null) {
				throw new GameIntegrityViolationException("Was not able to get new word to guess!");
			}
			return word.getWord();
		} catch (Exception e) {
			throw new GameIntegrityViolationException("WordService error during next word generation.");
		}
	}
}
