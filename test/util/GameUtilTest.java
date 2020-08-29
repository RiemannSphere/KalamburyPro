package util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import exception.InvalidWordException;
import service.GameUtil;

/**
 * 
 * @author Maciej Szaba³a
 */
class GameUtilTest {

	/**
	 * compareWords tests
	 */
	String w1, w2;

	@Test
	public void compareWordsFalse() {
		GameUtil gu = GameUtil.getInstance();
		w1 = "First";
		w2 = "Second";

		assertFalse(gu.compareWords(w1, w2));
	}

	@Test
	public void compareWordsTrue() {
		GameUtil gu = GameUtil.getInstance();
		w1 = "First";
		w2 = "First";

		assertTrue(gu.compareWords(w1, w2));
	}

	@Test
	public void compareWordsFirstNull() {
		assertThrows(InvalidWordException.class, () -> {

			GameUtil gu = GameUtil.getInstance();
			w1 = null;
			w2 = "Second";
			gu.compareWords(w1, w2);

		});
	}

	@Test
	public void compareWordsSecondNull() {
		assertThrows(InvalidWordException.class, () -> {

			GameUtil gu = GameUtil.getInstance();
			w1 = "First";
			w2 = null;
			gu.compareWords(w1, w2);

		});
	}

	@Test
	public void compareWordsFirstUpperCase() {
		GameUtil gu = GameUtil.getInstance();
		w1 = "FIRST";
		w2 = "first";

		assertTrue(gu.compareWords(w1, w2));
	}

	@Test
	public void compareWordsSecondUpperCase() {
		GameUtil gu = GameUtil.getInstance();
		w1 = "first";
		w2 = "FIRST";

		assertTrue(gu.compareWords(w1, w2));
	}
	
	@Test
	public void compareWordsFirstUpperCaseWithSpaces() {
		GameUtil gu = GameUtil.getInstance();
		w1 = "	FIRST  ";
		w2 = "first";

		assertTrue(gu.compareWords(w1, w2));
	}
	
	@Test
	public void compareWordsSecondUpperCaseWithSpaces() {
		GameUtil gu = GameUtil.getInstance();
		w1 = "first";
		w2 = "	FiRsT  ";

		assertTrue(gu.compareWords(w1, w2));
	}

}
