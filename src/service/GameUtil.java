package service;

import java.util.List;
import java.util.stream.Collectors;

import exception.InvalidWordException;
import model.ActiveUser;
import model.Score;

/**
 * Helpful methods
 * 
 * @author Piotr Ko³odziejski
 */
public class GameUtil {

	private static GameUtil instance;

	private GameUtil() {
	}

	/**
	 * Implementation of the singleton pattern. Creates GameUtil object.
	 * 
	 * @return instance of GameUtil
	 */
	public static GameUtil getInstance() {
		if (instance == null)
			instance = new GameUtil();
		return instance;
	}

	/**
	 * @param word word to be validated
	 * @return true if word is null, empty or blank, false otherwise
	 */
	public boolean isWordInvalid(String word) {
		return word == null || word.isEmpty() || word.trim().isEmpty();
	}

	/**
	 * Comparison is not case sensitive and does not care about whitespaces around
	 * 
	 * @param w1 first word
	 * @param w2 second word
	 * @return true if words are equal
	 * @throws InvalidWordException one of words is null or blank
	 */
	public boolean compareWords(String w1, String w2) throws InvalidWordException {
		if (isWordInvalid(w1))
			throw new InvalidWordException("First word is null empty or blank.");

		if (isWordInvalid(w2))
			throw new InvalidWordException("Second word is null empty or blank.");

		return w1.trim().toUpperCase().equals(w2.trim().toUpperCase());
	}

	/**
	 * Produces scoreboard for given users
	 * 
	 * @param users list of active users
	 * @return list of scores for given users
	 */
	public List<Score> produceScoreboard(List<ActiveUser> users) {
		return users.stream()
				.map((au) -> new Score(au.getUser().getUsername(), au.isDrawing(), au.getUser().getPoints()))
				.collect(Collectors.toList());
	}
}
