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

package com.adobe.cq.commerce.demandware.preview;

import com.adobe.cq.commerce.demandware.PreviewServiceConfig;
import com.adobe.cq.commerce.demandware.PreviewServiceConfigProvider;
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

@Component(label = "Preview Service Config Provider", immediate = true)
@Service(value = PreviewServiceConfigProvider.class)
public class PreviewServiceConfigProviderImpl implements PreviewServiceConfigProvider {
    
    private static final Logger LOG = LoggerFactory.getLogger(PreviewServiceConfigProviderImpl.class);
    
    public static final String INSTANCE_ID_DEFAULT = "default";
    private static final String DWRE_SCHEME = "demandware://";
    
    @Reference(referenceInterface = com.adobe.cq.commerce.demandware.PreviewServiceConfig.class,
            bind = "bindPreviewServiceConfig",
            unbind = "unbindPreviewServiceConfig",
            cardinality = ReferenceCardinality.MANDATORY_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected HashMap<String, com.adobe.cq.commerce.demandware.PreviewServiceConfig> previewServiceConfigHashMap;
    
    /**
     * returns Preview Service Config, configured with "default" instance id
     * or, if not present, first found Preview Service Config.
     *
     * @return
     */
    public PreviewServiceConfig getDefaultPreviewServiceConfig() {
        PreviewServiceConfig config = getPreviewServiceConfigByInstanceId(INSTANCE_ID_DEFAULT);
        if (config != null) {
            return config;
        }
        
        LOG.debug("No default PreviewService configured: return first found service.");
        Optional<PreviewServiceConfig> previewServiceConfig = previewServiceConfigHashMap.entrySet()
                .stream()
                .findFirst()
                .map(Map.Entry::getValue);
        if (previewServiceConfig.isPresent()) {
            return previewServiceConfig.get();
        }
        
        LOG.error("Failed to get PreviewService - no configuration found.");
        return null;
    }
    
    /**
     * Returns the configured Demandware client defined for specific SFCC instance
     *
     * @param instanceId id of the SFCC instance. should match Replication Agent condig (URI field)
     * @return DemandwareClient or null if client not found
     */
    public PreviewServiceConfig getPreviewServiceConfigByInstanceId(final String instanceId) {
        PreviewServiceConfig config = previewServiceConfigHashMap.get(instanceId.replace("/", ""));
        if (config == null) {
            LOG.error("Preview Service Config not found for instanceId [{}]", instanceId);
        }
        return config;
    }
    
    /**
     * Returns the configured Preview Service Config defined for specific SFCC instance
     *
     * @param config Replication config containing id of the SFCC instance
     * @return PreviewServiceConfig or null if client not found
     */
    public PreviewServiceConfig getClientForSpecificInstance(AgentConfig config) {
        return getInstanceId(config)
                .map(this::getPreviewServiceConfigByInstanceId)
                .orElse(null);
    }
    
    
    private Optional<String> getInstanceId(final AgentConfig config) {
        return Optional.ofNullable(config.getTransportURI())
                .map(uri -> uri.replace(DWRE_SCHEME, StringUtils.EMPTY));
    }
    
    
    protected void bindPreviewServiceConfig(final PreviewServiceConfig config, final Map<String, Object> properties) {
        if (previewServiceConfigHashMap == null) {
            previewServiceConfigHashMap = Maps.newHashMap();
        }
        previewServiceConfigHashMap.put(config.getInstanceId(), config);
    }
    
    protected void unbindPreviewServiceConfig(final PreviewServiceConfig config, final Map<String, Object> properties) {
        previewServiceConfigHashMap.remove(config.getInstanceId());
    }
    
    
}
