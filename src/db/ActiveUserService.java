package db;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityExistsException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import exception.GameIntegrityViolationException;
import model.ActiveUser;
import model.Score;
import model.User;

public class ActiveUserService implements AutoCloseable {

	private Database db = Database.getInstance();
	private UserService userService = UserService.getInstance();

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
		return db.em().createQuery("SELECT au FROM ActiveUser au", ActiveUser.class).getResultList().stream()
				.map((au) -> new Score(au.getUser().getUsername(), au.isDrawing(), au.getUser().getPoints()))
				.collect(Collectors.toList());
	}
	
	/**
	 * @return true is drawing user exists, false otherwise
	 * @throws GameIntegrityViolationException if there is more than one drawing user
	 */
	public boolean doesDrawingUserExist() throws GameIntegrityViolationException{
		try {
			db.em().createQuery("SELECT au FROM ActiveUser au WHERE au.isDrawing = true", ActiveUser.class).getSingleResult();
			return true;
		} catch (NoResultException e) {
			return false;
		} catch (NonUniqueResultException e) {
			throw new GameIntegrityViolationException("More than one drawing user!", e);
		}
	}

	@Override
	public void close() throws Exception {
		db.close();
	}
}
