package de.uni_due.s3.jack3.business.microservices;

import static java.util.Map.entry;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.StatusChangeEvent;
import com.netflix.discovery.shared.Application;

import de.uni_due.s3.jack3.business.AbstractBusiness;
import de.uni_due.s3.jack3.business.ConfigurationBusiness;

@ApplicationScoped
public class EurekaBusiness extends AbstractBusiness {

	public static final String EUREKA_SERVER_URLS_KEY = "EurekaServerURLs";

	public static final Map<String,String> DEFAULT_CONFIG = Map.ofEntries(
		entry("eureka.preferSameZone","true"),
		entry("eureka.registration.enabled","false"),
		entry("eureka.serviceUrl.default","http://10.168.68.74:8761/eureka"),
		entry("eureka.shouldUseDns","false"),
		entry("eureka.serviceUrlPollIntervalMs","42000"),
		entry("evaluator-service.ribbon.NIWSServerListClassName","com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList"),
		entry("evaluator-service.ribbon.ServerListRefreshInterval","15000"),
		entry("evaluator-service.ribbon.DeploymentContextBasedVipAddresses","evaluator-service-v-4-3"));

	@Inject
	ConfigurationBusiness configurationBusiness;

	private AtomicReference<EurekaClient> eurekaClient = new AtomicReference<>();

	private AtomicInteger statusHash = new AtomicInteger();

	@PostConstruct
	private void initialize() {
		// We first initialize the Archaius configuration for the Eureka client.
		System.setProperty("archaius.dynamicProperty.disableDefaultConfig","false");
		for (final Entry<String,String> entry : DEFAULT_CONFIG.entrySet()) {
			ConfigurationManager.getConfigInstance().setProperty(entry.getKey(),entry.getValue());
		}

		// We push the server list from our configuration to Eureka's.
		configurationBusiness.addConfigurationChangeListener(this::pushServerListToEurekaConfig);
		pushServerListToEurekaConfig();

		// Finally we initialize the Eureka client.
		EurekaInstanceConfig instanceConfig = new MyDataCenterInstanceConfig();
		InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
		ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);
		DefaultEurekaClientConfig configuration = new DefaultEurekaClientConfig();

		EurekaClient eurekaClient = new DiscoveryClient(applicationInfoManager, configuration);
		eurekaClient.registerEventListener(this::eurekaEventOccurred);
		this.eurekaClient.set(eurekaClient);
	}

	private void pushServerListToEurekaConfig() {
		final List<String> serverList = configurationBusiness.getValueList(EUREKA_SERVER_URLS_KEY);
		if (!serverList.isEmpty()) {
			getLogger().infof("Pushing the following server list to eureka: %s",serverList);
			final String property = serverList.stream().collect(Collectors.joining(","));
			ConfigurationManager.getConfigInstance().setProperty("eureka.serviceUrl.default",property);
		}
	}

	void eurekaEventOccurred(final EurekaEvent event) {
		if (event instanceof StatusChangeEvent) {
			final StatusChangeEvent sce = ((StatusChangeEvent) event);
			getLogger().infof("Eureka client changed its status from {} to {}.",sce.getPreviousStatus(),sce.getStatus());
		}
		if (event instanceof CacheRefreshedEvent) {
			final String status = getEurekaCacheStatusAsString();
			final int hash = status.hashCode();
			if (statusHash.getAndSet(hash) != hash) {
				getLogger().info(status);
			}
		}
	}

	private String getEurekaCacheStatusAsString() {
		final StringBuilder sb = new StringBuilder("Eureka cache status:");
		final EurekaClient eurekaClient = this.eurekaClient.get();
		for (Application app : eurekaClient.getApplications().getRegisteredApplications()) {
			sb.append("\n * ").append(app.getName());
			for (InstanceInfo info : app.getInstancesAsIsFromEureka()) {
				sb.append("\n   - ")
					.append(info.getInstanceId())
					.append(" (").append(info.getStatus()).append(")");
			}
		}
		return sb.toString();
	}

	@PreDestroy
	private void destroy() {
		eurekaClient.getAndSet(null).shutdown();
	}

	public void touch() {
		// This method is intentionally empty.
		// It allows to ensure this business' CDI proxy is initialized.
	}
}
