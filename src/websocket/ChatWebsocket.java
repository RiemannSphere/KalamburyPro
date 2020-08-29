package websocket;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import db.ActiveUserService;
import db.AppDictionaryService;
import db.WordService;
import exception.GameIntegrityViolationException;
import model.ActiveUser;
import model.ChatMessage;
import model.ChatMessage.MsgType;
import model.Score;
import service.LoginUtil;

/**
 * This websocket contains main logic of an app. It authenticates user after
 * session is opened, then processes incoming messages. Class is responsible for
 * generating new words to guess and choosing next drawing user. It also
 * produces scoreboard.
 * 
 * @author Piotr Ko³odziejski
 */
@ServerEndpoint("/chat")
public class ChatWebsocket {

	private ActiveUserService activeUserService = ActiveUserService.getInstance();
	private LoginUtil loginUtil = LoginUtil.getInstance();
	private AppDictionaryService dictService = AppDictionaryService.getInstance();
	private WordService wordService = WordService.getInstance();
	private Jsonb jsonb;

	private String username;

	/**
	 * Initial procedures when websocket session is opened.
	 * 
	 * @param session current session
	 */
	@OnOpen
	public void onOpen(Session session) {
		jsonb = JsonbBuilder.create();
	}

	/**
	 * Proceeds incoming websocket messages. On very first message it expects JWT
	 * token to authenticate the user. If user is not valid it closes the session.
	 * Messages are processed based on message type (see enum ChatMessage.MsgType).
	 * Method contaings main logic: processing messages, adding active users,
	 * generating new word, choosing next drawing users, closing session in case of
	 * fatal internal errors.
	 * 
	 * @param s       current websocket session
	 * @param message incoming message
	 */
	@OnMessage
	public void onMessage(Session s, String message) {
		try {
			// Is User Active
			if (activeUserService.isUserActive(s.getId())) {
				// Check MsgType
				processBasedOnMsgType(s, message);
			} else {
				// Is Token Valid
				if (loginUtil.verifyJwt(message, dictService.getSecret(), dictService.getOwners())) {
					// Save username as global variable
					username = loginUtil.extractUsernameFromToken(message);

					// Mark user as active
					activeUserService.addActiveUser(username, s.getId());

					// Broadcast scoreboard
					broadcastScoreboard(s);

					if (activeUserService.doesDrawingUserExist()) {
						// Check MsgType
						processBasedOnMsgType(s, message);
					} else {
						// There is no drawing user
						startGame(s);
					}
				} else {
					System.out.println("Token invalid. Closing session...");
					try {
						s.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, "Invalid token."));
					} catch (IOException e) {
						System.out.println("Cannot close Chat Websocket.");
						e.printStackTrace();
					}
				}
			}
		} catch (GameIntegrityViolationException e) {
			e.printStackTrace();
			try {
				s.close(new CloseReason(CloseCodes.UNEXPECTED_CONDITION, "Game integrity has been violated."));
			} catch (IOException e2) {
				System.out.println("Cannot close Chat Websocket.");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Method is being invoked when websocket session is closed. It removes user
	 * from active users. In case drawing user is leaving the game it ensures that
	 * next drawing user is chosen.
	 * 
	 * @param session current session
	 * @throws GameIntegrityViolationException in case of error during word
	 *                                         generation, setting new drawing user,
	 *                                         setting new word to guess or
	 *                                         obtaining data from app dictionary.
	 */
	@OnClose
	public void onClose(Session session) throws GameIntegrityViolationException {
		System.out.println("ChatWebsocket closing session...");
		// Mark user as inactive
		activeUserService.removeActiveUser(session.getId());

		// If this was not the last active user
		if (activeUserService.getActiveUsers().size() > 0) {
			// Find drawing user. Retry few times in case of GameIntegrityViolationException
			ActiveUser drawingUser = null;

			int numOfRetry = dictService.getNumberOfRetries();
			int retryFreq = dictService.getRetryFrequency();
			for (int i = 0; i != numOfRetry; i++) {
				try {
					drawingUser = activeUserService.getActiveDrawingUser();
				} catch (GameIntegrityViolationException e) {
					try {
						Thread.sleep(retryFreq);
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
				}
			}

			// In case there is no drawing user and this is not the last user, start game
			// from random user.
			if (drawingUser == null) {
				System.err.println("Chat Websocket: on close: unable to get drawing user!");
				startGame(session);
			}

			// Broadcast scoreboard
			broadcastScoreboard(session);
		}

		try {
			jsonb.close();
		} catch (Exception e) {
			System.out.println("Jsonb cannot be closed.");
		}
	}

	/**
	 * Broadcast scoreboard to all the users. Display users, their points and
	 * whether they are drawing or not.
	 * 
	 * @param s current websocket session
	 */
	private void broadcastScoreboard(Session s) {
		List<Score> scores = activeUserService.produceScoreboardForActiveUsers();
		String scoresJson = jsonb.toJson(scores);
		ChatMessage response = new ChatMessage(MsgType.SCOREBOARD, scoresJson);
		String responseJson = jsonb.toJson(response);
		for (Session openedSession : s.getOpenSessions()) {
			try {
				if (openedSession.isOpen())
					openedSession.getBasicRemote().sendText(responseJson);
			} catch (IOException e) {
				System.out.println("Chat Websocket: broadcastScoreboard: sending message error.");
				e.printStackTrace();
			}
		}

		System.out.println("Chat Websocket: scoreboard has been updated!");
	}

	/**
	 * Process message based on its type (ChatMessage.MsgType)
	 * 
	 * @param s       current websocket session
	 * @param message message to be processed
	 */
	private void processBasedOnMsgType(Session s, String message) {
		if (message == null) {
			System.out.println("Chat Websocket received null message.");
			return;
		}

		// Parse received message
		System.out.println("ChatWebsocket: message precessed: " + message);
		final ChatMessage msg = jsonb.fromJson(message, ChatMessage.class);
		System.out.println("[" + msg.getMsgType() + "] Message received: " + msg.getMsgContent());

		if (msg.getMsgType().equals(MsgType.MESSAGE.getValue())) {
			processChatMessage(s, msg.getMsgContent());
		}

		if (msg.getMsgType().equals(MsgType.CLEAN_CANVAS.getValue())) {
			System.out.println("Clean Canvas!");
			// Clean canvas for everybody
			ChatMessage response = new ChatMessage(MsgType.CLEAN_CANVAS, "");
			String responseJson = jsonb.toJson(response);
			for (Session openedSession : s.getOpenSessions()) {
				try {
					if (openedSession.isOpen())
						openedSession.getBasicRemote().sendText(responseJson);
				} catch (IOException e) {
					System.out.println("Chat Websocket: clean canvas: sending message error.");
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Checks if the word has been guessed. Guessing by drawing user does not count.
	 * Adds points to winning user in case he guessed the word. Broadcasts messages
	 * to all the users when word has been guessed. Continues the game by choosing
	 * winner for drawing next word. Broadcasts scoreboard. In case the word has not
	 * been guessed broadcasts the message as a regular chat message without
	 * processing.
	 * 
	 * @param msgSender messages sender session
	 * @param msg       message to be processed
	 * @throws GameIntegrityViolationException in case of internal inconsistency
	 *                                         e.g. there is zero or more than one
	 *                                         drawing user, there is more than one
	 *                                         user with certain session id, winning
	 *                                         user is not an active user.
	 */
	private void processChatMessage(Session msgSender, String msg) throws GameIntegrityViolationException {
		// Has word been guessed?
		if (activeUserService.hasWordBeenGuessed(msg)) {
			// Guessed By Drawing User?
			String drawingSessionId = activeUserService.getActiveDrawingUser().getChatSessionId();
			String senderSessionId = msgSender.getId();
			if (senderSessionId.equals(drawingSessionId)) {
				// It does not count! Pass as regular message.
				broadcastMessage(msgSender, msg);
			} else {
				// Add points to user sending the message
				activeUserService.addPointsToTheUser(senderSessionId, 1);
				// Broadcast info about winner
				ChatMessage response = null;
				String responseJson = "";

				// Send message to winning user
				response = new ChatMessage(MsgType.YOU_GUESSED_IT, "Brawo " + username + ", zgad³eœ!");
				responseJson = jsonb.toJson(response);
				try {
					msgSender.getBasicRemote().sendText(responseJson);
				} catch (IOException e) {
					System.out.println("Chat Websocket: info to winner: sending message error");
					e.printStackTrace();
				}

				// Send messages to other users that the word has been guessed
				response = new ChatMessage(MsgType.MESSAGE, "U¿ytkownik " + username + " odgad³ has³o!");
				responseJson = jsonb.toJson(response);
				for (Session openedSession : msgSender.getOpenSessions()) {
					if (!openedSession.equals(msgSender)) {
						try {
							if (openedSession.isOpen())
								openedSession.getBasicRemote().sendText(responseJson);
						} catch (IOException e) {
							System.out.println("Chat Websocket: broadcast winner info: sending message error");
							e.printStackTrace();
						}
					}
				}

				// Broadcast cleaning canvas
				response = new ChatMessage(MsgType.CLEAN_CANVAS, "");
				responseJson = jsonb.toJson(response);
				for (Session openedSession : msgSender.getOpenSessions()) {
					try {
						if (openedSession.isOpen())
							openedSession.getBasicRemote().sendText(responseJson);
					} catch (IOException e) {
						System.out.println("Chat Websocket: clean canvas for all: sending message error");
						e.printStackTrace();
					}
				}

				// Continue game, user who guessed the word is not drawing
				continueGameWithWinner(msgSender);

				// Broadcast scoreboard
				broadcastScoreboard(msgSender);
			}
		} else {
			broadcastMessage(msgSender, msg);
		}
	}

	/**
	 * Broadcasts regular chat message. Adds username of a message sender.
	 * 
	 * @param s   current websocket session
	 * @param msg message to be sent to other users
	 */
	private void broadcastMessage(Session s, String msg) {
		ChatMessage response = new ChatMessage(MsgType.MESSAGE, username + ": " + msg);
		String responseJson = jsonb.toJson(response);
		for (Session openedSession : s.getOpenSessions()) {
			try {
				if (openedSession.isOpen())
					openedSession.getBasicRemote().sendText(responseJson);
			} catch (IOException e) {
				System.out.println("Chat Websocket: pass regular message: sending message error");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Starting game means choosing random user and sending him random word to
	 * guess. Game is started when there was no previous drawing user i.e. it is
	 * first user in the game or drawing user has left the game.
	 * 
	 * @param s current websocket session
	 * @throws GameIntegrityViolationException in case of error during word
	 *                                         generation, setting new drawing user
	 *                                         or setting new word to guess.
	 */
	private void startGame(Session s) throws GameIntegrityViolationException {
		// Get random active user to draw
		ActiveUser newDrawingUser = activeUserService.getRandomActiveUser();

		// Get random word
		String newWord = wordService.getRandomWord();

		// Set new drawing user in database. Set also new word to guess
		activeUserService.setDrawingUserAndNewWord(newDrawingUser, newWord);

		// Clean word to guess for all
		ChatMessage response = new ChatMessage(MsgType.CLEAN_WORD_TO_GUESS, "");
		String responseJson = jsonb.toJson(response);
		for (Session openedSession : s.getOpenSessions()) {
			try {
				if (openedSession.isOpen())
					openedSession.getBasicRemote().sendText(responseJson);
			} catch (IOException e) {
				System.out.println("Chat Websocket: clean word to guess: sending message error");
				e.printStackTrace();
			}
		}

		// Notify new drawing user and send him word to draw
		ChatMessage msg = new ChatMessage(MsgType.WORD_TO_GUESS, newWord);
		String msgJson = jsonb.toJson(msg);

		for (Session openedSession : s.getOpenSessions()) {
			if (openedSession.getId().equals(newDrawingUser.getChatSessionId())) {
				try {
					if (openedSession.isOpen())
						openedSession.getBasicRemote().sendText(msgJson);
				} catch (IOException e) {
					System.out.println("Chat Websocket: send word to guess: sending message error");
					e.printStackTrace();
				}
			}
		}

		// Broadcast scoreboard
		broadcastScoreboard(s);
	}

	/**
	 * Continuing game means the next drawing user is the user who won last turn.
	 * 
	 * @param winner user who guessed previous word
	 */
	private void continueGameWithWinner(Session winner) {
		// Get winner by his session id
		ActiveUser newDrawingUser = activeUserService.getActiveUserBySessionId(winner.getId());

		// Get random word
		String newWord = wordService.getRandomWord();

		// Set new drawing user in database. Set also new word to guess
		activeUserService.setDrawingUserAndNewWord(newDrawingUser, newWord);

		// Clean word to guess for all
		ChatMessage response = new ChatMessage(MsgType.CLEAN_WORD_TO_GUESS, "");
		String responseJson = jsonb.toJson(response);
		for (Session openedSession : winner.getOpenSessions()) {
			try {
				if (openedSession.isOpen())
					openedSession.getBasicRemote().sendText(responseJson);
			} catch (IOException e) {
				System.out.println("Chat Websocket: clean word to guess: sending message error");
				e.printStackTrace();
			}
		}

		// Notify new drawing user and send him word to draw
		ChatMessage msg = new ChatMessage(MsgType.WORD_TO_GUESS, newWord);
		String msgJson = jsonb.toJson(msg);
		try {
			Session newDrawing = winner.getOpenSessions().stream()
					.filter((session) -> session.getId().equals(newDrawingUser.getChatSessionId())).findFirst().get();
			newDrawing.getBasicRemote().sendText(msgJson);
		} catch (NoSuchElementException e) {
			throw new GameIntegrityViolationException("Cannot choose non-existing user for drawing!", e);
		} catch (IOException e) {
			throw new GameIntegrityViolationException("New drawing user was not set properly!", e);
		}

		// Broadcast scoreboard
		broadcastScoreboard(winner);
	}
}
