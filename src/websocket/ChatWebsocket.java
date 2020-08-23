package websocket;

import java.io.IOException;
import java.util.List;
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

import model.ChatMessage;
import model.ChatMessage.MsgType;
import model.Score;
import service.ChatService;
import service.LoginService;

/**
 * 
 * @author Piotr Ko³odziejski
 */
@ServerEndpoint("/chat")
public class ChatWebsocket {

	private Session session;
	private boolean isNewSession;
	private static Set<ChatWebsocket> endpoints = new CopyOnWriteArraySet<>();

	private static ChatService chatService = ChatService.getInstance();
	private static LoginService loginService = LoginService.getInstance();
	private Jsonb jsonb;

	private String username;

	@OnOpen
	public void onOpen(Session session) throws IOException {
		jsonb = JsonbBuilder.create();
		this.session = session;
		isNewSession = true;
		if (!endpoints.add(this)) {
			System.out.println("Session already exists!");
		}
		System.out.println("New chat session: " + session.getId() + " (all sessions: " + endpoints.size() + ")");
	}

	@OnMessage
	public void onMessage(Session s, String message) {
		// New session, expecting token in the message
		// Allow websocket connection only if the token is valid
		if (isNewSession) {
			if (loginService.verifyJwt(message)) {
				System.out.println("Token valid");
				isNewSession = false;

				// Set current user username
				username = loginService.extractUsernameFromToken(message);

				// Mark user as active
				loginService.markUserAsActive(username, s.getId());

				// If there is no currently drawing user or word is not set.
				if (chatService.getCurrentUserDrawing() == null || chatService.getCurrentWordToGuess() == null) {
					try {
						continueTheGame(null);
					} catch (IOException e) {
						System.out.println("Chat Websocket sending message error.");
						e.printStackTrace();
					}
				}
				
				// Update scoreboard
				updateScoreboard();
			} else {
				System.out.println("Token invalid. Closing session...");
				try {
					s.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, "Invalid token."));
				} catch (IOException e) {
					System.out.println("Cannot close Chat Websocket.");
					e.printStackTrace();
				}
			}
			return;
		}

		// If there is no currently drawing user or word is not set.
		if (chatService.getCurrentUserDrawing() == null || chatService.getCurrentWordToGuess() == null) {
			try {
				continueTheGame(null);
			} catch (IOException e) {
				System.out.println("Chat Websocket sending message error.");
				e.printStackTrace();
			}
		}

		if (message == null) {
			System.out.println("Chat Websocket received null message.");
			return;
		}

		final ChatMessage msg = jsonb.fromJson(message, ChatMessage.class);
		System.out.println("[" + msg.getMsgType() + "] Message received: " + msg.getMsgContent());

		if (msg.getMsgType().equals(MsgType.MESSAGE.getValue())) {
			try {
				processMessage(msg.getMsgContent(), s);
			} catch (IOException e) {
				System.out.println("Chat Websocket sending message error.");
				e.printStackTrace();
			} catch (NoResultException e) {
				System.out.println("Chat Websocket: there is no drawing user.");
				e.printStackTrace();
			} catch (NonUniqueResultException e) {
				System.out.println("Chat Websocket: there is more than 1 drawing user.");
				e.printStackTrace();
			}
		}

		if (msg.getMsgType().equals(MsgType.CLEAN_CANVAS.getValue())) {
			System.out.println("Clean Canvas!");
			// Clean canvas for everybody
			ChatMessage response = new ChatMessage(MsgType.CLEAN_CANVAS, "");
			String responseJson = jsonb.toJson(response);
			for (ChatWebsocket user : endpoints) {
				try {
					user.session.getBasicRemote().sendText(responseJson);
				} catch (IOException e) {
					System.out.println("Chat Websocket sending message error.");
					e.printStackTrace();
				}
			}
		}
	}

	@OnClose
	public void onClose(Session session) {
		System.out.println("Closing session...");

		this.isNewSession = true;
		
		// remove session
		endpoints.remove(this);
		
		// If drawing user is gone, get new user and new word
		if (this.session.equals(chatService.getCurrentUserDrawing())) {
			try {
				continueTheGame(null);
			} catch (IOException e) {
				System.out.println("Chat Websocket sending message error.");
				e.printStackTrace();
			}
		}

		// Mark user as inactive
		loginService.markUserAsInactive(username);
		
		try {
			jsonb.close();
		} catch (Exception e) {
			System.out.println("Jsonb cannot be closed.");
		}
		
		// Update scoreboard
		updateScoreboard();
		
		System.out.println("Chat Websocket: on close: user " + username + " has been removed.");
	}

	/**
	 * 
	 * @param nextDrawingUserSession
	 * @throws IOException
	 */
	private void continueTheGame(Session nextDrawingUserSession) throws IOException {
		if (endpoints.isEmpty()) {
			// Clean current word and current drawing user
			chatService.setCurrentUserDrawing(null);
			chatService.cleanCurrentWordToGuess();
			return;
		}
		
		// Select next user for drawing
		setNextUserForDrawing(nextDrawingUserSession);

		// Send all command to clean current word
		cleanWordToGuessForAll();

		// Send him generated word to draw
		sendNextWordToGuess();
	}

	private synchronized void setNextUserForDrawing(Session nextDrawingUser) {
		
		if(nextDrawingUser == null) {
			// Choose random user for drawing
			String chatSessionId = chatService.setDrawingUserInDb(null);
			for (ChatWebsocket user : endpoints) {
				System.out.println(user.session.getId() + " == " + chatSessionId);
				if( user.session.getId().equals(chatSessionId) ) {
					chatService.setCurrentUserDrawing(user.session);
					return;
				}
			}
			System.err.println("ChatWebsocket: setNextUserForDrawing: next drawing user has not been set.");
			System.err.println("ChatWebsocket: setNextUserForDrawing: chatSessionId = " + chatSessionId);
			return;
		}
		
		// Update database
		chatService.setDrawingUserInDb(nextDrawingUser.getId());
		
		// Set global variable for currently drawing user
		chatService.setCurrentUserDrawing(nextDrawingUser);
	}

	private synchronized void cleanWordToGuessForAll() throws IOException {
		chatService.cleanCurrentWordToGuess();

		ChatMessage response = new ChatMessage(MsgType.CLEAN_WORD_TO_GUESS, "");
		String responseJson = jsonb.toJson(response);
		for (ChatWebsocket user : endpoints) {
			user.session.getBasicRemote().sendText(responseJson);
		}
	}

	private synchronized void sendNextWordToGuess() throws IOException {
		chatService.nextWordToGuess();
		ChatMessage msg = new ChatMessage(MsgType.WORD_TO_GUESS, chatService.getCurrentWordToGuess());
		String msgJson = jsonb.toJson(msg);
		chatService.getCurrentUserDrawing().getBasicRemote().sendText(msgJson);
		System.out.println("New word to guess: " + chatService.getCurrentWordToGuess());
	}

	private synchronized void processMessage(String msg, Session msgSender)
			throws IOException, NoResultException, NonUniqueResultException {
		System.out.println("Processing message: " + msg);
		ChatMessage response = null;
		String responseJson = "";

		// Word has been guessed (not by drawing user)
		if (chatService.isWordGuessed(msg) && !msgSender.equals(chatService.getCurrentUserDrawing())) {
			// Add points to the sender
			chatService.addPointsToTheUser(msgSender.getId(), 1);
			
			// Update scoreboard
			updateScoreboard();

			// Send message to winning user
			response = new ChatMessage(MsgType.YOU_GUESSED_IT, "Brawo " + username + ", zgad³eœ!");
			responseJson = jsonb.toJson(response);
			msgSender.getBasicRemote().sendText(responseJson);

			// Send messages to other users (besides drawing user) that the word has been
			// guessed
			response = new ChatMessage(MsgType.MESSAGE, "U¿ytkownik " + username + " odgad³ has³o!");
			responseJson = jsonb.toJson(response);
			for (ChatWebsocket user : endpoints) {
				if (!user.session.equals(msgSender) && !user.session.equals(chatService.getCurrentUserDrawing())) {
					user.session.getBasicRemote().sendText(responseJson);
				}
			}

			// Inform drawing user that word has been guessed.
			response = new ChatMessage(MsgType.MESSAGE, "U¿ytkownik " + username + " odgad³ has³o!");
			responseJson = jsonb.toJson(response);
			chatService.getCurrentUserDrawing().getBasicRemote().sendText(responseJson);

			// Clean canvas for everybody
			response = new ChatMessage(MsgType.CLEAN_CANVAS, "");
			responseJson = jsonb.toJson(response);
			for (ChatWebsocket user : endpoints) {
				user.session.getBasicRemote().sendText(responseJson);
			}

			// Get next user to draw and next word to guess
			continueTheGame(msgSender);
			return;
		}

		// Pass regular message to other users
		response = new ChatMessage(MsgType.MESSAGE, username + ": " + msg);
		responseJson = jsonb.toJson(response);
		for (ChatWebsocket user : endpoints) {
			user.session.getBasicRemote().sendText(responseJson);
		}
	}
	
	private void updateScoreboard() {
		List<Score> scores = chatService.scoreboard();
		String scoresJson = jsonb.toJson(scores);
		ChatMessage response = new ChatMessage(MsgType.SCOREBOARD, scoresJson);
		String responseJson = jsonb.toJson(response);
		for (ChatWebsocket user : endpoints) {
			try {
				user.session.getBasicRemote().sendText(responseJson);
			} catch (IOException e) {
				System.out.println("Chat Websocket sending message error.");
				e.printStackTrace();
			}
		}
	}
}
