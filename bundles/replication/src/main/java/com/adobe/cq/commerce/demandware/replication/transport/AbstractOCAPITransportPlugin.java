/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe Systems Incorporated and others
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
import com.adobe.cq.commerce.demandware.DemandwareClientException;
import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.InstanceIdProvider;
import com.adobe.cq.commerce.demandware.replication.DemandwareReplicationException;
import com.adobe.cq.commerce.demandware.replication.DemandwareReplicationLoginService;
import com.adobe.granite.auth.oauth.AccessTokenProvider;
import com.day.cq.replication.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.osgi.service.component.ComponentContext;

import javax.jcr.Session;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Abstract {@code TransportHandlerPlugin} class used as base for all Demandware OCAPI transport handlers.
 */
public abstract class AbstractOCAPITransportPlugin extends AbstractTransportHandlerPlugin {
    static final String ACCESS_TOKEN_PROPERTY = "accessTokenProvider";
    static final String BEARER_AUTHENTICATION_FORMAT = "Bearer %s";

    protected static final String DEFAULT_OCAPI_VERSION = "v17_6";
    protected static final String DEFAULT_OCAPI_PATH = "/s/-/dw/data";
    protected static final String DW_HTTP_METHOD_OVERRIDE_HEADER = "x-dw-http-method-override";

    protected static final String ACCESS_TOKEN_PROVIDER = "accessTokenProviderId";
    protected static final String OCAPI_VERSION = "ocapi.version";
    protected static final String OCAPI_PATH = "ocapi.path";
    protected static final String OCAPI_EP = "ocapi.ep";

    abstract protected DemandwareReplicationLoginService getReplicationLoginService();

    abstract protected InstanceIdProvider getInstanceIdProvider();

    private Map<String, Comparable<Object>> accessTokenProvidersProps =
            new ConcurrentSkipListMap<>(Collections.reverseOrder());
    private Map<Comparable<Object>, AccessTokenProvider> accessTokenProviders =
            new ConcurrentSkipListMap<>(Collections.reverseOrder());

    private String accessTokenProviderClientId;
    private String ocapiVersion;
    private String ocapiPath;

    @Override
    String getApiType() {
        return DemandwareCommerceConstants.TYPE_OCAPI;
    }

    /**
     * Deliver data using OC data API using a GET &gt; PATCH / PUT request approach. If the data object exists it
     * will be updated using PATCH otherwise created using PUT. Deactivation will be send using DELETE requests.
     * @param delivery the JSON data containing the meta information and payload data
     * @param config the replication agent config
     * @param log the replication agent log
     * @param action the replication action
     * @return <code>true</code> for successful delivery, otherwise <code>false</code>
     * @throws ReplicationException in case of an error
     */
    @Override
    public boolean deliver(JSONObject delivery, AgentConfig config, ReplicationLog log, ReplicationAction action)
            throws ReplicationException {
        final String id = delivery.optString(DemandwareCommerceConstants.ATTR_ID,
                StringUtils.substringAfterLast(action.getPath(), "/"));
        final String dwInstanceId = getInstanceIdProvider().getInstanceId(config);

        // step 1: check if the content asset already exists
        RequestBuilder requestBuilder;
        requestBuilder = getRequestBuilder("GET", delivery, dwInstanceId);
        log.info("Deliver %s to %s (%s)", id, requestBuilder.build().getRequestLine().toString(), action.getType().getName());

        log.info("Check if %s %s already exists", getContentType(), id);
        final HttpClient httpClient = getHttpClient(config, log);
        HttpResponse response = null;
        String eTagHeaderValue = null;
        response = executeRequest(httpClient, requestBuilder.build(), log);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            eTagHeaderValue = getETagHeader(response);
            log.info("%s %s already exists, will be updated", getContentType(), id);
        } else {
            log.info("%s %s does not exist", getContentType(), id);
        }
        HttpClientUtils.closeQuietly(response);

