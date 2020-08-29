package db;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * This is a singleton class initializing EntityManager. It is meant to be
 * injected whenever interaction with database is needed.
 * 
 * @author Maciej Szaba³a
 *
 */
public class Database implements AutoCloseable {

	private EntityManagerFactory emf;
	private EntityManager em;

	private static Database instance;

	private Database() {
	}

	/**
	 * Implementation of the singleton pattern. Creates Database object, then
	 * initializes persistence layer.
	 * 
	 * @return instance of Database
	 */
	public static Database getInstance() {
		if (instance == null) {
			instance = new Database();
			instance.initPersistence();
		}
		return instance;
	}

	/**
	 * Initializes persistence layer, creates EntityManager instance.
	 */
	private void initPersistence() {
		try {
			emf = Persistence.createEntityManagerFactory("postgres");
			em = emf.createEntityManager();
		} catch (Exception e) {
			System.err.println("Database init Entity Manager failed.");
			e.printStackTrace();
		}
	}

	/**
	 * @return instance of Entity Manager
	 */
	public EntityManager em() {
		return em;
	}

	/**
	 * On Database close closes all objects used by Database.
	 */
	@Override
	public void close() {
		try {
			em.close();
			emf.close();
		} catch (Exception e) {
			System.err.println("Database close failed.");
		}
	}

}
