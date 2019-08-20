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
import com.adobe.cq.commerce.demandware.DemandwareClientException;
import com.adobe.cq.commerce.demandware.DemandwareClientProvider;
import com.adobe.cq.commerce.demandware.InstanceIdProvider;
import com.day.cq.replication.AgentConfig;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

@Component(label = "Demandware Demandware Client Provider", immediate = true)
@Service(value = DemandwareClientProvider.class)
public class DemandwareClientProviderImpl implements DemandwareClientProvider {
	private static final Logger LOG = LoggerFactory.getLogger(DemandwareClientProviderImpl.class);
	@Reference(referenceInterface = DemandwareClient.class,
			bind = "bindDemandwareClient",
			unbind = "unbindDemandwareClient",
			cardinality = ReferenceCardinality.MANDATORY_MULTIPLE,
			policy = ReferencePolicy.DYNAMIC)
	protected HashMap<String, DemandwareClient> demandwareClients;

	@Reference
	private InstanceIdProvider instanceIdProvider;

	/**
	 * Returns the configured Demandware client defined for specific SFCC instance
	 * @param instanceId unique id of the SFCC instance
	 *
	 * @return DemandwareClient
	 * @throws DemandwareClientException if DemandwareClient not found
	 */
	@Override
	public DemandwareClient getClientForSpecificInstance(final String instanceId) {
		DemandwareClient demandwareClient = demandwareClients.get(instanceId);
		if (demandwareClient == null) {
			String errorMessage = "DemandwareClient not found for instanceId: " + instanceId;
			LOG.error(errorMessage);
			throw new DemandwareClientException(errorMessage);
		}
		return demandwareClient;
	}
	
	@Override
	public HashMap<String, DemandwareClient> getDemandwareClients (){
		return demandwareClients;
	}

	/**
	 * Returns the configured Demandware client defined for specific SFCC instance
	 * @param config Replication config containing id of the SFCC instance
	 *
	 * @return DemandwareClient
	 * @throws DemandwareClientException if DemandwareClient not found
	 */
	@Override
	public DemandwareClient getClientForSpecificInstance(final AgentConfig config) {
		final String instanceId = instanceIdProvider.getInstanceId(config);
		return getClientForSpecificInstance(instanceId);
	}

	protected void bindDemandwareClient(final DemandwareClient client) {
		if (demandwareClients == null) {
			demandwareClients = Maps.newHashMap();
		}
		demandwareClients.put(client.getInstanceId(), client);
	}

	protected void unbindDemandwareClient(final DemandwareClient client) {
		demandwareClients.remove(client.getInstanceId());
	}
}
