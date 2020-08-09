package websocket;

import java.io.IOException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import model.ChatMessage;
import model.ChatMessage.MsgType;
import service.ChatService;
import service.LoginService;

@ServerEndpoint("/chat")
public class ChatWebsocket {

	private Session session;
	private boolean isNewSession;
	private static Set<ChatWebsocket> endpoints = new CopyOnWriteArraySet<>();

	private static ChatService chatService = ChatService.getInstance();
	private Jsonb jsonb;

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
			if (LoginService.verifyJwt(message)) {
				System.out.println("Token valid");
				isNewSession = false;
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
				continueTheGame();
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
		endpoints.remove(this);
		try {
			jsonb.close();
		} catch (Exception e) {
			System.out.println("Jsonb cannot be closed.");
		}
	}

	/**
	 * If it's start of the game and there are at least 2 players: - generate word
	 * to guess - choose random player to for drawing - send him word to draw
	 * 
	 * @throws IOException
	 */
	private synchronized void continueTheGame() throws IOException {
		// Select random user for drawing
		int i = 0;
		int rand = new Random().nextInt(endpoints.size());
		for (ChatWebsocket user : endpoints) {
			if (rand == i) {
				chatService.setCurrentUserDrawing(user);
				break;
			}
			i++;
		}
		
		// Send him generated word to draw
		sendNextWordToGuess();
	}

	private synchronized void sendNextWordToGuess() throws IOException {
		chatService.nextWordToGuess();
		ChatMessage msg = new ChatMessage(MsgType.WORD_TO_GUESS, chatService.getCurrentWordToGuess());
		String msgJson = jsonb.toJson(msg);
		chatService.getCurrentUserDrawing().session.getBasicRemote().sendText(msgJson);
		System.out.println("New word to guess: " + chatService.getCurrentWordToGuess());
	}

	private synchronized void processMessage(String msg, Session msgSender) throws IOException {
		System.out.println("Processing message: " + msg);
		ChatMessage response = null;
		String responseJson = "";

		// Word has been guessed (not by drawing user)
		if (chatService.isWordGuessed(msg) && !msgSender.equals(chatService.getCurrentUserDrawing().session)) {
			// Send message to winning user
			response = new ChatMessage(MsgType.YOU_GUESSED_IT, "Zgad³eœ!");
			responseJson = jsonb.toJson(response);
			msgSender.getBasicRemote().sendText(responseJson);

			// Send messages to other users (besides drawing user) that the word has been
			// guessed
			response = new ChatMessage(MsgType.MESSAGE, "Ktoœ inny zgad³ has³o...");
			responseJson = jsonb.toJson(response);
			for (ChatWebsocket user : endpoints) {
				if (!user.session.equals(msgSender) && !user.equals(chatService.getCurrentUserDrawing())) {
					user.session.getBasicRemote().sendText(responseJson);
				}
			}

			// Inform drawing user that word has been guessed.
			response = new ChatMessage(MsgType.MESSAGE, "Ktoœ odgad³ has³o!");
			responseJson = jsonb.toJson(response);
			chatService.getCurrentUserDrawing().session.getBasicRemote().sendText(responseJson);

			// Clean canvas for everybody
			response = new ChatMessage(MsgType.CLEAN_CANVAS, "");
			responseJson = jsonb.toJson(response);
			for (ChatWebsocket user : endpoints) {
				user.session.getBasicRemote().sendText(responseJson);
			}

			// Get next user to draw and next word to guess
			continueTheGame();
			return;
		}

		// Pass regular message to other users
		response = new ChatMessage(MsgType.MESSAGE, msg);
		responseJson = jsonb.toJson(response);
		for (ChatWebsocket user : endpoints) {
			if (!user.equals(chatService.getCurrentUserDrawing())) {
				user.session.getBasicRemote().sendText(responseJson);
			}
		}
	}
}
