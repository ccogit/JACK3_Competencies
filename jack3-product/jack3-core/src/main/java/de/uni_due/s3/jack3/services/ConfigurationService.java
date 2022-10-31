package de.uni_due.s3.jack3.services;

import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import de.uni_due.s3.jack3.entities.tenant.Config;
import de.uni_due.s3.jack3.services.utils.DBHelper;

/**
 * Service for managing {@link Config} entities.
 */
@Stateless
public class ConfigurationService extends AbstractServiceBean {
	
	@Inject
	private BaseService baseService;

	/**
	 * Persists a new config pair in the persistence context.
	 * 
	 * @param config
	 *            The new config pair to persist.
	 * @return {@code TRUE} if the config was added or {@code FALSE} if the config was not added due an exception or
	 *         because the config already exists.
	 */
	public boolean saveConfig(Config config) {
		if (configKeyValuePairWithKeyExists(config)) {
			getLogger().warn("Config key " + config + " already exists, not added!");
			return false;
		}
		try {
			baseService.persist(config);
			return true;
		} catch (Exception e) {
			getLogger().error("Error while trying to persist " + config, e);
			return false;
		}
	}

	/**
	 * @param config
	 *            Any config pair
	 * @return Wether the key of the given config already belongs to an existing config pair.
	 */
	public boolean configKeyValuePairWithKeyExists(Config config) {
		Optional<Config> optionalResultByKey = getConfigByKey(config.getKey());
		return optionalResultByKey.isPresent();
	}

	/**
	 * Lookups a config pair in the database.
	 * 
	 * @param key
	 *            The key of the config pair.
	 */
	public Optional<Config> getConfigByKey(String key) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Config> query = em.createNamedQuery(Config.GET_BY_KEY, Config.class);
		query.setParameter("key", key);

		return DBHelper.getOneOrZero(query);
	}

	public void deleteConfigKeyValuePair(Config configKeyValuePair) {
		baseService.deleteEntity(configKeyValuePair);
	}

}