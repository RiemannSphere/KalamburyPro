package db;

import java.util.List;
import java.util.Random;

import javax.persistence.EntityExistsException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import exception.GameIntegrityViolationException;
import exception.InvalidWordException;
import model.ActiveUser;
import model.Score;
import model.User;
import service.GameUtil;

/**
 * This class is responsible for interacting with table containing active users
 * in database. It it a singleton and is meant to be injected as a field i.e.
 * private ActiveUserService as = ActiveUserService.getInstance();
 * 
 * @author Piotr Ko³odziejski
 */
public class ActiveUserService implements AutoCloseable {

	private Database db = Database.getInstance();
	private UserService userService = UserService.getInstance();
	private GameUtil gameUtil = GameUtil.getInstance();

	private static ActiveUserService instance;

	private ActiveUserService() {
		this.db = Database.getInstance();
	}

	/**
	 * Implementation of the singleton pattern. Creates ActiveUserService object.
	 * 
	 * @return instance of ActiveUserService
	 */
	public static ActiveUserService getInstance() {
		if (instance == null)
			instance = new ActiveUserService();
		return instance;
	}

	/**
	 * Checks if user with given session id exists in active users table in db.
	 * 
	 * @param sessionId id of a websocket session
	 * @return true if user exists in ActiveUser table, false otherwise
	 * @throws GameIntegrityViolationException in case of inconsistency in database
	 */
	public boolean isUserActive(String sessionId) throws GameIntegrityViolationException {
		try {
			String drawingSessionId = db.em()
					.createQuery("SELECT au FROM ActiveUser au WHERE au.chatSessionId = :sessionId", ActiveUser.class)
					.setParameter("sessionId", sessionId).getSingleResult().getChatSessionId();
			if (drawingSessionId != null && !drawingSessionId.isEmpty())
				return true;
			else
				throw new GameIntegrityViolationException("Inconsistency in database! Null or empty session id.");
		} catch (NoResultException e) {
			return false;
		} catch (NonUniqueResultException e) {
			throw new GameIntegrityViolationException("Active user session id is not unique!", e);
		}
	}

	/**
	 * Saves active user in database.
	 * 
	 * @param username      User to be set as active
	 * @param chatSessionId User's session id
	 * @throws GameIntegrityViolationException user does not exist or is not unique
	 */
	public void addActiveUser(String username, String chatSessionId) throws GameIntegrityViolationException {
		User user = userService.getUserByUsername(username);

		try {
			db.em().getTransaction().begin();

			// Create active user entity
			ActiveUser activeUser = new ActiveUser();
			activeUser.setDrawing(false);
			activeUser.setChatSessionId(chatSessionId);
			activeUser.setUser(user);
			activeUser.setWord(null);

			db.em().persist(activeUser);

			db.em().getTransaction().commit();
		} catch (EntityExistsException e) {
			throw new GameIntegrityViolationException("User is already active!", e);
		}

	}

	/**
	 * Removes user with given session id from active users table in db.
	 * 
	 * @param sessionId User's session id
	 */
	public void removeActiveUser(String sessionId) {
		ActiveUser user = null;
		try {
			user = getActiveUserBySessionId(sessionId);
		} catch (GameIntegrityViolationException e) {
			System.out.println("ActiveUserService: removeActiveUser: user already removed");
			return;
		}
		System.out.println("ActiveUserService: removing user " + user.getUser().getUsername());
		db.em().getTransaction().begin();
		db.em().remove(user);
		db.em().getTransaction().commit();
	}

	/**
	 * Produces scoreboard for given list or users.
	 * 
	 * @return list of active users and their points
	 */
	public List<Score> produceScoreboardForActiveUsers() {
		return gameUtil.produceScoreboard(getActiveUsers());
	}

	/**
	 * Checks if there is a drawing user among active users in db.
	 * 
	 * @return true is drawing user exists, false otherwise
	 * @throws GameIntegrityViolationException if there is more than one drawing
	 *                                         user
	 */
	public boolean doesDrawingUserExist() throws GameIntegrityViolationException {
		try {
			db.em().createQuery("SELECT au FROM ActiveUser au WHERE au.isDrawing = true", ActiveUser.class)
					.getSingleResult();
			return true;
		} catch (NoResultException e) {
			return false;
		} catch (NonUniqueResultException e) {
			throw new GameIntegrityViolationException("More than one drawing user!", e);
		}
	}

	/**
	 * Compares given word to the current word to guess stored in a db.
	 * 
	 * @param word to be compared with current word to guess
	 * @return true if words are equal after trim and to upper case, false otherwise
	 * @throws GameIntegrityViolationException when there is zero or more than one
	 *                                         drawing user
	 */
	public boolean hasWordBeenGuessed(String word) throws GameIntegrityViolationException {

		if (gameUtil.isWordInvalid(word))
			return false;

		String currentWord = getActiveDrawingUser().getWord();

		try {
			return gameUtil.compareWords(word, currentWord);
		} catch (InvalidWordException e) {
			System.err.println("ActiveUserService: hasWordBeenGuessed: word to guess is probably null or blank!");
			return false;
		}
	}

