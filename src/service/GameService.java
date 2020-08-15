package service;

import java.util.List;

public class GameService {

	// game state
	
	private static GameService instance;
	
	private GameService() {
	}
	
	public static GameService getInstance() {
		if (instance == null) {
			instance = new GameService();
		}
		return instance;
	}
	
}
