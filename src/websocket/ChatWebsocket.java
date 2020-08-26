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

import db.ActiveUserService;
import db.AppDictionaryService;
import exception.GameIntegrityViolationException;
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
			if( activeUserService.isUserActive(s.getId()) ) {
				// Check MsgType
				processBasedOnMsgType(s, message);
			} else {
				// Is Token Valid
				if(loginUtil.verifyJwt(message, dictService.getSecret(), dictService.getOwners())) {
					// Save username as global variable
					username = loginUtil.extractUsernameFromToken(message);
					
					// Mark user as active
					activeUserService.addActiveUser(username, s.getId());
					
					// Broadcast scoreboard
					broadcastScoreboard(s);
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
		} catch(GameIntegrityViolationException e) {
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
	
	private void broadcastScoreboard(Session s) {
		List<Score> scores = activeUserService.produceScoreboardForActiveUsers();
		String scoresJson = jsonb.toJson(scores);
		ChatMessage response = new ChatMessage(MsgType.SCOREBOARD, scoresJson);
		String responseJson = jsonb.toJson(response);
		for (Session openedSession : s.getOpenSessions()) {
			try {
				openedSession.getBasicRemote().sendText(responseJson);
			} catch (IOException e) {
				System.out.println("Chat Websocket sending message error.");
				e.printStackTrace();
			}
		}

		System.out.println("Chat Websocket: scoreboard has been updated!");
	}
	
	private void processBasedOnMsgType(Session s, String message) {
		
	}

}
