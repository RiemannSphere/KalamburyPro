package model;

/**
 * Element of displayed scoreboard.
 * 
 * @author Maciej Szaba³a
 */
public class Score {

	private String username;
	private Boolean isDrawing;
	private Integer points;

	public Score() {

	}

	public Score(String username, Boolean isDrawing, Integer points) {
		this.username = username;
		this.isDrawing = isDrawing;
		this.points = points;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Boolean getIsDrawing() {
		return isDrawing;
	}

	public void setIsDrawing(Boolean isDrawing) {
		this.isDrawing = isDrawing;
	}

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

}
