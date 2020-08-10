package model;

import javax.persistence.Entity;

@Entity
public class User {

	private String username;
	private Integer points;
	private Integer level;
	
	private UserState state;
	
	public enum UserState {
		GUESSING("GUESSING"), DRAWING("DRAWING");
		
		private String value;
		
		private UserState(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
	}
	
}
