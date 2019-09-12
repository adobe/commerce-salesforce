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
package com.adobe.cq.commerce.demandware.replication.transport.oauth;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.net.ssl.SSLContext;

import com.adobe.cq.commerce.demandware.DemandwareClient;
import com.adobe.cq.commerce.demandware.DemandwareClientProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.Consts;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Deactivate;
import com.adobe.granite.auth.oauth.AccessTokenProvider;
import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;

@Service
@Component(metatype = true, name = AccessTokenProviderImpl.FACTORY_PID, configurationFactory = true, immediate = true,
        policy = ConfigurationPolicy.REQUIRE, label = "Demandware Access Token provider")
@Properties({
        @Property(name = "webconsole.configurationFactory.nameHint", value = "{service.factoryPid}: {auth.token.provider.client.id}-{instance.id}")
})
public class AccessTokenProviderImpl implements AccessTokenProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessTokenProviderImpl.class);

    public static final String FACTORY_PID = "com.adobe.cq.commerce.demandware.oauth.accesstoken.provider";

    @Property(
            label = "InstanceID",
            description = "Demandware instance identifier. Used in conjunction with the provider.id " +
                    "to uniquely identify this service.")
    private static final String INSTANCE_ID = "instance.id";

    private static final String DEFAULT_CLIENT_ID = "DemandwareAuthorizationClient";
    @Property(
            label = "ProviderID",
            value = DEFAULT_CLIENT_ID,
            description = "Can be used to further differentiate the access " +
                    "token provider. An AccessTokenProvider is identified by it's " +
                    "provider.id AND instance.id. This is mainly used for backwards compatibility. " +
                    "Leave to 'DemandwareAuthorizationClient' if in doubt.")
    protected static final String CLIENT_ID = "auth.token.provider.client.id";



    private static final String DEFAULT_END_POINT = "account.demandware.com";
    @Property(value = DEFAULT_END_POINT)
    protected static final String END_POINT = "auth.token.provider.endpoint";

    private static final String DEFAULT_AUTHORIZATION_GRANTS_TYPE = "client_credentials";
    @Property(propertyPrivate = true, value = DEFAULT_AUTHORIZATION_GRANTS_TYPE)
    protected static final String AUTHORIZATION_GRANTS_TYPE = "auth.token.provider.authorization.grants";

    private static final String DEFAULT_ACCESS_TOKEN_REQ_FORMAT = "https://%s/token";
    @Property(value = DEFAULT_ACCESS_TOKEN_REQ_FORMAT)
    protected static final String ACCESS_TOKEN_REQ_FORMAT = "auth.access.token.request";

    /**
     * The timeout until a connection is established.
     * Default is 30 seconds.
     */
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30 * 1000;
    @Property(intValue = DEFAULT_CONNECTION_TIMEOUT)
    protected static final String CONNECTION_TIMEOUT = "auth.token.provider.conn.timeout";

    /**
     * The socket timeout in milliseconds which is the timeout for waiting for data from the authorization server.
     * Default is 30 seconds.
     */
    private static final int DEFAULT_SO_TIMEOUT = 30 * 1000;
    @Property(intValue = DEFAULT_SO_TIMEOUT)
    protected static final String SO_TIMEOUT = "auth.token.provider.so.timeout";



    /**
     * Token validity leeway in minute (default Demandware tokes are valid to 30 minutes, so we used 25 minutes)
     */
    private static final int DEFAULT_LEEWAY = 25 * 60 * 1000;
    @Property(propertyPrivate = true, intValue = DEFAULT_LEEWAY)
    protected static final String LEEWAY = "auth.token.provider.leeway";

    private static final boolean DEFAULT_REUSE_ACCESS_TOKENS = true;
    @Property(boolValue = DEFAULT_REUSE_ACCESS_TOKENS)
    protected static final String REUSE_ACCESS_TOKENS = "auth.token.provider.reuse.access.token";

    /**
     * Enable/disable the relaxed SSL configuration
     */
    private static final boolean DEFAULT_RELAXED_SSL = false;
    @Property(boolValue = DEFAULT_RELAXED_SSL)
    protected static final String RELAXED_SSL = "auth.token.provider.relaxed.ssl";

    /*
     * Demandware Client ID and password
    */
    private static final String DEFAULT_DEMANDWARE_CLIENT_ID = "";
    private static final String DEFAULT_DEMANDWARE_CLIENT_PASSWORD = "";
    private static final String HTTPS = "https";
    @Property(value = DEFAULT_DEMANDWARE_CLIENT_ID)
    protected static final String DEMANDWARE_CLIENT_ID = "auth.token.provider.demandware.client.id";

    @Property(value = DEFAULT_DEMANDWARE_CLIENT_PASSWORD)
    protected static final String DEMANDWARE_CLIENT_PASSWORD = "auth.token.provider.demandware.client.password";


    /**
     * String format applied against a single argument (the clientId) that builds a relative path from the user home,
     * where the access token is stored in an encrypted format.
     */
    private static final String DEFAULT_ACCESS_TOKEN_PATH_FORMAT = "oauth/oauthid-%s";

    @Reference
    private CryptoSupport cryptoSupport;

    @Reference
    private DemandwareClientProvider clientProvider;

    private CloseableHttpClient httpClient;

    private String authorizationGrantType;
    private String clientId;
    private String endPoint;
    private int leeway;
    private boolean reuseAccessTokens;
    private String relativeUri;
    private String instanceId;

    @Activate
    protected void activate(Map<String, Object> props)
            throws Exception {
        authorizationGrantType = PropertiesUtil.toString(props.get(AUTHORIZATION_GRANTS_TYPE),
                DEFAULT_AUTHORIZATION_GRANTS_TYPE);
        clientId = PropertiesUtil.toString(props.get(CLIENT_ID), DEFAULT_CLIENT_ID);
        endPoint = PropertiesUtil.toString(props.get(END_POINT), DEFAULT_END_POINT);
        reuseAccessTokens = PropertiesUtil.toBoolean(props.get(REUSE_ACCESS_TOKENS), DEFAULT_REUSE_ACCESS_TOKENS);
        instanceId = PropertiesUtil.toString(props.get(INSTANCE_ID), StringUtils.EMPTY);
        final String accessRequestFormat = PropertiesUtil.toString(props.get(ACCESS_TOKEN_REQ_FORMAT),
                DEFAULT_ACCESS_TOKEN_REQ_FORMAT);
        int connectionTimeout = Math.abs(PropertiesUtil.toInteger(props.get(CONNECTION_TIMEOUT),
                DEFAULT_CONNECTION_TIMEOUT));
        leeway = Math.abs(PropertiesUtil.toInteger(props.get(LEEWAY), DEFAULT_LEEWAY));
        final int soTimeout = Math.abs(PropertiesUtil.toInteger(props.get(SO_TIMEOUT), DEFAULT_SO_TIMEOUT));
        final boolean relaxedSsl = PropertiesUtil.toBoolean(props.get(RELAXED_SSL), DEFAULT_RELAXED_SSL);
        final String demandWareClientId = PropertiesUtil.toString(props.get(DEMANDWARE_CLIENT_ID),
                DEFAULT_DEMANDWARE_CLIENT_ID);
        final String demandWareClientPassword = PropertiesUtil.toString(props.get(DEMANDWARE_CLIENT_PASSWORD),
                DEFAULT_DEMANDWARE_CLIENT_PASSWORD);

        final URI uri = new URI(String.format(accessRequestFormat, endPoint));

        if (HTTPS.equals(uri.getScheme())) {
            relativeUri = uri.toString();
            DemandwareClient dwClient = clientProvider.getClientForSpecificInstance(instanceId);
            HttpClientBuilder builder = dwClient.getHttpClientBuilder();

            //set a default destination host
            HttpRoutePlanner routePlanner = new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE) {
                @Override
                public HttpRoute determineRoute(
                        final HttpHost target,
                        final HttpRequest request,
                        final HttpContext context) throws HttpException {
                    return super.determineRoute(
                            target != null ? target : new HttpHost(uri.getHost(), uri.getPort(), HTTPS),
                            request, context);
                }
            };
            builder.setRoutePlanner(routePlanner);

            if (StringUtils.isNoneEmpty(demandWareClientId + demandWareClientPassword)) {
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(
                        new AuthScope(endPoint, 443),
                        new UsernamePasswordCredentials(demandWareClientId, demandWareClientPassword));
                builder.setDefaultCredentialsProvider(credsProvider);
            }

            if (relaxedSsl) {
                LOGGER.info("Enable relaxed SSL");
                LOGGER.warn("Do not use relaxed SSL in production");

                // use TrustSelfSignedStrategy in order to allow self-signed certificates
                SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
                sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());

                //create new SSL context
                SSLContext sslContext = sslContextBuilder.build();

                //create a new socket factory
                SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
                        SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

                //build a registry using the socket factory
                Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                        .<ConnectionSocketFactory>create().register(HTTPS, sslConnectionSocketFactory)
                        .build();

                // make the connection manager use the  self-signed strategy
                builder.setConnectionManager(new PoolingHttpClientConnectionManager(socketFactoryRegistry));
                builder.setSSLSocketFactory(sslConnectionSocketFactory);
            } else {
                builder.setConnectionManager(new PoolingHttpClientConnectionManager());
            }

            // set the connection and socket timeouts
            RequestConfig.Builder configBuilder = RequestConfig.custom();
            configBuilder.setConnectTimeout(connectionTimeout);
            configBuilder.setSocketTimeout(soTimeout);

            builder.setDefaultRequestConfig(configBuilder.build());

            httpClient = builder.build();
        } else {
            String msg = "Communication with authorization server requires HTTPS";
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }
        LOGGER.info("activating provider");
    }

    @Deactivate
    protected void deactivate() throws Exception {
        HttpClientUtils.closeQuietly(httpClient);
        LOGGER.info("deactivating provider");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAccessToken(ResourceResolver resolver, String userId, Map<String, ?> claims) throws CryptoException, IOException, NullPointerException {
        if (reuseAccessTokens) {
            final String storedAccessToken = internalGetAccessToken(resolver, userId);
            if (storedAccessToken != null) {
                // check if the access token is still valid (extending the validity to the configured LEEWAY)
                final long tokenTime = new Long(StringUtils.substringAfter(storedAccessToken, ":"));
                if (tokenTime + leeway > System.currentTimeMillis()) {
                    LOGGER.debug("Reuse existing access token: " + storedAccessToken);
                    return StringUtils.substringBefore(storedAccessToken, ":");
                }
            }
        }
        LOGGER.info("requires a new access token");
        final String accessToken = tradeAccessToken(relativeUri);
        if (reuseAccessTokens) {
            internalStoreAccessToken(resolver, userId, accessToken);
        }
        return accessToken;
    }

    /**
     * @return The access token from the user profile or {@code null} if none exists
     */
    private String internalGetAccessToken(ResourceResolver resolver, String userId) {
        UserManager userManager = resolver.adaptTo(UserManager.class);
        try {
            User user = getUser(userManager, userId);
            if (user != null) {
                String accessTokenPath = getAccessTokenPath(clientId);
                if (user.hasProperty(accessTokenPath)) {
                    Value[] value = user.getProperty(accessTokenPath);
                    if (value != null && value[0] != null) {
                        String protectedAccessToken = value[0].getString();
                        return unprotect(protectedAccessToken);
                    }
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error("error while looking for access token for the user: {}", userId, e);
        }
        return null;
    }

    /**
     * store the access token
     */
    private void internalStoreAccessToken(ResourceResolver resolver, String userId, String accessToken) {
        UserManager userManager = resolver.adaptTo(UserManager.class);
        try {
            User user = getUser(userManager, userId);
            if (user != null) {
                Session session = resolver.adaptTo(Session.class);
                ValueFactory valueFactory = session.getValueFactory();
                String protectedAccessToken = protect(accessToken + ":" + System.currentTimeMillis());
                String accessTokenPath = getAccessTokenPath(clientId);
                user.setProperty(accessTokenPath, valueFactory.createValue(protectedAccessToken));
                if (!userManager.isAutoSave()) {
                    session.save();
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error("error while storing an access token for the user: {}", userId, e);
        }
    }

    /**
     * @return the user or {@code null} if the user could not be found
     */
    private User getUser(UserManager userManager, String userId)
            throws RepositoryException {
        Authorizable auth = userManager.getAuthorizable(userId);
        if (auth instanceof User) {
            return (User) auth;
        } else {
            LOGGER.warn("authorizable {} does not exist or is not a User", userId);
        }
        return null;
    }

    /**
     * @return The access token or throws an exception if no access token could be fetched from the server
     */
    private String tradeAccessToken(String uri)
            throws IOException {
        String response = null;
        HttpPost postMethod = null;
        CloseableHttpResponse responseObj = null;
        try {
            postMethod = new HttpPost(uri);

            postMethod.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

            HttpHost target = new HttpHost(endPoint, 443, HTTPS);
            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(target, basicAuth);

            // Add AuthCache to the execution context
            HttpClientContext localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);

            List<NameValuePair> body = new ArrayList<>();
            body.add(new BasicNameValuePair("grant_type", authorizationGrantType));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(body, Consts.UTF_8);
            postMethod.setEntity(entity);

            responseObj = httpClient.execute(postMethod, localContext);
            //get the response body as a string
            InputStream is = responseObj.getEntity().getContent();
            response = IOUtils.toString(is);

            int status = responseObj.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
                response = parseAccessToken(response);
            } else {
                String msg = String.format(
                        "failed to get access token from authorization server status: %s response: %s",
                        String.valueOf(status), response);
                LOGGER.error(msg);
                throw new IOException(msg);
            }
        } finally {
            HttpClientUtils.closeQuietly(responseObj);
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
        }
        return response;
    }

    /**
     * @return The access token from the authorization server response or {@code null} if the access token could
     * not be found. The response must comply with http://tools.ietf.org/html/draft-ietf-oauth-v2-31#section-4.1.4.
     */
    private String parseAccessToken(String response)
            throws IOException {
        try {
            JSONObject json = new JSONObject(response);
            return json.getString("access_token");
        } catch (JSONException e) {
            String msg = String.format("failed to parse access token: %s", e.getMessage());
            LOGGER.error(msg);
            throw new IOException(msg);
        }
    }

    /**
     * @return the relative path (to the user home folder) where the access token is stored
     */
    private String getAccessTokenPath(String clientId) {
        return String.format(DEFAULT_ACCESS_TOKEN_PATH_FORMAT, clientId);
    }

    private String protect(String accessToken) {
        try {
            return cryptoSupport.protect(accessToken);
        } catch (Exception e) {
            LOGGER.error("failed to protect access token", e);
        }
        return null;
    }

    private String unprotect(String protectedAccessToken) {
        try {
            return cryptoSupport.unprotect(protectedAccessToken);
        } catch (Exception e) {
            LOGGER.error("failed to unprotect access token", e);
        }
        return null;
    }
}