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

package com.adobe.cq.commerce.demandware.pim.impl;

import com.adobe.cq.commerce.demandware.pim.EndpointConfig;
import com.adobe.cq.commerce.demandware.pim.EndpointConfigProvider;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


@Component(label = "Demandware PIM Endpoint Config Provider", immediate = true)
@Service(value = EndpointConfigProvider.class)
public class EndpointConfigProviderImpl implements EndpointConfigProvider {
    
    private static final Logger LOG = LoggerFactory.getLogger(EndpointConfigProviderImpl.class);
    
    
    @Reference(referenceInterface = EndpointConfig.class,
            bind = "bindEndpointConfig",
            unbind = "unbindEndpointConfig",
            cardinality = ReferenceCardinality.MANDATORY_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected HashMap<String, EndpointConfig> endpointConfigHashMap;
    
    
    @Override
    public EndpointConfig getEndpointConfigByInstanceId(String instanceId) {
        EndpointConfig config = endpointConfigHashMap.get(instanceId.replace("/", ""));
        if (config == null) {
            LOG.error("Endpoint Config not found for instanceId [{}]", instanceId);
        }
        return config;
    }
    
    protected void bindEndpointConfig(final EndpointConfig config, final Map<String, Object> properties) {
        if (endpointConfigHashMap == null) {
            endpointConfigHashMap = Maps.newHashMap();
        }
        endpointConfigHashMap.put(config.getInstanceId(), config);
    }
    
    protected void unbindEndpointConfig(final EndpointConfig config, final Map<String, Object> properties) {
        endpointConfigHashMap.remove(config.getInstanceId());
    }
}
