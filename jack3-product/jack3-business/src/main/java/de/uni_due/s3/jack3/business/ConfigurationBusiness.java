package de.uni_due.s3.jack3.business;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.NonUniqueResultException;
import javax.transaction.Transactional;

import com.google.common.base.VerifyException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.uni_due.s3.jack3.entities.tenant.Config;
import de.uni_due.s3.jack3.interfaces.ConfigurationChangeListener;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.services.ConfigurationService;

@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class ConfigurationBusiness extends AbstractBusiness {

	private static Map<String, String> configurationCache = new ConcurrentHashMap<>();

	private static Set<ConfigurationChangeListener> configurationChangeListeners = ConcurrentHashMap.newKeySet();

	private static boolean configurationAdded = false;

	private static final Type collectionType = new TypeToken<Collection<String>>() {
	}.getType();

	@Inject
	private ConfigurationService configService;

	@Inject
	private BaseService baseService;

	/**
	 * <p>
	 * Returns a list of {@code String} containing the current configuration values for the given key if that key indeed
	 * exists in the database. Returns an empty list if that key does not exists.
	 * </p>
	 *
	 * <p>
	 * Values are served from an internal cache. Clients thus usually do not need to build an own cache for performance
	 * reasons. Clients that nevertheless cache the retrieved value in some way are advised to register themselves as
	 * listeners via {@link #addConfigurationChangeListener(ConfigurationChangeListener)} so that the know when to
	 * refresh their local cache.
	 * </p>
	 *
	 */
	public List<String> getValueList(String key) {
		String value = getConfigValueByKey(key);
		if (value != null) {
			return deSerializeToStringList(value);
		}
		return new ArrayList<>();
	}

	/**
	 * <p>
	 * Returns a {@code boolean} representing the current configuration value for the given key if that key indeed
	 * exists in the database and contains a single configuration value. Returns {@code false} if that key does not
	 * exists. Throws a {@link NonUniqueResultException} if the key is associated with a list of config values.
	 * </p>
	 *
	 * <p>
	 * Values are served from an internal cache. Clients thus usually do not need to build an own cache for performance
	 * reasons. Clients that nevertheless cache the retrieved value in some way are advised to register themselves as
	 * listeners via {@link #addConfigurationChangeListener(ConfigurationChangeListener)} so that the know when to
	 * refresh their local cache.
	 * </p>
	 *
	 */
	public boolean booleanOf(String key) {
		String singleValue = getSingleValue(key).orElse("false");
		return Boolean.parseBoolean(singleValue);
	}

	/**
	 * <p>
	 * Returns an {@code Optional} containing the current configuration value for the given key if that key indeed
	 * exists in the database and contains a single configuration value. Returns an empty {@code Optional} if that key
	 * does not exists. Throws a {@link NonUniqueResultException} if the key is associated with a list of config values.
	 * </p>
	 *
	 * <p>
	 * Values are served from an internal cache. Clients thus usually do not need to build an own cache for performance
	 * reasons. Clients that nevertheless cache the retrieved value in some way are advised to register themselves as
	 * listeners via {@link #addConfigurationChangeListener(ConfigurationChangeListener)} so that the know when to
	 * refresh their local cache.
	 * </p>
	 *
	 */
	public Optional<String> getSingleValue(String key) {
		List<String> valueList = getValueList(key);
		if (valueList.size() > 1) {
			throw new NonUniqueResultException();
		}
		if (valueList.size() == 1) {
			return Optional.of(valueList.get(0));
		}
		return Optional.empty();
	}

	/**
	 * Serializes a list of strings to json using Google's gson lib
	 *
	 * @param values A list of strings
	 * @return ["foo","bar",..]
	 */
	public static String serializeStringListToJson(List<String> values) {
		return new Gson().toJson(values);
	}

	/**
	 * Deserializes a json-string list to a javalist of Strings using Google's gson lib
	 *
	 * @param values A string in JSON syntax: ["foo","bar",..]
	 * @return A Java-List representing the collection
	 */
	public static List<String> deSerializeToStringList(String values) {
		return new Gson().fromJson(values, collectionType);
	}

	/**
	 * Returns all config entries from the database. This method is NOT intended to be used by client code that needs a
	 * specific config value. Use {@link #getSingleValue(String)}, {@link #getValueList(String)}, or
	 * {@link #booleanOf(String)} instead.
	 */
	public List<Config> getAllConfigs() {
		return baseService.findAll(Config.class);
	}

	/**
	 * Returns a single config entry from the database. This method is NOT intended to be used by client code that needs
	 * a specific config value. Use {@link #getSingleValue(String)}, {@link #getValueList(String)}, or
	 * {@link #booleanOf(String)} instead.
	 */
	public Optional<Config> getSingleConfig(String key) {
		return configService.getConfigByKey(key);
	}

	/**
	 * Adds a new config entry to the database. Returns {@code true} if that was successful and {@code false} otherwise.
	 *
	 * The method does NOT update any cached values.
	 */
	public synchronized boolean saveConfig(Config config) {
		boolean saved = configService.saveConfig(config);
		if (saved) {
			configurationAdded = true;
		}
		return saved;
	}

	/**
	 * Deletes a config entry from the database.
	 *
	 * The method does NOT update any cached values.
	 */
	public void deleteConfigKeyValuePair(Config configKeyValuePair) {
		configService.deleteConfigKeyValuePair(configKeyValuePair);
	}

	/**
	 * Creates a new config entry and adds it to the database. Returns {@code true} if that was successful and
	 * {@code false} otherwise. The method expects the value parameter to be a JSON string. Clients may use
	 * {{@link #serializeStringListToJson(List)} to create a proper string from a value list.
	 *
	 * The method does NOT update any cached values.
	 */
	public boolean addNewConfig(String key, String value) {
		verifyJsonString(value);

		Config configKeyValuePair = new Config(key, value);
		return saveConfig(configKeyValuePair);
	}

	/**
	 * Creates a new config entry and adds it to the database. Returns {@code true} if that was successful and
	 * {@code false} otherwise. This method expects the value parameter to be a single config value NOT in JSON-format!
	 *
	 * The method does NOT update any cached values.
	 */
	public boolean addNewConfigSingleValue(String key, String value) {
		return addNewConfig(key, serializeStringListToJson(Arrays.asList(value)));
	}

	private void verifyJsonString(String value) {
		try {
			new Gson().fromJson(value, collectionType);
		} catch (Exception e) {
			throw new VerifyException("Not a valid JSON string: '" + value + "'", e);
		}
	}

	/**
	 * Updates a config entry in the database.
	 *
	 * The method does NOT update any cached values.
	 */
	public void merge(Config configKeyValuePair) {
		baseService.merge(configKeyValuePair);
	}

	/**
	 * Retrieves a config value. First checks cached values and returns values from there if present. Otherwise checks
	 * the database. If a corresponding entry is found in the database, the value is returned and stored in the cache.
	 * Returns {@code null} if no config entry with the given key exists.
	 */
	private String getConfigValueByKey(String key) {
		if (configurationCache.containsKey(key)) {
			return configurationCache.get(key);
		}

		Optional<Config> value = configService.getConfigByKey(key);
		if (value.isPresent()) {
			configurationCache.put(key, value.get().getValue());
			return value.get().getValue();
		}
		return null;
	}

	/**
	 * Returns {@code true} if the cache contains config values that are not consistent with the database. Returns
	 * {@code false} if all values in the cache are equal to the respective entries in the database. Does not check
	 * whether all entries from the database have been loaded into the cache.
	 */
	public boolean isCacheOutdated() {
		if (configurationAdded) {
			return true;
		}

		for (Entry<String, String> entry : configurationCache.entrySet()) {
			String cacheValue = entry.getValue();
			Optional<Config> databaseValue = configService.getConfigByKey(entry.getKey());
			if (!databaseValue.isPresent() || !cacheValue.equals(databaseValue.get().getValue())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes all config values from the cache and informs all listeners that the config has changed.
	 *
	 */
	public synchronized void clearCache() {
		configurationCache.clear();
		configurationAdded = false;
		for (ConfigurationChangeListener listener : configurationChangeListeners) {
			listener.configHasChanged();
		}
	}

	public synchronized void addConfigurationChangeListener(ConfigurationChangeListener listener) {
		configurationChangeListeners.add(listener);
	}

	public synchronized void removeConfigurationChangeListener(ConfigurationChangeListener listener) {
		configurationChangeListeners.remove(listener);
	}

}