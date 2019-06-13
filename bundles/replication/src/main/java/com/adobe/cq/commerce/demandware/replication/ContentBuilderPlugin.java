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

package com.adobe.cq.commerce.demandware.replication;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import aQute.bnd.annotation.ProviderType;
import com.day.cq.replication.ReplicationAction;

/**
 * A <code>ContentBuilderPlugin</code> prepares the content (JSON format) for the given resource. The JSON object is
 * forwarded from plugin to plugin.
 * Plugins are implemented as OSGI services and called in the order of the service ranking. At the end of the plugin
 * chain a valid JSON object is expected which can be handled by the transport handler.
 */
@ProviderType
public interface ContentBuilderPlugin {

    String PN_TASK = "plugin.name";

    /**
     * Returns <code>true</code> if this content builder plugin can handle the given resource.
     *
     * @param action   the {@code ReplicationAction}
     * @param resource the resource to be replicated
     * @return <code>true</code> if this plugin can handle the resource
     */
    boolean canHandle(ReplicationAction action, Resource resource);

    /**
     * Creates the JSON content object for the resource.
     *
     * @param action   the {@code ReplicationAction}
     * @param resource the resource to be replicated
     * @param content  the existing, already prepared JSON content
     * @return the prepared JSON replication result of this plugin
     * @throws JSONException if an error occurs.
     */
    JSONObject create(ReplicationAction action, Resource resource, JSONObject content) throws JSONException;

}
