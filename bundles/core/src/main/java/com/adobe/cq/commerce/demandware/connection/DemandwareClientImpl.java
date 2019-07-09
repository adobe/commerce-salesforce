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

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.ssl.SSLContexts;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.demandware.DemandwareClient;

/**
 * Central Demandware client service providing Demandware instance endpoint and already prepared, ready to use HTTP
 * clients to access the instance via OCAPI or WebDAV.
 */
@Component(metatype = true, policy = ConfigurationPolicy.REQUIRE, label = "Demandware Client")
@Service()
public class DemandwareClientImpl implements DemandwareClient {

    private static final Logger LOG = LoggerFactory.getLogger(DemandwareClientImpl.class);

    @Property(label = "Instance endpoint ip or hostname")
    private static final String INSTANCE_ENDPOINT = "endpoint";

    @Property(label = "Socket timeout")
    private static final String INSTANCE_SOCKET_TIMEOUT = "timeout.socket";

    @Property(label = "HTTP connection timeout")
    private static final String INSTANCE_CONNECTION_TIMEOUT = "connection.socket";

    @Property(label = "Local network interface to be used")
    private static final String PROTOCOL_INTERFACE = "interface";

    @Property(label = "SSL version", value = "TLSv1.2")
    private static final String PROTOCOL_SSL = "ssl";

    @Property(label = "Keystore type", options = {@PropertyOption(name = "JKS", value = "JKS"), @PropertyOption(name = "PKCS12", value = "PKCS12")})
    private static final String KEYSTORE_TYPE = "keystore.type";

    @Property(label = "Keystore path", description = "Path of client certificate key store path, we support JKS or PKCS12 keystores")
    private static final String KEYSTORE_PATH = "keystore.path";

    @Property(label = "Keytsore password")
    private static final String KEYSTORE_PWD = "keystore.password";

    @Property(label = "Key password", description = "Leave empty for no password")
    private static final String KEY_PWD = "key.password";

    @Property(label = "Instance id", description = "TODO", value = "mariia")
    private static final String INSTANCE_ID = "instance.id";

    private String instanceEndPoint;
    private int socketTimeout;
    private int connectionTimeout;
    private String protocolInterface;
    private String protocolSSL;
    private String keystoreType;
    private String keyStorePath;
    private String keyStorePwd;
    private String keyPwd;

    @Override
    public String getEndpoint() {
        return instanceEndPoint;
    }

    @Override
    public HttpClientBuilder getHttpClientBuilder() {
        LOG.debug("Create new HttpClient");

        // create and configure the Http client builder
        final HttpClientBuilder httpClientBuilder = HttpClients.custom();

        Registry<ConnectionSocketFactory> schemeRegistry = this.createDefaultSchemeRegistry();
        HttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(schemeRegistry);
        httpClientBuilder.setConnectionManager(cm);

        // configure proxy
        httpClientBuilder.setRoutePlanner(createHttpRoutePlanner());

        // define socket timeout
        if (socketTimeout > 0) {
            LOG.debug("* Socket Timeout: {}", socketTimeout);
            final SocketConfig sc = SocketConfig.custom().setSoTimeout(socketTimeout).build();
            httpClientBuilder.setDefaultSocketConfig(sc);
        }

        // define default request behavior
        httpClientBuilder.setDefaultRequestConfig(createRequestConfig());

        // define redirect strategy
        httpClientBuilder.setRedirectStrategy(createDefaultRedirectStrategy());

        return httpClientBuilder;
    }

    /**
     * Setup the default http route planer.
     *
     * @return the {@code HttpRoutePlanner}
     */
    protected HttpRoutePlanner createHttpRoutePlanner() {
        return new SystemDefaultRoutePlanner(ProxySelector.getDefault());
    }

    /**
     * Setup the default redirect strategy.
     *
     * @return the {@code RedirectStrategy}
     */
    protected RedirectStrategy createDefaultRedirectStrategy() {
        return new DefaultRedirectStrategy();
    }

    /**
     * Creates the default request config.
     *
     * @return the {@code RequestConfig}
     */
    protected RequestConfig createRequestConfig() {
        final RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();

        // define connect timeout
        if (connectionTimeout > 0) {
            LOG.debug("* Connect Timeout: {}", connectionTimeout);
            requestConfigBuilder.setConnectTimeout(connectionTimeout);
        }

        // define local interface
        if (StringUtils.isNotEmpty(protocolInterface)) {
            try {
                requestConfigBuilder.setLocalAddress(InetAddress.getByName(protocolInterface));
            } catch (UnknownHostException e) {
                LOG.debug("Cannot set local address to {}: {}", protocolInterface, e.toString());
            }
        }
        return requestConfigBuilder.build();
    }

    /**
     * Creates a new registry for default ports with socket factories.
     *
     * @return the {@code Registry}
     */
    protected Registry<ConnectionSocketFactory> createDefaultSchemeRegistry() {
        return RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", this.createDefaultSecureSocketFactory())
            .build();
    }

    /**
     * @return Default SSL socket factory
     */
    protected ConnectionSocketFactory createDefaultSecureSocketFactory() {
        SSLConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        SSLContext sslContext = SSLContexts.createDefault();

        try {
            if (keyStorePath != null && keyStorePwd != null) {
                LOG.debug("* Use client keystore of type {} from {}", keystoreType, keyStorePath);
                KeyStore keyStore = null;
                try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                    keyStore = KeyStore.getInstance(keystoreType != null ? keystoreType : "JKS");
                    keyStore.load(fis, keyStorePwd.toCharArray());
                }
                // create custom SSL context
                sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, keyPwd.toCharArray()).loadTrustMaterial(null, new TrustSelfSignedStrategy())
                    .build();
            }

            // create custom SSL connection factory
            sslsf = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{protocolSSL},
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Cannot set custom ssl config: {}", e.toString());
        } catch (KeyManagementException e) {
            LOG.error("Cannot set custom ssl config: {}", e.toString());
        } catch (Exception e) {
            LOG.error("Cannot set custom ssl config: {}", e.toString());
        }
        return sslsf;
    }

    @Override
    public CloseableHttpClient getHttpClient() {
        return getHttpClientBuilder().build();
    }

    @Activate
    protected void activate(Map<String, Object> configuration) {
        instanceEndPoint = PropertiesUtil.toString(configuration.get(INSTANCE_ENDPOINT), null);
        protocolInterface = PropertiesUtil.toString(configuration.get(PROTOCOL_INTERFACE), null);
        protocolSSL = StringUtils.trimToNull(PropertiesUtil.toString(configuration.get(PROTOCOL_SSL), "TLSv1.2"));
        keystoreType = StringUtils.trimToNull(PropertiesUtil.toString(configuration.get(KEYSTORE_TYPE), "JKS"));
        keyStorePath = StringUtils.trimToNull(PropertiesUtil.toString(configuration.get(KEYSTORE_PATH), null));
        keyStorePwd = StringUtils.trimToNull(PropertiesUtil.toString(configuration.get(KEYSTORE_PWD), null));
        keyPwd = StringUtils.trimToNull(PropertiesUtil.toString(configuration.get(KEY_PWD), null));
        socketTimeout = PropertiesUtil.toInteger(configuration.get(INSTANCE_SOCKET_TIMEOUT), 0);
        connectionTimeout = PropertiesUtil.toInteger(configuration.get(INSTANCE_CONNECTION_TIMEOUT), 0);

        String instanceId = PropertiesUtil.toString(configuration.get(INSTANCE_ID), null);
        LOG.debug("activating Demandware client service");
    }
}
