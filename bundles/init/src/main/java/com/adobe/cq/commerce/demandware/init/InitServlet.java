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

package com.adobe.cq.commerce.demandware.init;

import java.io.IOException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.demandware.DemandwareClient;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineImpl;

/**
 * Simple init servlet to preload static resources and velocity templates to demandware.
 */

@SlingServlet(resourceTypes = "commerce/demandware/components/init", extensions = "html", methods = "POST", label = "Demandware Init Servlet")
public class InitServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(InitServlet.class);

    @Reference
    DemandwareClient demandwareClient;

    @Reference
    private Replicator replicator;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws
        ServletException, IOException {
        LOG.debug("Init Demandware Sandbox");
        ValueMap config = request.getResource().getValueMap();

        // get assets and push to webdav
        final String webDAVEndpoint = config.get("assetWebDAV", String.class);
        final String[] assetURIs = config.get("assetUris", String[].class);
        if (StringUtils.isNotEmpty(webDAVEndpoint) && ArrayUtils.isNotEmpty(assetURIs)) {
            for (String assetURI : assetURIs) {
                upload(request, webDAVEndpoint, assetURI);
            }
        }

        // replicate templates
        if (config.containsKey("templatePaths")) {
            String[] templatePaths = config.get("templatePaths", String[].class);
            if (ArrayUtils.isNotEmpty(templatePaths)) {
                Session session = request.getResourceResolver().adaptTo(Session.class);
                try {
                    for (String templatePath : templatePaths) {
                        replicator.replicate(session, ReplicationActionType.ACTIVATE, templatePath);
                    }
                } catch (ReplicationException e) {
                    LOG.error("Error during template activation", e);
                }
            }
        }

        response.sendRedirect(request.getResource().getParent().getPath() + "." +
            request.getRequestPathInfo().getExtension());
    }

    private void upload(SlingHttpServletRequest slingRequest, String endPoint, String assetURI) {
        final RequestBuilder requestBuilder = RequestBuilder.get();
        requestBuilder.setUri(assetURI);
        final Cookie token = slingRequest.getCookie("login-token");
        requestBuilder.addHeader(new BasicHeader("Cookie", token.getName() + "=" + token.getValue()));
        final HttpHost localHost = new HttpHost("localhost", 4502);
        final HttpClient httpClient = demandwareClient.getHttpClientBuilder().build();
        try {
            // get the from AEM content
            final HttpResponse response = httpClient.execute(localHost, requestBuilder.build());

            if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                final HttpEntity entity = response.getEntity();
                HttpClientBuilder httpClientBuilder = demandwareClient.getHttpClientBuilder();
                httpClientBuilder.setDefaultCredentialsProvider(
                    getWebDAVCredentials(slingRequest.getResource().getValueMap()));
                // upload to webDAV
                final Sardine webDav = new SardineImpl(httpClientBuilder);
                getOrCreateFolders(webDav, endPoint, StringUtils.substringBeforeLast(assetURI, "."));
                webDav.put(endPoint + assetURI, EntityUtils.toByteArray(entity),
                    entity.getContentType().getValue());
            } else {
                LOG.error("Could not get local AEM content for {}", assetURI);
            }
        } catch (IOException e) {
            LOG.error("WebDAV upload failed", e);
        }
    }

    /**
     * Get or creates the folder structure for a given asset path.
     *
     * @param sardine     the WebDAV client
     * @param endPointUrl the endpoint url
     * @param path        the asset path
     * @throws IOException if an error occurs
     */
    private void getOrCreateFolders(Sardine sardine, String endPointUrl, String path) throws
        IOException {
        final String folderPath = StringUtils.substringBeforeLast(path, "/");
        final String[] folders = StringUtils.split(folderPath, "/");
        for (int i = 0; i < folders.length; i++) {
            endPointUrl = StringUtils.appendIfMissing(endPointUrl, "/", "/") + folders[i];
            if (sardine.exists(endPointUrl)) {
                continue;
            }
            sardine.createDirectory(endPointUrl);
        }
    }

    /**
     * Set up WebDAV credentials.
     *
     * @param config value map with config parameter
     * @return the configured credentials provider
     */
    private CredentialsProvider getWebDAVCredentials(ValueMap config) {
        final String user = config.get("assetWebDAVUser", String.class);
        if (StringUtils.isNotEmpty(user)) {
            final CredentialsProvider credsProvider = new BasicCredentialsProvider();
            String pass = config.get("assetWebDAVPassword", String.class);
            if (pass == null) {
                pass = "";
            }
            credsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(user, pass));
            return credsProvider;
        }
        return null;
    }

    private String getToken(SlingHttpServletRequest request) {
        return request.getCookie("login-token").getValue();
    }
}
