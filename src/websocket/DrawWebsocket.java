package websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import db.AppDictionaryService;
import service.LoginUtil;

/**
 * Websocket used for passing drawn image to other users.
 * 
 * @author Maciej Szaba³a
 *
 */
@ServerEndpoint("/draw")
public class DrawWebsocket {

	private AppDictionaryService dictService = AppDictionaryService.getInstance();
	private LoginUtil loginUtil = LoginUtil.getInstance();
	private Session session;
	private boolean isNewSession;
	private static Set<DrawWebsocket> endpoints = new CopyOnWriteArraySet<>();

	@OnOpen
	public void onOpen(Session session) throws IOException {
		this.session = session;
		isNewSession = true;
		if (!endpoints.add(this)) {
			System.out.println("Session already exists!");
		}
		System.out.println("New draw session: " + session.getId() + " (all sessions: " + endpoints.size() + ")");
	}

	@OnMessage
	public void onMessage(Session s, String message) throws IOException {
		// New session, expecting token in the message
		// Allow websocket connection only if the token is valid
		if (isNewSession) {
			if (loginUtil.verifyJwt(message, dictService.getSecret(), dictService.getOwners())) {
				System.out.println("DrawWebsocket: Token valid");
				isNewSession = false;
			} else {
				System.out.println("DrawWebsocket: Token invalid. Closing session...");
				s.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, "Invalid token."));
			}
			return;
		}
		endpoints.forEach(endpoint -> {
			synchronized (endpoint) {
				try {
					if (!endpoint.equals(this))
						endpoint.session.getBasicRemote().sendText(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@OnClose
	public void onClose(Session session) {
		System.out.println("DrawWebsocket: Closing session...");
		this.isNewSession = true;
		endpoints.remove(this);
	}
}