        if (action.getType() == ReplicationActionType.ACTIVATE) {
            // step 2: construct JSON object to send
            log.debug("deserialize content for delivery");
            StringEntity requestInput;
            try {
                final JSONObject payload = delivery.getJSONObject(DemandwareCommerceConstants.ATTR_PAYLOAD);
                requestInput = new StringEntity(payload.toString(), ContentType.APPLICATION_JSON);
                log.debug("set %d bytes of post body.", requestInput.getContentLength());
            } catch (JSONException e) {
                throw new ReplicationException("Can not create request body content", e);
            }

            // step 3: deliver the content asset using PUT or PATCH request
            if (requestInput.getContentLength() > 0) {
                if (StringUtils.isEmpty(eTagHeaderValue)) {
                    requestBuilder = getRequestBuilder("POST", delivery, dwInstanceId);
                    requestBuilder.addHeader(DW_HTTP_METHOD_OVERRIDE_HEADER, "PUT");
                } else {
                    requestBuilder = getRequestBuilder("PATCH", delivery, dwInstanceId);
                    requestBuilder.addHeader(HttpHeaders.IF_MATCH, eTagHeaderValue);
                }
                log.debug("Send %s %s using %s", getContentType(), id, requestBuilder.getMethod());

                requestBuilder.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
                requestBuilder.addHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
                requestBuilder.setEntity(requestInput);
                response = executeRequest(httpClient, requestBuilder.build(), log);

                HttpClientUtils.closeQuietly(response);
                HttpClientUtils.closeQuietly(httpClient);
                return isRequestSuccessful(response);
            } else {
                log.warn("No request body to send !?");
                return false;
            }
        } else {
            // check we there is an eTag header from the get, if not there is nothing to delete
            if (StringUtils.isNotEmpty(eTagHeaderValue)) {
                requestBuilder = getRequestBuilder("DELETE", delivery, dwInstanceId);
                log.info("Delete %s %s", getContentType(), id);
                response = executeRequest(httpClient, requestBuilder.build(), log);
                HttpClientUtils.closeQuietly(response);
                HttpClientUtils.closeQuietly(httpClient);
                return isRequestSuccessful(response);
            }

            HttpClientUtils.closeQuietly(httpClient);
            return true;
        }
    }

    /**
     * Construct the OCAPI request based on request method and api information of JSON object.
     *
     * @param method   the HTTP request method
     * @param delivery the JSON data
     * @param dwInstanceId
     * @return the created request builder
     * @throws ReplicationException in case request builder can not be created
     */
    protected RequestBuilder getRequestBuilder(final String method, final JSONObject delivery,
                                               final String dwInstanceId) throws ReplicationException {
        if (StringUtils.isEmpty(method)) {
            throw new ReplicationException("No request method provided");
        }
        try {
            // construct the OCAPI request
            final StringBuilder transportUriBuilder = new StringBuilder();
            final String endpoint = getClientProvider().getClientForSpecificInstance(dwInstanceId).getEndpoint();
            transportUriBuilder.append(DemandwareClient.DEFAULT_SCHEMA).append(endpoint);
            transportUriBuilder.append(getOCApiPath()).append(getOCApiVersion());
            transportUriBuilder.append(
                    constructEndpointURL(delivery.getString(DemandwareCommerceConstants.ATTR_API_ENDPOINT), delivery));

            final RequestBuilder requestBuilder = RequestBuilder.create(method);
            requestBuilder.setUri(transportUriBuilder.toString());
            return requestBuilder;
        } catch (JSONException | DemandwareClientException e) {
            throw new ReplicationException("Can not create endpoint URI: " + e.getMessage(), e);
        }
    }

    /**
     * Little helper to executeRequest the HTTP request and log the request and response headers and parameters.
     *
     * @param httpClient the {@code HttpClient}
     * @param request    the request object
     * @param log        the replication log
     * @return the response object
     */
    protected HttpResponse executeRequest(HttpClient httpClient, HttpUriRequest request, ReplicationLog log) {
        HttpResponse response;
        try {
            logRequest(request, log);
            response = httpClient.execute(request);
            logResponse(response, log);
            return response;
        } catch (IOException e) {
            log.error("Error while sending request: %s", e);
            return null;
        }
    }

    /**
     * Get the OAuth 2.0 Authorization Bearer token using the cofigured access token provider and set it to the
     * default request header list.
     *
     * @param config the {@link AgentConfig}
     * @param log    the {@link ReplicationLog}
     * @return default request header containing OAuth 2.0 Authorization Bearer token
     */
    @Override
    protected List<Header> createDefaultHeaders(AgentConfig config, ReplicationLog log) {
        final List<Header> defaultHeaders = super.createDefaultHeaders(config, log);

        try {
            // get the transport uri and remove the demandware:// prefix
            final String transportUri = StringUtils.replace(config.getTransportURI(), DWRE_SCHEME, "https://");

            final URI uri = new URI(transportUri);
            if ("https".equals(uri.getScheme())) {
                ValueMap conf = config.getProperties();
                // OAuth 2.0 Authorization Grants
                // obtain a valid access token and use it for authentication
                if (config.isOAuthEnabled()) {
                    log.debug("* Using OAuth 2.0 Authorization Grants");
                    AccessTokenProvider accessTokenProvider = getAccessTokenProvider(config);
                    if (accessTokenProvider != null) {
                        final String agentUserID = conf.get(AgentConfig.AGENT_USER_ID, "");
                        log.debug("* OAuth 2.0 User: %s", agentUserID);
                        // get an access token for agent user

                        try (final ResourceResolver resolver = getReplicationLoginService().createResourceResolver()) {
                            String accessToken = accessTokenProvider.getAccessToken(resolver, agentUserID, null);
                            String authorization = String.format(BEARER_AUTHENTICATION_FORMAT, accessToken);
                            defaultHeaders.add(new BasicHeader(HttpHeaders.AUTHORIZATION, authorization));
                            log.debug("* OAuth 2.0 Authorization Bearer setup successful");
                        } catch (DemandwareReplicationException drex) {
                            log.error("Failed to obtain resource resolver: %s", drex.getMessage());
                        } catch (Exception e) {
                            log.error("Failed to get an access token for user: %s msg: %s", agentUserID,
                                    e.getMessage());
                        }
                    } else {
                        log.error("Access token provider is not bind");
                    }
                } else {
                    log.warn("OAuth 2.0 Authorization not configured");
                }
            } else {
                if (config.isOAuthEnabled()) {
                    log.warn("OAuth 2.0 Authorization Grants requires SSL");
                }
                log.warn("Agent needs to be configured using https protocol");

            }
        } catch (URISyntaxException e) {
            log.error("Transport uri not valid: ", e);
        }
        return defaultHeaders;
    }

    protected String getOCApiVersion() {
        return ocapiVersion;
    }

    protected String getOCApiPath() {
        return ocapiPath;
    }

    /**
     * Extract the ETag header value from the response.
     *
     * @param response the response
     * @return the ETag header value or null
     */
    protected String getETagHeader(HttpResponse response) {
        if (response != null && response.containsHeader(HttpHeaders.ETAG)) {
            return response.getFirstHeader(HttpHeaders.ETAG).getValue();
        }
        return null;
    }

    /**
     * Helper to check if a HTTP request was successful.
     *
     * @param response the HTTP response
     * @return true or false
     */
    protected boolean isRequestSuccessful(HttpResponse response) {
        return response != null && (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK || response
                .getStatusLine().getStatusCode() == HttpStatus.SC_CREATED || response.getStatusLine().getStatusCode()
                == HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Log the HTTP request to replication log.
     *
     * @param request the HTTP request
     * @param log the replication log
     */
    protected void logRequest(HttpUriRequest request, ReplicationLog log) {
        log.debug("Request %s to %s", request.getMethod(), request.getURI());
        for (Header header : request.getAllHeaders()) {
            log.debug("> Header %s", header.getName() + ": " + header.getValue());
        }
    }

    /**
     * Log the HTTP response to replication log.
     *
     * @param response the HTTP response
     * @param log the replication log
     */
    protected void logResponse(HttpResponse response, ReplicationLog log) {
        log.debug("Response %s %s %s", response.getStatusLine().getStatusCode(), response.getStatusLine()
                .getReasonPhrase(), response.getStatusLine().getProtocolVersion().toString());
        for (Header header : response.getAllHeaders()) {
            log.debug("> Header %s", header.getName() + ": " + header.getValue());
        }
        if (isRequestSuccessful(response)) {
            return;
        }

        try {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                log.debug("Not found: %s", EntityUtils.toString(response.getEntity()));
            } else {
                log.error("> Error message: %s", EntityUtils.toString(response.getEntity()));
            }
        } catch (IOException e) {
           // do nothing
        }
    }

    private AccessTokenProvider getAccessTokenProvider(final AgentConfig agentConfig) {

        final String instanceId = getInstanceIdProvider().getInstanceId(agentConfig);
        final String accessTokenProviderId = getAccessTokenProviderId(accessTokenProviderClientId, instanceId);

        AccessTokenProvider accessTokenProvider = null;
        if (accessTokenProvidersProps.size() > 0 && accessTokenProviders.size() > 0) {
            if (StringUtils.isNotBlank(accessTokenProviderId) && null != accessTokenProvidersProps.get(
                    accessTokenProviderId)) {
                accessTokenProvider = accessTokenProviders.get(accessTokenProvidersProps.get(
                        accessTokenProviderId));
            }
            if (null == accessTokenProvider) {
                accessTokenProvider = (AccessTokenProvider) accessTokenProviders.values().toArray()[0];
            }
        }
        return accessTokenProvider;
    }

    protected String getAccessTokenProviderId(final Map<String, Object> properties) {
        String clientId = (String) properties.get("auth.token.provider.client.id");
        String instanceId = (String) properties.get("instance.id");
        return getAccessTokenProviderId(clientId, instanceId);
    }

    protected String getAccessTokenProviderId(final String clientId, final String instanceId) {
        return String.format("%s-%s", clientId, instanceId);
    }

    protected Map<String, Comparable<Object>> getAccessTokenProvidersProps() {
        return accessTokenProvidersProps;
    }

    protected Map<Comparable<Object>, AccessTokenProvider> getAccessTokenProviders() {
        return accessTokenProviders;
    }


    /**
     * Implementors have to call this activate method in order to
     * initialize common properties.
     *
     * This ist a remnant of the former abstract components declaration which
     * still has to be cleaned up.
     *
     * @param ctx The component context of the implementing OSGi service.
     */
    protected void activate(final ComponentContext ctx) {
        final Dictionary<?, ?> config = ctx.getProperties();
        accessTokenProviderClientId = PropertiesUtil.toString(config.get(ACCESS_TOKEN_PROVIDER), "");
        ocapiVersion = PropertiesUtil.toString(config.get(OCAPI_VERSION), DEFAULT_OCAPI_VERSION);
        ocapiPath = StringUtils.appendIfMissing(PropertiesUtil.toString(config.get(OCAPI_PATH), DEFAULT_OCAPI_PATH),
                "/", "/");
    }

    /**
     * This bind has to be wired via a refence annotation on the implementing
     * component.
     *
     * This ist a remnant of the former abstract components declaration which
     * still has to be cleaned up.
     *
     * @param atp
     * @param properties
     */
    protected void bindAccessTokenProvider(final AccessTokenProvider atp, final Map<String, Object> properties) {
        String atpId = getAccessTokenProviderId(properties);
        getAccessTokenProvidersProps().put(atpId, ServiceUtil.getComparableForServiceRanking(properties));
        getAccessTokenProviders().put(ServiceUtil.getComparableForServiceRanking(properties), atp);
    }

    /**
     * This unbind has to be wired via a reference annotation on the implementing
     * component.
     *
     * This ist a remnant of the former abstract components declaration which
     * still has to be cleaned up.
     *
     * @param atp
     * @param properties
     */
    protected void unbindAccessTokenProvider(final AccessTokenProvider atp, final Map<String, Object> properties) {
        String atpId = getAccessTokenProviderId(properties);
        getAccessTokenProviders().remove(getAccessTokenProvidersProps().get(atpId));
        getAccessTokenProvidersProps().remove(atpId);
    }

}