	/**
	 * Selects currently drawing user from db.
	 * 
	 * @return active drawing user
	 * @throws GameIntegrityViolationException when there is zero or more than one
	 *                                         drawing user
	 */
	public ActiveUser getActiveDrawingUser() throws GameIntegrityViolationException {
		try {
			return db.em().createQuery("SELECT au FROM ActiveUser au WHERE au.isDrawing = true", ActiveUser.class)
					.getSingleResult();
		} catch (NoResultException e) {
			throw new GameIntegrityViolationException("There is no drawing user!", e);
		} catch (NonUniqueResultException e) {
			throw new GameIntegrityViolationException("More than one drawing user!", e);
		}
	}

	/**
	 * Selects all the active users from db.
	 * 
	 * @return all active users
	 */
	public List<ActiveUser> getActiveUsers() {
		return db.em().createQuery("SELECT au FROM ActiveUser au", ActiveUser.class).getResultList();
	}

	/**
	 * Adds given number of points to the active user with given session id.
	 * 
	 * @param chatSessionId user session id to which add points
	 * @param points        number of points to be added
	 * @throws GameIntegrityViolationException when user is inactive or there is
	 *                                         more than one user with given session
	 *                                         id
	 */
	public void addPointsToTheUser(String chatSessionId, int points) throws GameIntegrityViolationException {

		if (points <= 0) {
			System.err.println("ActiveUserService: addPointsToTheUser: cnnot add zero or less points!");
			return;
		}

		ActiveUser user = null;
		try {
			// Get user by chat session id
			user = db.em().createQuery("SELECT au FROM ActiveUser au WHERE au.chatSessionId = :chatSessionId",
					ActiveUser.class).setParameter("chatSessionId", chatSessionId).getSingleResult();
		} catch (NoResultException e) {
			throw new GameIntegrityViolationException("Cannot add point to inactive user!", e);
		} catch (NonUniqueResultException e) {
			throw new GameIntegrityViolationException("There is more than one user with the same session id!", e);
		}

		if (user == null)
			return;

		db.em().getTransaction().begin();
		// Update user with incremented points
		Integer currPoints = user.getUser().getPoints();
		user.getUser().setPoints(currPoints + points);
		db.em().merge(user);

		db.em().getTransaction().commit();
	}

	/**
	 * Selects random active user from db.
	 * 
	 * @return random active user
	 */
	public ActiveUser getRandomActiveUser() {
		List<ActiveUser> users = getActiveUsers();
		int rand = new Random().nextInt(users.size());
		return users.get(rand);
	}

	/**
	 * Reset drawing state for all to false. Also reset previous words to guess.
	 */
	public void unsetDrawingStateForAllAndUnsetWords() {
		List<ActiveUser> drawingUsers = db.em()
				.createQuery("SELECT au FROM ActiveUser au WHERE au.isDrawing = true", ActiveUser.class)
				.getResultList();

		db.em().getTransaction().begin();

		for (ActiveUser u : drawingUsers) {
			u.setDrawing(false);
			u.setWord(null);
			db.em().merge(u);
		}

		db.em().getTransaction().commit();
	}

	/**
	 * At first it resets state of all active users to not drawing. It also resets
	 * all the words to null. After that this method sets new drawing user and new
	 * word to guess.
	 * 
	 * @param user to be set as drawing
	 * @param word new word to guess
	 * @throws GameIntegrityViolationException when either user is null or word is
	 *                                         invalid
	 */
	public void setDrawingUserAndNewWord(ActiveUser user, String word) throws GameIntegrityViolationException {
		if (user == null)
			throw new GameIntegrityViolationException("Cannot set null user as drawing!");

		if (gameUtil.isWordInvalid(word))
			throw new GameIntegrityViolationException("Cannot set invalid word!");

		// Before setting new drawing user and new word
		// unset all users to not drawing state and unset previous words to guess.
		unsetDrawingStateForAllAndUnsetWords();

		db.em().getTransaction().begin();

		ActiveUser foundUser = db.em().find(ActiveUser.class, user.getIdau());
		foundUser.setDrawing(true);
		foundUser.setWord(word);

		db.em().merge(foundUser);

		db.em().getTransaction().commit();
	}

	/**
	 * Selects active user by his session id from db.
	 * 
	 * @param sessionId session id
	 * @return active user
	 * @throws GameIntegrityViolationException in case session id is null, empty or
	 *                                         blank
	 */
	public ActiveUser getActiveUserBySessionId(String sessionId) throws GameIntegrityViolationException {
		if (sessionId == null || sessionId.isEmpty() || sessionId.trim().isEmpty())
			throw new GameIntegrityViolationException("Cannot get active user for null, empty or blank session id!");

		try {
			return db.em()
					.createQuery("SELECT au FROM ActiveUser au WHERE au.chatSessionId = :sessionId", ActiveUser.class)
					.setParameter("sessionId", sessionId).getSingleResult();
		} catch (NoResultException e) {
			throw new GameIntegrityViolationException("Active user with given session id does not exist!", e);
		} catch (NonUniqueResultException e) {
			throw new GameIntegrityViolationException("Active user session id is not unique!", e);
		}
	}

	/**
	 * On close of the object makes sure to close the db connection.
	 */
	@Override
	public void close() throws Exception {
		db.close();
	}
}
