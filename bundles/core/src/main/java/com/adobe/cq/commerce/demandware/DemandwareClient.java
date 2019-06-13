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

package com.adobe.cq.commerce.demandware;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Central Demandware client service providing Demandware instance endpoint and already prepared, ready to use HTTP
 * clients to access the instance via OCAPI or WebDAV.
 */
public interface DemandwareClient {

    String DEFAULT_SCHEMA = "https://";

    /**
     * Get the configured base endpoint of the Demandware instance.
     *
     * @return configured Demandware instance endpoint
     */
    String getEndpoint();

    /**
     * Creates a HTTP client builder with all of the defaults.
     *
     * @return the created {@code HttpClientBuilder}
     */
    HttpClientBuilder getHttpClientBuilder();

    /**
     * Creates a HTTP client using the client builder.
     *
     * @return the created {@code CloseableHttpClient}
     */
    CloseableHttpClient getHttpClient();
}
