package model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Active user
 * 
 * @author Piotr Ko³odziejski
 */
@Entity
@Table(name = "u¿ytkownicy_aktywni")
public class ActiveUser {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long idau;

	@Column(name = "rysuje")
	private Boolean isDrawing;

	@Column(name = "rysowane_s³owo")
	private String word;

	@Column(name = "id_sesji")
	private String chatSessionId;

	@OneToOne
	@JoinColumn(name = "idu")
	private User user;

	public ActiveUser() {

	}

	public Long getIdau() {
		return idau;
	}

	public void setIdau(Long idau) {
		this.idau = idau;
	}

	public boolean isDrawing() {
		return isDrawing;
	}

	public void setDrawing(boolean isDrawing) {
		this.isDrawing = isDrawing;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getChatSessionId() {
		return chatSessionId;
	}

	public void setChatSessionId(String chatSessionId) {
		this.chatSessionId = chatSessionId;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}
}
