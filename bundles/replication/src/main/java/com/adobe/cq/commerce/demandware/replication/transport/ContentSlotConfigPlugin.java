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

import com.adobe.cq.commerce.demandware.DemandwareClientProvider;
import com.adobe.cq.commerce.demandware.InstanceIdProvider;
import com.adobe.granite.auth.oauth.AccessTokenProvider;
import org.apache.felix.scr.annotations.*;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Constants;

import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.replication.TransportHandlerPlugin;
import com.day.cq.replication.ReplicationException;
import org.osgi.service.component.ComponentContext;

import static com.adobe.cq.commerce.demandware.replication.transport.AbstractOCAPITransportPlugin.*;

/**
 * <code>TransportHandlerPlugin</code> to send content asset data to Demandware using OC data API.
 */
@Component(label = "Demandware TransportHandler Plugin Content Slot Config", metatype = true, immediate = true)
@Service(value = TransportHandlerPlugin.class)
@Properties({
        @Property(name = TransportHandlerPlugin.PN_TASK, value = "ContentSlotConfigPlugin", propertyPrivate = true),
        @Property(name = Constants.SERVICE_RANKING, intValue = 10),
        @Property(name = ACCESS_TOKEN_PROVIDER, label = "Access Token Provider Id to be used for OCAPI access"),
        @Property(name = OCAPI_VERSION, label = "OCAPI version", value = DEFAULT_OCAPI_VERSION),
        @Property(name = OCAPI_PATH, label = "OCAPI path", value = DEFAULT_OCAPI_PATH)
})
@Reference(name = AbstractOCAPITransportPlugin.ACCESS_TOKEN_PROPERTY,
        referenceInterface = AccessTokenProvider.class, bind = "bindAccessTokenProvider", unbind = "unbindAccessTokenProvider",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
public class ContentSlotConfigPlugin extends AbstractOCAPITransportPlugin {

    @Reference
    private InstanceIdProvider instanceIdProvider;

    @Reference
    private DemandwareClientProvider clientProvider;

    @Reference
    private ResourceResolverFactory rrf;

    @Override
    protected DemandwareClientProvider getClientProvider() {
        return clientProvider;
    }

    @Override
    protected ResourceResolverFactory getResourceResolverFactory() {
        return rrf;
    }

    @Override
    protected InstanceIdProvider getInstanceIdProvider() {
        return instanceIdProvider;
    }

    @Override
    String getContentType() {
        return "content-slot-config";
    }

    @Override
    protected RequestBuilder getRequestBuilder(final String method, final JSONObject delivery, String dwInstanceId) throws
            ReplicationException {
        final RequestBuilder requestBuilder = super.getRequestBuilder(method, delivery, dwInstanceId);
        try {
            if (delivery.has(DemandwareCommerceConstants.ATTR_CONTEXT)) {
                requestBuilder.addParameter(DemandwareCommerceConstants.ATTR_CONTEXT, delivery.getString
                        (DemandwareCommerceConstants.ATTR_CONTEXT));
            }
        } catch (JSONException e) {
            throw new ReplicationException("Can not create endpoint URI", e);
        }
        return requestBuilder;
    }

    @Activate
    protected void activate(ComponentContext ctx) {
        super.activate(ctx);
    }
}
