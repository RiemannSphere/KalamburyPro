package service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import model.Word;

public class ChatService {

	private EntityManagerFactory emf;
	private EntityManager em;

	private static ChatService instance;

	private ChatService() {

	}

	public static ChatService getInstance() {
		if (instance == null) {
			instance = new ChatService();
			instance.initPersistence();
		}
		return instance;
	}

	private void initPersistence() {
		try {
			emf = Persistence.createEntityManagerFactory("postgres");
			em = emf.createEntityManager();
		} catch (Exception e) {
			System.err.println("Chat Service init Entity Manager failed.");
			e.printStackTrace();
		}
	}

	public String nextWordToGuess() {
		try {
			// Min ID
			Long minId = em.createQuery("SELECT MIN(w.id) FROM Word w", Long.class).getSingleResult();
			// Max ID
			Long maxId = em.createQuery("SELECT MAX(w.id) FROM Word w", Long.class).getSingleResult();
			System.out.println("Min/Max id: (" + minId + ", " + maxId + ")");

			Word word = null;
			int failCounter = 0;
			int maxFailes = 10;
			while (word == null && failCounter < maxFailes) {
				Long randId = (long) (new Random().nextDouble() * (maxId - minId + 1)) + minId;
				word = em.find(Word.class, randId);
				System.out.println("[" + failCounter + "] next word...");
				failCounter++;
			}
			if (word == null) {
				return "[ERR] No word found.";
			}
			return word.getWord();
		} catch (Exception e) {
			System.err.println("Chat Service error during next word generation.");
			e.printStackTrace();
			return "[EXCEPTION]";
		}
	}
}
