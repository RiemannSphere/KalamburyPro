package model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Password in form of hash and its salt.
 * 
 * @author Maciej Szaba³a
 *
 */
@Entity
@Table(name = "has³a")
public class Password {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "idp")
	private Long id;

	@Lob
	@Column(name = "sól", length = 16)
	private byte[] salt;

	@Lob
	@Column(name = "hash", length = 128)
	private byte[] hash;

	@OneToOne
	private User user;

	public Password() {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public byte[] getSalt() {
		return salt;
	}

	public void setSalt(byte[] salt) {
		this.salt = salt;
	}

	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash = hash;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

}
