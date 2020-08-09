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
import service.ChatService;
import service.LoginService;

@ServerEndpoint("/chat")
public class ChatWebsocket {

	private Session session;
	private boolean isNewSession;
	private static Set<ChatWebsocket> endpoints = new CopyOnWriteArraySet<>();

	private static ChatWebsocket currentUserDrawing;
	private static String currentWordToGuess;
	
	private static ChatService chatService = ChatService.getInstance();

	@OnOpen
	public void onOpen(Session session) throws IOException {		
		this.session = session;
		isNewSession = true;
		if (!endpoints.add(this)) {
			System.out.println("Session already exists!");
		}
		System.out.println("New chat session: " + session.getId() + " (all sessions: " + endpoints.size() + ")");
	}

	@OnMessage
	public void onMessage(Session s, String message) throws IOException {
		// New session, expecting token in the message
		// Allow websocket connection only if the token is valid
		if (isNewSession) {
			if (LoginService.verifyJwt(message)) {
				System.out.println("Token valid");
				startTheGame();
				isNewSession = false;
			} else {
				System.out.println("Token invalid. Closing session...");
				s.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, "Invalid token."));
			}
			return;
		}
		
		
	}

	@OnClose
	public void onClose(Session session) {
		System.out.println("Closing session...");
		this.isNewSession = true;
		endpoints.remove(this);
	}

	/**
	 * If it's start of the game and there are at least 2 players: - generate word
	 * to guess - choose random player to for drawing - send him word to draw
	 * @throws IOException
	 */
	private void startTheGame() throws IOException {
		System.out.println(
				"Is new session: " + isNewSession + "\nUsers: " + endpoints.size() + "\nWord:" + currentWordToGuess);

		if (isNewSession && endpoints.size() > 1 && (currentWordToGuess == null || currentWordToGuess.length() == 0)) {
			synchronized (ChatWebsocket.class) {
				currentWordToGuess = chatService.nextWordToGuess();
				int i = 0;
				int rand = new Random().nextInt(endpoints.size());
				for (ChatWebsocket user : endpoints) {
					if (rand == i) {
						currentUserDrawing = user;
						break;
					}
					i++;
				}
				ChatMessage msg = new ChatMessage(ChatMessage.MsgType.WORD_TO_GUESS, currentWordToGuess);
				Jsonb jsonb = JsonbBuilder.create();
				String msgJson = jsonb.toJson(msg);
				currentUserDrawing.session.getBasicRemote().sendText(msgJson);
				System.out.println("New word to guess: " + currentWordToGuess);
			}
		}
	}
}
