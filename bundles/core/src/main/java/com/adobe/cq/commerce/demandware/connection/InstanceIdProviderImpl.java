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

import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.InstanceIdProvider;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.replication.AgentConfig;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


@Component(label = "Demandware Component Instance Id Service")
@Service()
public class InstanceIdProviderImpl implements InstanceIdProvider {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceIdProviderImpl.class);
    private static final String DWRE_SCHEME = "demandware://";
    
    @Override
    public String getInstanceId(final Page page) {
        if (page == null) {
            LOG.error("Failed to fetch Demandware instance id. Page is missing.");
            return StringUtils.EMPTY;
        }
        final HierarchyNodeInheritanceValueMap pageProperties = new HierarchyNodeInheritanceValueMap(
                page.getContentResource());
        return pageProperties.getInherited(DemandwareCommerceConstants.PN_DWRE_INSTANCE_ID, StringUtils.EMPTY);
    }
    
    @Override
    public String getInstanceId(final SlingHttpServletRequest request) {
        String path = getReferrerPath(request);
        ResourceResolver resourceResolver = request.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        if (StringUtils.isEmpty(path) || pageManager == null) {
            return StringUtils.EMPTY;
        }
        return getInstanceId(pageManager.getPage(path));
    }
    
    @Override
    public String getInstanceId(final AgentConfig config) {
        return Optional.ofNullable(config.getTransportURI())
                .map(uri -> uri.replace(DWRE_SCHEME, StringUtils.EMPTY))
                .orElse(StringUtils.EMPTY);
    }
    
    private String getReferrerPath(final SlingHttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (StringUtils.isEmpty(referer)) {
            LOG.error("Failed to fetch Demandware instance id. Request referrer header is missing [{}]", referer);
            return StringUtils.EMPTY;
        }
        String host = request.getHeader("Host");
        return StringUtils.substringBetween(referer, host, ".html");
    }
}
