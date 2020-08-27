package db;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.persistence.EntityExistsException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import exception.GameIntegrityViolationException;
import exception.InvalidWordException;
import model.ActiveUser;
import model.Score;
import model.User;
import service.GameUtil;

public class ActiveUserService implements AutoCloseable {

	private Database db = Database.getInstance();
	private UserService userService = UserService.getInstance();
	private GameUtil gameUtil = GameUtil.getInstance();
	private ActiveUserService activeUserService = ActiveUserService.getInstance();

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
	 * @return false if something went wrong e.g. user does not exist, transaction
	 *         failed
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
	 * 
	 * @return list of active users and their points
	 */
	public List<Score> produceScoreboardForActiveUsers() {
		return gameUtil.produceScoreboard(getActivUsers());
	}

	/**
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
	 * @return all active users
	 */
	public List<ActiveUser> getActivUsers() {
		return db.em().createQuery("SELECT au FROM ActiveUser au", ActiveUser.class).getResultList();
	}

	/**
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
	 * @return random active user
	 */
	public ActiveUser getRandomActiveUser() {
		List<ActiveUser> users = getActivUsers();
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
		
		for(ActiveUser u : drawingUsers) {
			u.setDrawing(false);
			u.setWord(null);
			db.em().merge(u);
		}
		
		db.em().getTransaction().commit();
	}
	
	/**
	 * @param user to be set as drawing
	 * @param word new word to guess
	 * @throws GameIntegrityViolationException when either user is null or word is invalid
	 */
	public void setDrawingUserAndNewWord(ActiveUser user, String word) throws GameIntegrityViolationException{
		if(user == null)
			throw new GameIntegrityViolationException("Cannot set null user as drawing!");
		
		if(gameUtil.isWordInvalid(word))
			throw new GameIntegrityViolationException("Cannot set invalid word!");
		
		// Before setting new drawing user and new word
		// unset all users to not drawing state and unset previous words to guess.
		activeUserService.unsetDrawingStateForAllAndUnsetWords();
		
		db.em().getTransaction().begin();
		
		ActiveUser foundUser = db.em().find(ActiveUser.class, user.getIdau());
		foundUser.setDrawing(true);
		foundUser.setWord(word);
		
		db.em().merge(foundUser);
		
		db.em().getTransaction().commit();
	}

	@Override
	public void close() throws Exception {
		db.close();
	}
}
