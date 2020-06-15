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
import com.adobe.cq.commerce.demandware.DemandwareClientProvider;
import com.adobe.cq.commerce.demandware.replication.TransportHandlerPlugin;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ReplicationLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.http.Header;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.sling.commons.json.JSONObject;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.day.cq.personalization.offerlibrary.usebean.OffercardPropertiesProvider.LOGGER;
import static org.eclipse.jetty.util.URIUtil.HTTPS;

/**
 * Abstract {@code TransportHandlerPlugin} class used as base for all Demandware transport handlers.
 */
public abstract class AbstractTransportHandlerPlugin implements TransportHandlerPlugin {

    static final Pattern placeHolder = Pattern.compile("\\{(.*?)\\}");

    /**
     * Return the demandware client provider
     *
     * @return
     */
    protected abstract DemandwareClientProvider getClientProvider();

    /**
     * Return the supported API type.
     *
     * @return
     */
    abstract String getApiType();

    /**
     * Return the supported content type.
     *
     * @return
     */
    abstract String getContentType();

    @Override
    public boolean canHandle(final String apiType, final String contentType) {
        return StringUtils.equals(apiType, getApiType()) && StringUtils.equals(contentType, getContentType());
    }

    /**
     * Constructs the final service endpoint by replacing placeholders with the matching attribute values form the
     * content meta data.
     *
     * @param endpointUrl the service endpoint URL with placeholders
     * @param delivery    the content object to be replicated
     * @return the final service endpoint URL
     */
    protected String constructEndpointURL(String endpointUrl, JSONObject delivery) {
        Matcher matchPattern = placeHolder.matcher(endpointUrl);
        while (matchPattern.find()) {
            if (delivery.has(matchPattern.group(1))) {
                endpointUrl = StringUtils.replace(endpointUrl, matchPattern.group(), delivery.optString(matchPattern
                        .group(1)));
            }
        }
        return endpointUrl;
    }

    /**
     * Creates a HTTP client builder with all of the defaults.
     *
     * @param config the replication agent config
     * @param log    the replication log
     * @return the {@code HttpClientBuilder}
     */
    protected HttpClientBuilder getHttpClientBuilder(final AgentConfig config, final ReplicationLog log) {
        // create and configure the Http client builder
        final DemandwareClient demandwareClient = getClientProvider().getClientForSpecificInstance(config);
        final HttpClientBuilder httpClientBuilder = demandwareClient.getHttpClientBuilder();

        if ("relaxed".equals(config.getSSLConfig())) {
            try {
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
                httpClientBuilder.setConnectionManager(new PoolingHttpClientConnectionManager(socketFactoryRegistry));
                httpClientBuilder.setSSLSocketFactory(sslConnectionSocketFactory);
            } catch(NoSuchAlgorithmException nsae) {
                LOGGER.error("Cannot trust self signed certificate", nsae);
                httpClientBuilder.setConnectionManager(new PoolingHttpClientConnectionManager());
            } catch (KeyStoreException kse) {
                LOGGER.error("Unable to open key store", kse);
                httpClientBuilder.setConnectionManager(new PoolingHttpClientConnectionManager());
            } catch (KeyManagementException kme) {
                LOGGER.error("Unable to retrieve key from key store", kme);
                httpClientBuilder.setConnectionManager(new PoolingHttpClientConnectionManager());
            }
        } else {
            httpClientBuilder.setConnectionManager(new PoolingHttpClientConnectionManager());
        }

        // configure credentials
        final CredentialsProvider credentialsProvider = createCredentialsProvider(demandwareClient, log);
        if (credentialsProvider != null) {
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        // set default request header
        httpClientBuilder.setDefaultHeaders(createDefaultHeaders(config, log));

        return httpClientBuilder;
    }

    /**
     * Create the credentials provider.
     *
     * @param demandwareClient client providing connection to specific SFCC instance
     * @param log    the replication log
     * @return the configured {@code CredentialsProvider}
     */
    protected CredentialsProvider createCredentialsProvider(DemandwareClient demandwareClient, ReplicationLog log) {
        return null;
    }

    /**
     * Creates a HTTP client with all of the defaults.
     *
     * @param config the replication agent config
     * @param log    the replication log
     * @return the {@code HttpClientBuilder}
     */
    protected HttpClient getHttpClient(final AgentConfig config, final ReplicationLog log) {
        return getHttpClientBuilder(config, log).build();
    }

    /**
     * Set default http request header attributes.
     *
     * @param config the replication agent config
     * @param log    the replication log
     * @return the list of headers
     */
    protected List<Header> createDefaultHeaders(AgentConfig config, ReplicationLog log) {
        List<Header> headers = new ArrayList<>();

        String[] confHeaders = config.getProperties().get(AgentConfig.PROTOCOL_HTTP_HEADERS, String[].class);
        if (confHeaders != null) {
            for (String header : confHeaders) {
                if (header.indexOf(':') > 0) {
                    final String name = StringUtils.substringBefore(header, ":");
                    final String value = StringUtils.substringAfter(header, ":");
                    log.debug("adding header: %s:%s", name, value);
                    headers.add(new BasicHeader(name, value));
                }
            }
        }
        return headers;
    }
}