package service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.websocket.Session;

import model.ActiveUser;
import model.Word;
import websocket.ChatWebsocket;

public class ChatService implements AutoCloseable {

	private EntityManagerFactory emf;
	private EntityManager em;

	private Session currentUserDrawing;
	private String currentWordToGuess;

	private static ChatService instance;

	private ChatService() {

	}

	public String getCurrentWordToGuess() {
		return currentWordToGuess;
	}

	public Session getCurrentUserDrawing() {
		return currentUserDrawing;
	}

	public void setCurrentUserDrawing(Session currentUserDrawing) {
		this.currentUserDrawing = currentUserDrawing;
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
			currentWordToGuess = word.getWord();
		} catch (Exception e) {
			System.err.println("Chat Service error during next word generation.");
			e.printStackTrace();
			currentWordToGuess = "[EXCEPTION]";
		}
	}

	public boolean isWordGuessed(String word) {
		return word == null ? false : word.toUpperCase().equals(currentWordToGuess.toUpperCase());
	}

	public void cleanCurrentWordToGuess() {
		currentWordToGuess = null;
	}

	public void addPointsToTheUser(String chatSessionId, int points)
			throws NoResultException, NonUniqueResultException {

		if (points <= 0)
			return;

		// Get user by chat session id
		ActiveUser user = em
				.createQuery("SELECT au FROM ActiveUser au WHERE au.chatSessionId = :chatSessionId", ActiveUser.class)
				.setParameter("chatSessionId", chatSessionId).getSingleResult();

		if (user == null)
			return;

		em.getTransaction().begin();
		// Update user with incremented points
		Integer currPoints = user.getUser().getPoints();
		user.getUser().setPoints(currPoints + points);
		em.merge(user);

		em.getTransaction().commit();
	}

	/**
	 * 
	 * @param chatSessionId
	 * @return if chatSessionId was null then choose random user for drawing and return its chatSessionId
	 */
	public String setDrawingUserInDb(String chatSessionId) {

		// Get active users
		List<ActiveUser> users = em.createQuery("SELECT au FROM ActiveUser au", ActiveUser.class).getResultList();

		if (users == null || users.isEmpty())
			return null;

		em.getTransaction().begin();

		// Find currently drawing user(s)
		List<ActiveUser> drawingUsers = users.stream().filter((user) -> user.isDrawing()).collect(Collectors.toList());

		// reset all the drawing users to not drawing
		for (ActiveUser u : drawingUsers) {
			u.setDrawing(false);
			em.merge(u);
		}

		if (chatSessionId == null) {
			// Choose random user
			int rand = new Random().nextInt(users.size());
			ActiveUser newDrawingUser = users.get(rand);
			newDrawingUser.setDrawing(true);
			em.merge(newDrawingUser);
			em.getTransaction().commit();
			return newDrawingUser.getChatSessionId();
		} else {
			// Choose given drawing user
			for (ActiveUser u : users) {
				if (u.getChatSessionId().equals(chatSessionId)) {
					// Set new user's isDrawing to true
					u.setDrawing(true);
					em.merge(u);
					break;
				}
			}
		}
		em.getTransaction().commit();
		return null;
	}

	@Override
	public void close() throws Exception {
		em.close();
		emf.close();
	}

}
