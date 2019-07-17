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

package com.adobe.cq.commerce.demandware.replication.transport;

import com.adobe.cq.commerce.demandware.DemandwareClient;
import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.replication.TransportHandlerPlugin;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationLog;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineException;
import com.github.sardine.impl.SardineImpl;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Dictionary;
import java.util.Optional;

/**
 * <code>TransportHandlerPlugin</code> to send static files to WebDAV.
 */
@Component(label = "Demandware TransportHandler Plugin for WebDAV", immediate = true, metatype = true, policy = ConfigurationPolicy.REQUIRE)
@Service(value = TransportHandlerPlugin.class)
@Properties({@Property(name = TransportHandlerPlugin.PN_TASK, value = "WebDAVTransportPlugin", propertyPrivate = true),
    @Property(name = Constants.SERVICE_RANKING, intValue = 30)})
public class WebDAVTransportPlugin extends AbstractTransportHandlerPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(WebDAVTransportPlugin.class);

    @Property(label = "WebDAV instance endpoint", description = "Optional: WebDAV server hostname or ip if different from instance endpoint")
    private static final String WEBDAV_ENDPOINT = "webdav.endpoint";

    @Property(label = "WebDAV user ")
    private static final String WEBDAV_USER = "webdav.user";

    @Property(label = "WebDAV user password")
    private static final String WEBDAV_PASSWORD = "webdav.password";

    private String webDavEndpoint;
    private String webDavUser;
    private String webDavUserPassword;

    @Override
    String getApiType() {
        return DemandwareCommerceConstants.TYPE_WEBDAV;
    }

    @Override
    String getContentType() {
        return "static-asset";
    }

    @Override
    public boolean deliver(JSONObject delivery, AgentConfig config, ReplicationLog log, ReplicationAction action)
        throws ReplicationException {

        // construct the WebDAV request
        String path = null;
        final String endpoint = StringUtils.isNotEmpty(webDavEndpoint)
                ? webDavEndpoint
                : getDemandwareClientEndpoint(config);
        final StringBuilder transportUriBuilder = new StringBuilder();
        transportUriBuilder.append(DemandwareClient.DEFAULT_SCHEMA);
        transportUriBuilder.append(endpoint);
        try {
            transportUriBuilder.append(
                constructEndpointURL(delivery.getString(DemandwareCommerceConstants.ATTR_WEBDAV_SHARE), delivery));
            path = delivery.getString(DemandwareCommerceConstants.ATTR_PATH);
            path = StringUtils.substringBeforeLast(path, "/") + "/" + URLEncoder.encode(StringUtils
                .substringAfterLast(path, "/"), "UTF-8").replaceAll("\\+", "%20");
        } catch (JSONException e) {
            LOG.error("Can not create endpoint URI", e);
            throw new ReplicationException("Can not create endpoint URI", e);
        } catch (UnsupportedEncodingException e) {
            LOG.error("Can not encode URI", e);
            throw new ReplicationException("Can not encode URI", e);
        }
        log.info("Deliver %s to %s (%s)", path, transportUriBuilder.toString(), action.getType().getName());

        final HttpClientBuilder httpClientBuilder = getHttpClientBuilder(config, log);
        if (action.getType() == ReplicationActionType.ACTIVATE) {
            // get asset / file to be delivered
            byte[] data;
            String contentType;
            try {
                JSONObject assetData = delivery.getJSONObject(DemandwareCommerceConstants.ATTR_PAYLOAD);
                if (assetData.has(DemandwareCommerceConstants.ATTR_BASE64)) {
                    data = Base64.decodeBase64(assetData.getString(DemandwareCommerceConstants.ATTR_DATA));
                } else {
                    data = assetData.getString(DemandwareCommerceConstants.ATTR_DATA).getBytes();
                }
                contentType = assetData.getString(DemandwareCommerceConstants.ATTR_MIMETYPE);
            } catch (JSONException e) {
                LOG.error("Can not create asset data", e);
                throw new ReplicationException("Can not create asset data", e);
            }

            // send asset to WebDAV share
            if (data != null && StringUtils.isNotEmpty(contentType)) {

                deliverWebDAV(httpClientBuilder, transportUriBuilder.toString(), path, data, contentType, log);
                return true;
            } else {
                log.warn("No asset data to send !?");
                return false;
            }
        } else {
            deleteWevDAV(httpClientBuilder, transportUriBuilder.toString(), action.getPath(), log);
            return true;
        }
    }

    /**
     * Upload asset data to webdav share.
     *
     * @param httpClientBuilder the HTTP client builder to be used
     * @param transportUri      the endpoint including protocol and hostname
     * @param path              the path to the resource
     * @param data              the data to be uploaded
     * @param contentType       the content type
     * @param log               the replication log
     * @throws ReplicationException if an error occurs
     */
    private void deliverWebDAV(HttpClientBuilder httpClientBuilder, String transportUri, String path, byte[] data,
                               String contentType, ReplicationLog log)
        throws ReplicationException {
        Sardine sardine = new SardineImpl(httpClientBuilder);
        try {
            // establish the folders first
            getOrCreateFolders(sardine, transportUri, path, log);

            // sent put request
            log.debug("Upload %s ...", path);
            sardine.put(transportUri + path, data, contentType);
            log.debug("Upload done.");
        } catch (IOException e) {
            throw new ReplicationException(e);
        } finally {
            if (sardine != null) {
                try {
                    sardine.shutdown();
                } catch (IOException e) {
                    log.warn("Error shut down WebDAV client %s", e.getMessage());
                }
            }
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
    private void getOrCreateFolders(Sardine sardine, String endPointUrl, String path, ReplicationLog log) throws
        IOException {
        final String folderPath = StringUtils.substringBeforeLast(path, "/");
        final String[] folders = StringUtils.split(folderPath, "/");
        for (String folder : folders) {
            endPointUrl = StringUtils.appendIfMissing(endPointUrl, "/", "/") + folder;
            if (sardine.exists(endPointUrl)) {
                continue;
            }
            log.debug("Create missing WebDAV folder %s", endPointUrl);
            sardine.createDirectory(endPointUrl);
        }
    }

    /**
     * Delete a WebDAV resource.
     *
     * @param httpClientBuilder the Http client builder
     * @param transportUri      the endpoint including protocol and hostname
     * @param path              the path to the resource
     * @param log               the replication log
     * @throws ReplicationException if an error occurs
     */
    private void deleteWevDAV(HttpClientBuilder httpClientBuilder, String transportUri, String
        path, ReplicationLog log) throws ReplicationException {
        Sardine sardine = new SardineImpl(httpClientBuilder);
        try {
            log.info("Delete %s", transportUri + path);
            sardine.delete(transportUri + path);
        } catch (SardineException e) {
            if (e.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
                throw new ReplicationException(e);
            }
        } catch (IOException e) {
            throw new ReplicationException(e);
        } finally {
            if (sardine != null) {
                try {
                    sardine.shutdown();
                } catch (IOException e) {
                    log.warn("Error shut down WebDAV client %s", e.getMessage());
                }
            }
        }
    }

    final String getDemandwareClientEndpoint(final AgentConfig config) {
        final Optional<DemandwareClient> demandwareClient = clientProvider.getClientForSpecificInstance(config);
        if (!demandwareClient.isPresent()) {
            LOG.error("Failed to get DemandwareClient endpoint - no configuration found.");
            return null;
        }
        return demandwareClient.get().getEndpoint();
    }

    /**
     * Setup transport user and other credentials.
     */
    @Override
    protected CredentialsProvider createCredentialsProvider(AgentConfig config, ReplicationLog log) {
        // set default user/pass
        if (StringUtils.isNotEmpty(webDavUser)) {
            log.debug("WebDAV auth user: %s", webDavUser);
            final CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(webDavUser, webDavUserPassword));
            return credsProvider;
        }
        return null;
    }

    /* OSGI stuff */

    @Activate
    protected void activate(final ComponentContext ctx) {
        final Dictionary<?, ?> config = ctx.getProperties();
        webDavEndpoint = PropertiesUtil.toString(config.get(WEBDAV_ENDPOINT), "");
        webDavUser = PropertiesUtil.toString(config.get(WEBDAV_USER), "");
        webDavUserPassword = PropertiesUtil.toString(config.get(WEBDAV_PASSWORD), "");
    }
}
