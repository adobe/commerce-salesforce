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

import org.apache.sling.commons.json.JSONObject;

import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationLog;

public interface TransportHandlerPlugin {

    String DWRE_SCHEME = "demandware://";
    String PN_TASK = "plugin.name";

    /**
     * Returns <code>true</code> if this transport handler plugin can handle the transport request.
     *
     * @param apiType     the type of API call (either OCAPI or WEBDAV)
     * @param contentType the content typ to be send
     * @return <code>true</code> if this plugin can handle the resource
     */
    boolean canHandle(String apiType, String contentType);

    boolean deliver(JSONObject delivery, AgentConfig config, ReplicationLog log,
                    ReplicationAction action) throws ReplicationException;
}
