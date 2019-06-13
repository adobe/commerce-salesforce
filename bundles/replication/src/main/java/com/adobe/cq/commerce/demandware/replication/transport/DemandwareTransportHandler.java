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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Constants;

import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.replication.TransportHandlerPlugin;
import com.day.cq.replication.*;

@Component(label = "Demandware TransportHandler")
@Service(value = TransportHandler.class)
public class DemandwareTransportHandler implements TransportHandler {

    @Reference(referenceInterface = TransportHandlerPlugin.class,
            bind = "bindTransportHandlerPlugin",
            unbind = "unbindTransportHandlerPlugin",
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    private final List<TransportHandlerPluginWrapper> transportHandlerPlugins = Collections.synchronizedList(new
            ArrayList<TransportHandlerPluginWrapper>());


    /**
     * {@inheritDoc}
     * Note: we can not use http:// or https:// here since that is handled by the default HTTP transport handler
     *
     * @return <code>true</code> if the scheme of the uri is <code>demandware</code>
     */
    @Override
    public boolean canHandle(AgentConfig config) {
        String uri = config == null ? null : config.getTransportURI();
        return uri != null && uri.startsWith(TransportHandlerPlugin.DWRE_SCHEME);
    }


    /**
     * Delivers content to a subscriber.
     *
     * @param ctx transport context
     * @param tx  the replication transaction
     * @return the replication result of this operation.
     * @throws ReplicationException if an error occurs.
     */
    @Override
    public ReplicationResult deliver(TransportContext ctx, ReplicationTransaction tx) throws ReplicationException {
        final ReplicationAction action = tx.getAction();
        final ReplicationLog log = tx.getLog();

        if (tx.getContent() == ReplicationContent.VOID && action.getType().equals(ReplicationActionType
                .ACTIVATE)) {
            log.info("Nothing to replicate for " + tx.getAction().getPath());
            return new ReplicationResult(true, 0, "Done");
        }

        if (action.getType() == ReplicationActionType.TEST) {
            throw new ReplicationException(
                    "Test replication not supported by this transport handler.");
        }
        if (action.getType() == ReplicationActionType.INTERNAL_POLL) {
            throw new ReplicationException(
                    "Reverse replication not supported by this transport handler.");
        }

        ReplicationResult replicationResult = null;
        final ReplicationContent content = tx.getContent();
        final ReplicationActionType actionType = action.getType();

        if (actionType == ReplicationActionType.ACTIVATE || actionType == ReplicationActionType.DELETE ||
                actionType == ReplicationActionType.DEACTIVATE) {
            if (content == null || content.getContentLength() == 0) {
                log.debug("No message body: No content to deliver");
                return new ReplicationResult(true, 410, "Replication content gone.");
            }
            JSONObject jsonReplicationContent = getJSONReplicationContent(content);
            try {
                final String apiType = jsonReplicationContent.getString(DemandwareCommerceConstants.ATTR_API_TYPE);
                if (StringUtils.isNotEmpty(apiType)) {
                    final String contentType = jsonReplicationContent.getString(DemandwareCommerceConstants
                            .ATTR_CONTENT_TYPE);
                    if (StringUtils.isNotEmpty(contentType)) {
                        boolean success = true;
                        boolean handled = false;
                        // iterate all registered plugins and delegate the work
                        for (TransportHandlerPluginWrapper transportHandlerPluginWrapper :
                                transportHandlerPlugins) {
                            TransportHandlerPlugin transportHandlerPlugin = transportHandlerPluginWrapper
                                    .transportHandlerPlugin;
                            if (transportHandlerPlugin.canHandle(apiType, contentType) && success) {
                                log.debug("Send data: api: %s content type: %s using %s", apiType, contentType,
                                        transportHandlerPlugin.getClass());
                                success = transportHandlerPlugin.deliver(jsonReplicationContent, ctx.getConfig(),
                                        log, action);
                                handled = true;
                            }
                        }
                        if (handled == false) {
                            log.error("No transport plugin found for this request - api: %s / content type: %s",
                                    apiType, contentType);
                            return new ReplicationResult(false, 422, "No transport plugin found for this request.");
                        }
                        replicationResult = createReplicationResult(success, "DWRE", log, action);
                    } else {
                        log.error("No message content type.");
                        return new ReplicationResult(false, 422, "No message content type.");
                    }
                } else {
                    log.error("No message API type.");
                    return new ReplicationResult(false, 422, "No message API type.");
                }
            } catch (JSONException e) {
                throw new ReplicationException(e);
            }
        } else {
            log.error("Replication action %s not supported", action.getType().getName());
        }
        return replicationResult;
    }

    /**
     * Transforms the {@link ReplicationContent} into a {@link JSONObject}.
     *
     * @param content the replication content
     * @return the transformed JSON object
     * @throws ReplicationException in case the JSON replication content could not be read
     */
    private JSONObject getJSONReplicationContent(ReplicationContent content) throws ReplicationException {
        try {
            if (StringUtils.equals(content.getContentType(), ContentType.APPLICATION_JSON.getMimeType())) {
                final String jsonString = IOUtils.toString(content.getInputStream(), "UTF-8");
                return new JSONObject(jsonString);
            }
        } catch (JSONException | IOException e) {
            throw new ReplicationException(e);
        }
        throw new ReplicationException("Content-type " + content.getContentType() + " is not supported");
    }

    /**
     * Create the {@link ReplicationResult} for the current action.
     * @param success true to indicate successful replication
     * @param statusMsg the message put into the log
     * @param log    the replication log
     * @param action the replication action
     * @return the new {@link ReplicationResult}
     */
    protected ReplicationResult createReplicationResult(boolean success, String statusMsg, ReplicationLog log,
                                                        ReplicationAction action) {
        int statusCode = HttpStatus.SC_OK;
        if (action.getType().equals(ReplicationActionType.DEACTIVATE) || action.getType().equals
                (ReplicationActionType.DELETE)) {
            statusCode = HttpStatus.SC_NO_CONTENT;
        }

        if (success) {
            log.info("Replication (%s) of %s successful.", action.getType(), action.getPath());
        } else {
            log.info("Replication (%s) of %s not successful.", action.getType(), action.getPath());
        }
        return new ReplicationResult(success, statusCode, statusMsg);
    }

    /*
     * OSGI stuff
     */

    protected void bindTransportHandlerPlugin(final TransportHandlerPlugin transportHandlerPlugin,
                                              final Map<?, ?> properties) {
        final String taskname = (String) properties.get(TransportHandlerPlugin.PN_TASK);
        final int serviceRanking = properties.containsKey(Constants.SERVICE_RANKING) ? (Integer) properties.get(
                Constants.SERVICE_RANKING) : 0;

        transportHandlerPlugins.add(
                new TransportHandlerPluginWrapper(transportHandlerPlugin, taskname, serviceRanking));
        Collections.sort(transportHandlerPlugins, new Comparator<TransportHandlerPluginWrapper>() {
            @Override
            public int compare(final TransportHandlerPluginWrapper tHPW1,
                               final TransportHandlerPluginWrapper tHPW2) {
                return Integer.compare(tHPW1.serviceRanking, tHPW2.serviceRanking);
            }
        });
    }

    protected void unbindTransportHandlerPlugin(final TransportHandlerPlugin transportHandlerPlugin,
                                                final Map<?, ?> properties) {
        final String taskname = (String) properties.get(TransportHandlerPlugin.PN_TASK);
        for (final TransportHandlerPluginWrapper wrapper : transportHandlerPlugins) {
            if (wrapper.taskname.equals(taskname)) {
                transportHandlerPlugins.remove(wrapper);
                break;
            }
        }
    }

    /**
     * Wrapper class for a {@code TransportHandlerPlugin} to keep track of service ranking properties.
     */
    private static class TransportHandlerPluginWrapper {
        private final TransportHandlerPlugin transportHandlerPlugin;
        private final String taskname;
        private final int serviceRanking;

        public TransportHandlerPluginWrapper(final TransportHandlerPlugin transportHandlerPlugin, final String taskname,
                                             final int serviceRanking) {
            this.transportHandlerPlugin = transportHandlerPlugin;
            this.taskname = taskname;
            this.serviceRanking = serviceRanking;
        }
    }
}
