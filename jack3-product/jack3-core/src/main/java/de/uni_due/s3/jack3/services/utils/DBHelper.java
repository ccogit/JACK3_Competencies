package de.uni_due.s3.jack3.services.utils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;

import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import de.uni_due.s3.jack3.utils.EntityReflectionHelper;

public class DBHelper {

	private DBHelper() {
		throw new IllegalStateException("Class must only be used statically!");
	}

	/**
	 * This method should most of the time be used instead of entityManager.getSingleResult(), because getSingleResult
	 * throws an Exception if there is no result, which is almost always a valid state. To further enforce this an
	 * optional is returned, so the clientcode has to deal with an empty result
	 * 
	 * @see <a href="http://sysout.be/2011/03/09/why-you-should-never-use-getsingleresult-in-jpa">Why you should never
	 *      user getSingleResult in JPA</a>
	 */
	public static <T> Optional<T> getOneOrZero(TypedQuery<T> query) {
		return getOneOrZeroOfCollection(query.getResultList(), false);
	}

	public static <T> Optional<T> getOneOrZeroInitializeObjectGraph(TypedQuery<T> query) {
		return getOneOrZeroOfCollection(query.getResultList(), true);
	}

	public static <T, C> Optional<C> getOneOrZeroConvertToTypeRemovingDuplicates(TypedQuery<T> query,
			final Class<C> clazz) {
		LinkedHashSet<T> result = new LinkedHashSet<>(query.getResultList());
		return getOneOrZeroOfCollectionConvertToType(result, clazz);
	}

	/**
	 * By using JOIN FETCH we possible get duplicates from outer joins on the table. To remove these duplicates we
	 * convert the resultlist to a set.
	 * 
	 * @see <a href=
	 *      "https://developer.jboss.org/wiki/HibernateFAQ-AdvancedProblems?_sscc=t#jive_content_id_Hibernate_does_not_return_distinct_results_for_a_query_with_outer_join_fetching_enabled_for_a_collection_even_if_I_use_the_distinct_keyword">Hibernate
	 *      does not return distinct results for a query with outer join fetching enabled for a collection (even if I
	 *      use the distinct keyword)?</a>
	 */
	public static <T> Optional<T> getOneOrZeroRemovingDuplicates(TypedQuery<T> query) {
		LinkedHashSet<T> result = new LinkedHashSet<>(query.getResultList());
		return getOneOrZeroOfCollection(result, false);
	}

	private static <T> Optional<T> getOneOrZeroOfCollection(Collection<T> dbResultCollection,
			boolean initializeObjectGraph) {
		if (dbResultCollection.size() > 1) {
			throw new NonUniqueResultException(
					"There is more than one element in unique collection:" + dbResultCollection);
		} else if (dbResultCollection.size() == 1) {
			T next = dbResultCollection.iterator().next();
			if (initializeObjectGraph) {
				EntityReflectionHelper.hibernateInitializeObjectGraph(next);
			}
			return Optional.of(next);
		} else {
			return Optional.empty();
		}
	}

	private static <T, C> Optional<C> getOneOrZeroOfCollectionConvertToType(Collection<T> dbResultCollection,
			Class<C> clazz) {
		if (dbResultCollection.size() > 1) {
			throw new NonUniqueResultException(
					"There is more than one element in unique collection:" + dbResultCollection);
		} else if (dbResultCollection.size() == 1) {
			return Optional.of(clazz.cast(dbResultCollection.iterator().next()));
		} else {
			return Optional.empty();
		}
	}
}
