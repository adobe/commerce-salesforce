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
import org.apache.commons.lang3.StringUtils;
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
 * Preview Service Configurations that contain Properties for Preview Service (PreviewServiceImpl)
 */
@Service
@Component(metatype = true, configurationFactory = true, immediate = true,
        policy = ConfigurationPolicy.REQUIRE, label = "Demandware Preview Service Configuration", name = "com.adobe.cq.commerce.demandware.preview.PreviewServiceConfigImpl")
@Properties({
        @Property(name = "webconsole.configurationFactory.nameHint", value = "{service.factoryPid} - {instance.id}")
})
public class PreviewServiceConfigImpl implements PreviewServiceConfig {
    private static final Logger LOG = LoggerFactory.getLogger(PreviewServiceConfigImpl.class);
    
    @Property(label = "Preview endpoint")
    private static final String PREVIEW_PAGE_ENDPOINT = "endpointPage";
    
    @Property(label = "Preview endpoint for category pages")
    private static final String PREVIEW_SEARCH_ENDPOINT = "endpointSearch";
    
    @Property(label = "Preview template path")
    private static final String PREVIEW_TEMPLATE = "template";
    
    @Property(label = "Preview default site")
    private static final String PREVIEW_DEFAULT_SITE = "site";
    
    @Property(label = "Enable preview cache", boolValue = true)
    private static final String PREVIEW_CACHE_ENABLED = "cache.enabled";
    
    private static final int DEFAULT_PREVIEW_CACHE_TIME = 60;
    @Property(label = "Caching time in seconds", intValue = DEFAULT_PREVIEW_CACHE_TIME)
    private static final String PREVIEW_CACHE_TIME = "cache.time";
    
    @Property(label = "Enable storefront protection", boolValue = false)
    private static final String STORFRONT_PROTECTION_ENABLED = "storefront.protected";
    
    @Property(label = "Protected storefront user")
    private static final String STORFRONT_PROTECTION_USER = "storefront.user";
    
    @Property(label = "Protected storefront password")
    private static final String STORFRONT_PROTECTION_PASSWORD = "storefront.password";
    
    @Property(label = "Instance id", description = "Preview Service instance id that corresponds to Replication Agent config")
    private static final String INSTANCE_ID = "instance.id";
    
    private String previewPageEndPoint;
    private String previewSearchEndPoint;
    private String previewTemplate;
    private String previewDefaultSite;
    private String previewCacheTime;
    private Boolean storfrontProtectionEnabled;
    private String storfrontProtectionUser;
    private String StorfrontProtectionPassword;
    private String instanceId;
    
    private PreviewCache cache;
    
    public String getInstanceId() {
        return instanceId;
    }
    
    public String getPreviewPageEndPoint() {
        return previewPageEndPoint;
    }
    
    public String getPreviewSearchEndPoint() {
        return previewSearchEndPoint;
    }
    
    public String getPreviewTemplate() {
        return previewTemplate;
    }
    
    public String getPreviewDefaultSite() {
        return previewDefaultSite;
    }
    
    public String getPreviewCacheTime() {
        return previewCacheTime;
    }
    
    public Boolean getStorfrontProtectionEnabled() {
        return storfrontProtectionEnabled;
    }
    
    public String getStorfrontProtectionUser() {
        return storfrontProtectionUser;
    }
    
    public String getStorfrontProtectionPassword() {
        return StorfrontProtectionPassword;
    }
    
    public PreviewCache getCache() {
        return cache;
    }
    
    @Activate
    protected void activate(Map<String, Object> configuration) {
        previewPageEndPoint = StringUtils.prependIfMissing(PropertiesUtil.toString(configuration.get
                (PREVIEW_PAGE_ENDPOINT), null), "/", "/");
        previewSearchEndPoint = StringUtils.prependIfMissing(
                PropertiesUtil.toString(configuration.get(PREVIEW_SEARCH_ENDPOINT), null), "/", "/");
        previewTemplate = PropertiesUtil.toString(configuration.get(PREVIEW_TEMPLATE), null);
        previewDefaultSite = PropertiesUtil.toString(configuration.get(PREVIEW_DEFAULT_SITE), null);
        previewCacheTime = PropertiesUtil.toString(configuration.get(PREVIEW_CACHE_TIME), "60");
        storfrontProtectionEnabled = PropertiesUtil.toBoolean(configuration.get(PREVIEW_CACHE_TIME), true);
        storfrontProtectionUser = PropertiesUtil.toString(configuration.get(STORFRONT_PROTECTION_USER), null);
        StorfrontProtectionPassword = PropertiesUtil.toString(configuration.get(STORFRONT_PROTECTION_PASSWORD), null);
        instanceId = PropertiesUtil.toString(configuration.get(INSTANCE_ID), null);
        
        if (PropertiesUtil.toBoolean(configuration.get(PREVIEW_CACHE_ENABLED), true)) {
            cache = new PreviewCache(PropertiesUtil.toInteger(configuration.get(PREVIEW_CACHE_TIME),
                    DEFAULT_PREVIEW_CACHE_TIME));
        }
        
        LOG.debug("activating preview service configuration");
    }
    
}
