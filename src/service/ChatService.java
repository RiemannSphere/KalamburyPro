package service;

import java.util.Random;

public class ChatService {
	public static String nextWordToGuess() {
		return "s�owo " + new Random().nextInt(1000);
	}
}
