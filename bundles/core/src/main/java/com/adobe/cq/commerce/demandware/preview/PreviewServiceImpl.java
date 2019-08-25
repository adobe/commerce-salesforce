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

import com.adobe.cq.commerce.demandware.DemandwareClient;
import com.adobe.cq.commerce.demandware.DemandwareClientProvider;
import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.InstanceIdProvider;
import com.adobe.cq.commerce.demandware.PreviewService;
import com.adobe.cq.commerce.demandware.PreviewServiceConfig;
import com.adobe.cq.commerce.demandware.PreviewServiceConfigProvider;
import com.adobe.cq.commerce.demandware.RenderService;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Component previewComponent service to render component previewComponent for Demandware placeholder components. The rendered previewComponent
 * will be cached by a simple cache, avoid unnecessary backend request.
 */
@Component
@Service
public class PreviewServiceImpl implements PreviewService {
    
    private static final Logger LOG = LoggerFactory.getLogger(PreviewServiceImpl.class);
    private static final String ERROR_MSG = "<p>Live Demandware preview failed to render. See log for details.</p>";
    
    @Reference
    DemandwareClientProvider clientProvider;
    
    @Reference
    RenderService renderService;
    
    @Reference
    private InstanceIdProvider instanceIdProvider;
    
    @Reference
    private PreviewServiceConfigProvider previewServiceConfigProvider;
    
    private CredentialsProvider credentialsProvider;
    
    
    @Override
    public String previewComponent(Resource resource, boolean useCache) {
        if (resource != null) {
            return getPreviewContent(resource, useCache, null, DemandwareCommerceConstants.DWRE_RENDERING_SELECTOR);
        } else {
            return null;
        }
    }
    
    @Override
    public String previewComponent(Resource resource, boolean useCache, String... selectors) {
        if (resource != null) {
            return getPreviewContent(resource, useCache, null, selectors);
        } else {
            return null;
        }
    }
    
