package service;

import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import model.ActiveUser;
import model.Word;
import websocket.ChatWebsocket;

public class ChatService implements AutoCloseable {

	private EntityManagerFactory emf;
	private EntityManager em;
	
	private ChatWebsocket currentUserDrawing;
	private String currentWordToGuess;

	private static ChatService instance;

	private ChatService() {
		
	}
	
	public void setCurrentUserDrawing(ChatWebsocket currentUserDrawing) {
		this.currentUserDrawing = currentUserDrawing;
	}
	
	public String getCurrentWordToGuess() {
		return currentWordToGuess;
	}
	
	public ChatWebsocket getCurrentUserDrawing() {
		return currentUserDrawing;
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

	public void nextWordToGuess() {
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
				currentWordToGuess = "[ERR] No word found.";
			}
			currentWordToGuess =  word.getWord();
		} catch (Exception e) {
			System.err.println("Chat Service error during next word generation.");
			e.printStackTrace();
			currentWordToGuess =  "[EXCEPTION]";
		}
	}
	
	public boolean isWordGuessed(String word) {
		return word == null ? false : word.toUpperCase().equals(currentWordToGuess.toUpperCase());
	}
	
	public void cleanCurrentWordToGuess() {
		currentWordToGuess = null;
	}
	
	public void addPointsToDrawingUser(int points) throws NoResultException, NonUniqueResultException {
		
		if(points <= 0)
			return;
		
		//em.getTransaction().begin();
		
		// Get drawing active user
		
		// Update user with incremented points
		
		
		//em.getTransaction().commit();
	}
	
	@Override
	public void close() throws Exception {
		em.close();
		emf.close();
	}
	
}
