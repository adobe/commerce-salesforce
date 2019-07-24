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
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Optional;


@Component(label = "Demandware Component Instance Id Service")
@Service()
public class InstanceIdProviderImpl implements InstanceIdProvider {
    private static final String DWRE_SCHEME = "demandware://";
    
    @Override
    public String getInstanceId(final Page page) {
        String previewInstanceId = "";
        final HierarchyNodeInheritanceValueMap pageProperties = new HierarchyNodeInheritanceValueMap(
                page.getContentResource());
        if (pageProperties != null) {
            return pageProperties.getInherited(DemandwareCommerceConstants.PN_DWRE_INSTANCE_ID, previewInstanceId);
        }
        return previewInstanceId != null ? previewInstanceId : "";
    }
    
    @Override
    public String getInstanceId(SlingHttpServletRequest request) {
        String previewInstanceId = getInstanceId(getPage(request));
        return previewInstanceId != null ? previewInstanceId : "";
    }
    
    @Override
    public String getInstanceId(final AgentConfig config) {
        return Optional.ofNullable(config.getTransportURI())
                .map(uri -> uri.replace(DWRE_SCHEME, org.apache.commons.lang3.StringUtils.EMPTY))
                .orElse(org.apache.commons.lang3.StringUtils.EMPTY);
    }
    
    private Page getPage(SlingHttpServletRequest request) {
        String path = getRequestedPath(request);
        ResourceResolver resourceResolver = request.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        return pageManager.getPage(path);
    }
    
    private String getRequestedPath(SlingHttpServletRequest request) {
        String referer = request.getHeader("Referer");
        String host = request.getHeader("Host");
        return StringUtils.substringBetween(referer, host, ".html");
    }
}
