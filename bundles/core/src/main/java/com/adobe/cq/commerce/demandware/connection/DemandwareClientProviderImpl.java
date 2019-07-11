/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

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
	public static final String INSTANCE_ID_DEFAULT = "default";
	@Reference(referenceInterface = DemandwareClient.class,
			bind = "bindDemandwareClient",
			unbind = "unbindDemandwareClient",
			cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, // TODO check if works with one
			policy = ReferencePolicy.DYNAMIC)
	protected HashMap<String, DemandwareClient> demandwareClients;

	/**
	 * Returns DemandwareClient service, configured with "default" instance id
	 * or, if not present, first found DemandwareClient service.
	 *
	 * @return
	 */
	public DemandwareClient getDefaultClient() {
		DemandwareClient client = getDemandwareClientByInstanceId(INSTANCE_ID_DEFAULT);
		if (client != null) {
			return client;
		}

		LOG.debug("No default DemandwareClient configured: return first found service.");
		Optional<DemandwareClient> clientOpt = demandwareClients.entrySet()
				.stream()
				.findFirst()
				.map(Map.Entry::getValue);
		if (clientOpt.isPresent()) {
			return clientOpt.get();
		}

		LOG.error("Failed to get DemandwareClient - no configuration found.");
		return null;
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
				.orElse(null);
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
