package websocket;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import service.RandomWordService;

@ServerEndpoint("/chatOld")
public class ChatWebsocketOld {

	static Set<Session> chatroomUsers = Collections.synchronizedSet(new HashSet<Session>());
	static String wordToGuess = "";
	String username;
	
	private static final String NEXT = "next";
	private static final String USERNAME = "username";
	private static final String FIRST_MESSAGE = "FIRST_MESSAGE";
	private static final String ANSWER = "ANSWER";
	private static final String WORD = "WORD";

	@OnOpen
	public void handleOpen(Session userSession) throws IOException {

		chatroomUsers.add(userSession);
		if(wordToGuess == "")
			wordToGuess = RandomWordService.getNextWord();
		
	}

	@OnMessage
	public void handleMessage(String message, Session userSession) throws Exception {
		Iterator<Session> iterator = chatroomUsers.iterator();
		username = (String) userSession.getUserProperties().get(USERNAME);

		if (username == null && !message.contains(NEXT)) {
			String type = FIRST_MESSAGE;
			userSession.getUserProperties().put(USERNAME, message);
			userSession.getBasicRemote()
					.sendText(buildJsonData("System", "nowy uzytkownik: " + message, type, wordToGuess));
			return;
		}

		if (!message.equals(wordToGuess) && !message.contains(NEXT)) {
			String type = ANSWER;
			while (iterator.hasNext())
				iterator.next().getBasicRemote().sendText(buildJsonData(username, message, type, wordToGuess));
			return;
		}

		if (message.contains(NEXT)) {
			String type = WORD;
			wordToGuess = RandomWordService.getNextWord();
			while (iterator.hasNext())
				iterator.next().getBasicRemote().sendText(buildJsonData("System", "Nowe has³o!", type, wordToGuess));
			return;
		}

		if (message.equals(wordToGuess)) {
			String type = WORD;
			wordToGuess = RandomWordService.getNextWord();
			while (iterator.hasNext())
				iterator.next().getBasicRemote()
						.sendText(buildJsonData("System", "Brawo! chodzi³o o " + message, type, wordToGuess));
			return;
		}
	}

	private String buildJsonData(String username, String message, String type, String txt) {
		JsonObjectBuilder jsonObject = Json.createObjectBuilder();
		jsonObject.add("message", username + ": " + message);
		jsonObject.add("type", type);
		jsonObject.add("txt", txt);

		return jsonObject.build().toString();

	}

	@OnClose
	public void handleClose(Session userSession) {
		chatroomUsers.remove(userSession);

	}

	@OnError
	public void handleError(Throwable t) {
		t.printStackTrace();
	}

}
