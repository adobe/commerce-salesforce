package com.adobe.cq.commerce.demandware.connection;

import com.adobe.cq.commerce.demandware.DemandwareClient;
import com.adobe.cq.commerce.demandware.DemandwareClientProvider;
import com.day.cq.replication.AgentConfig;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component(label = "Demandware Demandware Client Provider", immediate = true)
@Service(value = DemandwareClientProvider.class)
public class DemandwareClientProviderImpl implements DemandwareClientProvider {
	private static final Logger LOG = LoggerFactory.getLogger(DemandwareClientProviderImpl.class);
	private static final String DWRE_SCHEME = "demandware://";
	@Reference(referenceInterface = DemandwareClient.class,
			bind = "bindDemandwareClient",
			unbind = "unbindDemandwareClient",
			cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, // TODO check if works with one
			policy = ReferencePolicy.DYNAMIC)
	protected HashMap<String, DemandwareClient> demandwareClients;

	/**
	 * Returns the first found configured Demandware client.
	 *
	 * @return
	 */
	public DemandwareClient getAnyClient() {
		Optional<DemandwareClient> client = demandwareClients.entrySet()
				.stream()
				.findFirst()
				.map(Map.Entry::getValue);
		if (!client.isPresent()) {
			LOG.error("Failed to get DemandwareClient - no configuration found.");
			return null;
		}
		return client.get();
	}

	/**
	 * Returns the configured Demandware client defined for specific SFCC instance
	 * @param config Replication config containing id of the SFCC instance
	 *
	 * @return DemandwareClient or null if client not found
	 */
	public DemandwareClient getClientForSpecificInstance(AgentConfig config) {
		return getInstanceId(config)
				.map(this::getDemandwareClientByInstanceId)
				.orElseGet(this::getAnyClient);
	}

	/**
	 * Returns the configured Demandware client defined for specific SFCC instance
	 * @param instanceId id of the SFCC instance. should match Replication Agent condig (URI field)
	 *
	 * @return DemandwareClient or null if client not found
	 */
	private DemandwareClient getDemandwareClientByInstanceId(final String instanceId) {
		DemandwareClient demandwareClient = demandwareClients.get(instanceId.replace("/", ""));
		if (demandwareClient == null) {
			LOG.error("DemandwareClient not found for instanceId [{}]", instanceId);
		}
		return demandwareClient;
	}

	private Optional<String> getInstanceId(final AgentConfig config) {
		return Optional.ofNullable(config.getTransportURI())
				.map(uri -> uri.replace(DWRE_SCHEME, StringUtils.EMPTY));
	}

	protected void bindDemandwareClient(final DemandwareClient client, final Map<String, Object> properties) {
		if (demandwareClients == null) {
			demandwareClients = Maps.newHashMap();
		}
		demandwareClients.put(client.getInstanceId(), client);
	}

	protected void unbindDemandwareClient(final DemandwareClient client, final Map<String, Object> properties) {
		demandwareClients.remove(client.getInstanceId());
	}
}
