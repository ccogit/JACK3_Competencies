package de.uni_due.s3.jack3.services;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

/**
 * Service for development purposes that executes direct queries from a given {@link String}.
 */
@Stateless
public class DatabaseTestService extends AbstractServiceBean {

	/* This query is responsible for creating a testing table .*/
	private static final String INITIALIZATION_QUERY =
		// 1. We drop an old table if there was one.
		"DROP TABLE IF EXISTS public.connectivity_test;" +

		// 2. We create the new test table with one timestamp column.
		"CREATE TABLE public.connectivity_test (" +
		"   \"timestamp\" timestamp without time zone" +
		") WITH (OIDS = FALSE);" +
		
		// 3. We alter the table's owner to jack. 
		"ALTER TABLE IF EXISTS public.connectivity_test OWNER to jack;" +
		
		// 4. We insert one row that we can use later on for an update test.
		"INSERT INTO public.connectivity_test SELECT CURRENT_TIMESTAMP;";
	
	private static final String CONNECTIVITY_QUERY = "SELECT 1;";
	
	private static final String UPDATE_QUERY = "UPDATE public.connectivity_test " +
		"SET timestamp = CURRENT_TIMESTAMP;";
	
	@PostConstruct
	void prepareTestTable() {
		createQuery(INITIALIZATION_QUERY).executeUpdate();
	}
	
	public boolean isConnected() {
		try {
			final Number result = (Number)createQuery(CONNECTIVITY_QUERY).getSingleResult();
			return result.intValue() == 1;
		}
		catch (final PersistenceException e) {
			getLogger().error("The database connectivity test failed.",e);
			return false;
		}
	}

	public boolean canWrite() {
		try {
			int update = createQuery(UPDATE_QUERY).executeUpdate();
			return update == 1;
		}
		catch (final PersistenceException e) {
			getLogger().error("The database update test failed.",e);
			return false;
		}
	}
	
	private final Query createQuery(final String query) {
		return getEntityManager().createNativeQuery(query);
	}
}