    @Override
    public String previewCategoryComponent(Resource resource, boolean useCache) {
        if (resource != null) {
            final Page page = resource.getResourceResolver().adaptTo(PageManager.class).getContainingPage(resource);
            // add the category id parameter, using the page property if existing with fall back to page name
            final List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("cgid", page.getProperties().get("dwreCGID", page.getName())));
            return getPreviewContent(resource, useCache, params, DemandwareCommerceConstants.DWRE_RENDERING_SELECTOR);
        } else {
            return null;
        }
    }
    
    private String getPreviewContent(final Resource resource, final boolean useCache,
                                     final List<NameValuePair> params, final String... selectores) {
        String renderedPreview = "";
        PreviewServiceConfig previewServiceConfig = getPreviewServiceConfig(resource);
        final String endPoint = previewServiceConfig.getPreviewPageEndPoint();
        PreviewCache cache = previewServiceConfig.getCache();
        // check for cached preview content
        if (useCache && cache != null) {
            renderedPreview = cache.get(resource);
            if (StringUtils.isNotEmpty(renderedPreview)) {
                LOG.debug("Render previewComponent for {} from cache", resource.getPath());
                return renderedPreview;
            }
        }
        
        // render AEM component for Demandware
        final String renderedComponentContent = renderService.render(resource, null, selectores);
        final Page containingPage = getPage(resource);
        DemandwareClient demandwareClient = getDemandwareClient(containingPage);
        
        final HttpClientBuilder httpClientBuilder = demandwareClient.getHttpClientBuilder();
        
        if (previewServiceConfig.getStorfrontProtectionEnabled()) {
            credentialsProvider = createCredentialsProvider(previewServiceConfig, demandwareClient);
        }
        
        if (credentialsProvider != null) {
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
        final CloseableHttpClient httpClient = httpClientBuilder.build();
        
        CloseableHttpResponse responseObj = null;
        try {
            final RequestBuilder requestBuilder = RequestBuilder.post();
            String previewEndpoint;
            previewEndpoint = StringUtils.replace(endPoint, "{site}", getSite(previewServiceConfig, containingPage));
            previewEndpoint = StringUtils.replace(previewEndpoint, "{locale}", getLanguage(containingPage));
            try {
                requestBuilder.setUri(DemandwareClient.DEFAULT_SCHEMA + demandwareClient.getEndpoint() + previewEndpoint);
            } catch (IllegalArgumentException e) {
                LOG.error("Unable to set preview URI: {}", e.getMessage());
            }
            
            // add parameters, headers, body
            if (params != null) {
                for (NameValuePair param : params) {
                    requestBuilder.addParameter(param);
                }
            }
            requestBuilder.addParameter("template", previewServiceConfig.getPreviewTemplate());
            requestBuilder.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_HTML.getMimeType());
            requestBuilder.setEntity(new StringEntity(renderedComponentContent, ContentType.TEXT_HTML));
            
            final HttpUriRequest requestObj = requestBuilder.build();
            responseObj = httpClient.execute(requestObj);
            if (responseObj != null) {
                final StatusLine statusLine = responseObj.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    renderedPreview = EntityUtils.toString(responseObj.getEntity());
                    LOG.debug("Render previewComponent for {} freshly rendered from DWRE", resource.getPath());
                    if (useCache && cache != null && StringUtils.isNotEmpty(renderedPreview)) {
                        cache.put(resource, renderedPreview);
                    }
                } else {
                    LOG.error("Failed to render live Demandware preview for {}. \n" +
                                    "> Demandware request: {} \n> Demandware response: {}", resource.getPath(),
                            requestObj.getRequestLine().toString(), statusLine.toString());
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to render live Demandware preview for {}", resource.getPath(), e);
        } finally {
            HttpClientUtils.closeQuietly(responseObj);
            HttpClientUtils.closeQuietly(httpClient);
        }
        
        if (StringUtils.isEmpty(renderedPreview)) {
            renderedPreview = ERROR_MSG;
        }
        
        return renderedPreview;
    }
    
    private DemandwareClient getDemandwareClient(Page containingPage) {
        // call Demandware to render preview for component
        String instanceId = instanceIdProvider.getInstanceId(containingPage);
        return clientProvider.getClientForSpecificInstance(instanceId);
    }
    
    private PreviewServiceConfig getPreviewServiceConfig(Resource resource) {
        Page page = getPage(resource);
        String instanceId = instanceIdProvider.getInstanceId(page);
        return previewServiceConfigProvider.getPreviewServiceConfigByInstanceId(instanceId);
    }
    
    private Page getPage(Resource resource) {
        // prepare endpoint path and construct
        final PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        return pageManager.getContainingPage(resource);
    }
    
    /**
     * Get the Demandware site for the current resource, which is configured at page level up to root page. If no
     * site is define we fall beack to the default site.
     *
     * @param page                 the current page
     * @param previewServiceConfig the preview service config
     * @return the Demandware site to be used
     */
    private String getSite(PreviewServiceConfig previewServiceConfig, final Page page) {
        final HierarchyNodeInheritanceValueMap pageProperties = new HierarchyNodeInheritanceValueMap(
                page.getContentResource());
        if (pageProperties != null) {
            return pageProperties.getInherited(DemandwareCommerceConstants.PN_DWRE_SITE, previewServiceConfig.getPreviewDefaultSite());
        }
        return previewServiceConfig.getPreviewDefaultSite();
    }
    
    
    protected final String getLanguage(final Page page) {
        final String locale = StringUtils.replaceChars(page.getLanguage(false).toString(), "_", "-");
        return StringUtils.isNotEmpty(
                new HierarchyNodeInheritanceValueMap(page.getContentResource()).getInherited(JcrConstants.JCR_LANGUAGE,
                        String.class)) ? locale : "default";
    }
    
    /**
     * Setup storefront user credentials.
     */
    protected CredentialsProvider createCredentialsProvider(PreviewServiceConfig previewServiceConfig, DemandwareClient client) {
        // set default user/pass
        if (previewServiceConfig.getStorfrontProtectionUser() != null && previewServiceConfig.getStorfrontProtectionPassword() != null) {
            final CredentialsProvider credsProvider = new BasicCredentialsProvider();
            
            credsProvider.setCredentials(new AuthScope(client.getEndpoint(), AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(previewServiceConfig.getStorfrontProtectionUser(),
                            previewServiceConfig.getStorfrontProtectionPassword()));
            return credsProvider;
        }
        return null;
    }
    
}
