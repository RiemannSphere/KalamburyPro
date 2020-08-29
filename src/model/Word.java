package model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Word to guess
 * 
 * @author Maciej Szaba³a
 *
 */
@Entity
@Table(name = "slowa")
public class Word {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	@NotNull
	private Long id;

	@Column(name = "slowo")
	@NotNull
	private String word;

	public Word() {
	}

	public Word(String word) {
		this.word = word;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

}
