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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Endpoint Configurations that contains Properties for Product Import
 */
@Service
@Component(metatype = true, configurationFactory = true, immediate = true,
        policy = ConfigurationPolicy.REQUIRE, label = "Endpoint Configuration for Demandware Product Import",
        name = "com.adobe.cq.commerce.demandware.pim.impl.EndpointConfigImpl")
@Properties({
@Property(name = "webconsole.configurationFactory.nameHint", value = "{service.factoryPid} - {instance.id}")
})
public class EndpointConfigImpl implements EndpointConfig {
    private static final Logger LOG = LoggerFactory.getLogger(com.adobe.cq.commerce.demandware.pim.impl.EndpointConfigImpl.class);
    
    @Property(label = "DownloadEndpoint")
    private static final String DOWNLOAD_ENDPOINT = "downloadEndpoint";
    
    @Property(label = "Instance id", description = "Instance id to identify Demandware Client")
    private static final String INSTANCE_ID = "instance.id";
    
    private String downloadEndpoint;
    private String instanceId;
    
    public String getInstanceId() {
        return instanceId;
    }
    
    public String getDownloadEndpoint() {
        return downloadEndpoint;
    }
    
    
    @Activate
    protected void activate(Map<String, Object> configuration) {
        downloadEndpoint = PropertiesUtil.toString(configuration.get(DOWNLOAD_ENDPOINT),"");
        instanceId = PropertiesUtil.toString(configuration.get(INSTANCE_ID), null);
        LOG.debug("Activating endpoint configuration for product import.");
    }
    
}
