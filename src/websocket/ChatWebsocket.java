package websocket;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
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
import service.ChatService;
import service.LoginService;
import service.LoginUtil;

/**
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

	@OnOpen
	public void onOpen(Session session) throws IOException {
		jsonb = JsonbBuilder.create();
	}

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

	@OnClose
	public void onClose(Session session) {
		try {
			jsonb.close();
		} catch (Exception e) {
			System.out.println("Jsonb cannot be closed.");
		}
	}

	/**
	 * @param s current user session
	 */
	private void broadcastScoreboard(Session s) {
		List<Score> scores = activeUserService.produceScoreboardForActiveUsers();
		String scoresJson = jsonb.toJson(scores);
		ChatMessage response = new ChatMessage(MsgType.SCOREBOARD, scoresJson);
		String responseJson = jsonb.toJson(response);
		for (Session openedSession : s.getOpenSessions()) {
			try {
				openedSession.getBasicRemote().sendText(responseJson);
			} catch (IOException e) {
				System.out.println("Chat Websocket: broadcastScoreboard: sending message error.");
				e.printStackTrace();
			}
		}

		System.out.println("Chat Websocket: scoreboard has been updated!");
	}

	private void processBasedOnMsgType(Session s, String message) {
		if (message == null) {
			System.out.println("Chat Websocket received null message.");
			return;
		}

		// Parse received message
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
					openedSession.getBasicRemote().sendText(responseJson);
				} catch (IOException e) {
					System.out.println("Chat Websocket: clean canvas: sending message error.");
					e.printStackTrace();
				}
			}
		}
	}

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

	private void broadcastMessage(Session s, String msg) {
		ChatMessage response = new ChatMessage(MsgType.MESSAGE, username + ": " + msg);
		String responseJson = jsonb.toJson(response);
		for (Session openedSession : s.getOpenSessions()) {
			try {
				openedSession.getBasicRemote().sendText(responseJson);
			} catch (IOException e) {
				System.out.println("Chat Websocket: pass regular message: sending message error");
				e.printStackTrace();
			}
		}
	}

	private void startGame(Session s) throws GameIntegrityViolationException {
		// Get random active user to draw
		ActiveUser newDrawingUser = activeUserService.getRandomActiveUser();

		// Get random word
		String newWord = wordService.getRandomWord();

		// Set new drawing user in database. Set also new word to guess
		activeUserService.setDrawingUserAndNewWord(newDrawingUser, newWord);

		// Notify new drawing user and send him word to draw
		ChatMessage msg = new ChatMessage(MsgType.WORD_TO_GUESS, newWord);
		String msgJson = jsonb.toJson(msg);
		try {
			Session newDrawing = s.getOpenSessions().stream()
					.filter((session) -> session.getId().equals(newDrawingUser.getChatSessionId())).findFirst().get();
			newDrawing.getBasicRemote().sendText(msgJson);
		} catch (NoSuchElementException e) {
			throw new GameIntegrityViolationException("Cannot choose non-existing user for drawing!", e);
		} catch (IOException e) {
			throw new GameIntegrityViolationException("New drawing user was not set properly!", e);
		}

		// Broadcast scoreboard
		broadcastScoreboard(s);

		// Clean word to guess for all
		ChatMessage response = new ChatMessage(MsgType.CLEAN_WORD_TO_GUESS, "");
		String responseJson = jsonb.toJson(response);
		for (Session openedSession : s.getOpenSessions()) {
			try {
				openedSession.getBasicRemote().sendText(responseJson);
			} catch (IOException e) {
				System.out.println("Chat Websocket: clean word to guess: sending message error");
				e.printStackTrace();
			}
		}
	}

	private void continueGameWithWinner(Session winner) {

	}
}
