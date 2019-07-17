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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Constants;

import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.replication.TransportHandlerPlugin;
import com.day.cq.replication.ReplicationException;

/**
 * <code>TransportHandlerPlugin</code> to send content asset data to Demandware using OC data API.
 */
@Component(label = "Demandware TransportHandler Plugin Content Slot Config", metatype = true, immediate = true)
@Service(value = TransportHandlerPlugin.class)
@Properties({@Property(name = TransportHandlerPlugin.PN_TASK, value = "ContentSlotConfigPlugin", propertyPrivate =
        true), @Property(name = Constants.SERVICE_RANKING, intValue = 10)})
public class ContentSlotConfigPlugin extends AbstractOCAPITransportPlugin {

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
}
