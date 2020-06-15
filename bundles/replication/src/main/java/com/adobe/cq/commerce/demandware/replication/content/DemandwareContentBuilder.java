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

package com.adobe.cq.commerce.demandware.replication.content;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlException;

import com.adobe.cq.commerce.demandware.replication.DemandwareReplicationException;
import com.adobe.cq.commerce.demandware.replication.DemandwareReplicationLoginService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;

import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.replication.ContentBuilderPlugin;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ContentBuilder;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationContentFactory;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationLog;

@Component(label = "Demandware ContentBuilder")
@Service(value = ContentBuilder.class)
public class DemandwareContentBuilder implements ContentBuilder {

    private static final String TITLE = "Demandware ContentBuilder";

    @Property(name = ContentBuilder.PROPERTY_NAME, propertyPrivate = true)
    private static final String NAME = "demandware";

    private String name;

    @Reference
    private DemandwareReplicationLoginService rrf;

    @Reference(referenceInterface = ContentBuilderPlugin.class,
            bind = "bindContentHandlerPlugin",
            unbind = "unbindContentHandlerPlugin",
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    private final List<ContentBuilderPluginWrapper> contentHandlerPlugins = Collections.synchronizedList(new
            ArrayList<ContentBuilderPluginWrapper>());

    /**
     * Create replication content for a given node.
     *
     * @param action  replication action
     * @param factory factory used to create content
     * @return replication content
     * @throws ReplicationException if a replication failure occurs
     */
    public ReplicationContent create(Session session, ReplicationAction action,
                                     ReplicationContentFactory factory) throws ReplicationException {
        final ReplicationLog log = action.getLog();
        final AgentConfig config = action.getConfig();

//        if (action.getType() != ReplicationActionType.ACTIVATE) {
//            return ReplicationContent.VOID;
//        }

        if (config == null) {
            throw new ReplicationException("No agent configuration found.");
        }
        if (log == null) {
            throw new ReplicationException("No replication log found.");
        }

        try {
            final Node node = (Node) session.getItem(action.getPath());

            JSONObject json = null;
            try (final ResourceResolver resolver = rrf.createResourceResolver()) {
                final Resource resource = resolver.getResource(node.getPath());

                // iterate all registered plugins and delegate the work
                for (ContentBuilderPluginWrapper contentBuilderPluginWrapper : contentHandlerPlugins) {
                    ContentBuilderPlugin contentBuilderPlugin = contentBuilderPluginWrapper.contentBuilderPlugin;
                    if (contentBuilderPlugin.canHandle(action, resource)) {
                        json = contentBuilderPlugin.create(action, resource, json);
                    }
                }
            } catch(DemandwareReplicationException rex) {
                log.error("Error replicating to SFCC: %s", rex.getMessage());
                throw new ReplicationException(rex);
            } catch (JSONException e) {
                log.error("Error creating JSON object: %s", e.getMessage());
                throw new ReplicationException(e);
            }

            // do some validation ...
            if (json == null || json.length() == 0) {
                log.debug("JSON is empty, nothing to replicate");
                return ReplicationContent.VOID;
            }
            if (!json.has(DemandwareCommerceConstants.ATTR_API_TYPE)) {
                throw new ReplicationException(
                        String.format("Invalid JSON, %s attribute missing.", DemandwareCommerceConstants.ATTR_API_TYPE));
            }

            // create the temp file to be delivered by the transport hanlder
            final File file = createJSONFile(json);
            try {
                return factory.create(ContentType.APPLICATION_JSON.getMimeType(), file, true);
            } catch (IOException e) {
                boolean deleted = file.delete();
                log.debug("file {} deleted : {}", file.getAbsolutePath(), deleted);
                throw new ReplicationException(e);
            }

        } catch (AccessControlException e) {
            log.error(String.format("Agent cannot access %s: %s",
                    action.getPath(), e.getMessage()));
            throw new ReplicationException("Agent is unable to access "
                    + action.getPath(), e);
        } catch (RepositoryException e) {
            log.error(String.format("Repository exception occurred during serialization of content %s.",
                    action.getPath()));
            throw new ReplicationException(
                    "RepositoryException during serialization", e);
        } catch (IOException e) {
            log.error(String.format("I/O error occurred during serialization of content %s",
                    action.getPath()));
            throw new ReplicationException("I/O error during serialization", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReplicationContent create(Session session, ReplicationAction action,
                                     ReplicationContentFactory factory, Map<String, Object> parameters)
            throws ReplicationException {
        return create(session, action, factory);
    }

    /**
     * Create JSON content file
     *
     * @param json the JSON string
     * @return the created file
     * @throws IOException if an I/O error occurs
     */
    private File createJSONFile(JSONObject json) throws IOException {
        final File tmpFile = File.createTempFile("demandware", ".json");
        boolean successful = false;
        try {
            FileWriter writer = new FileWriter(tmpFile);
            writer.write(json.toString());
            writer.close();
            successful = true;
            return tmpFile;
        } finally {
            if (!successful) {
                if (!tmpFile.delete()) {
                    throw new IOException("tmp zip file could not be deleted");
                }
            }
        }
    }

    /**
     * Return the name of this content builder.
     *
     * @return name
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Return the title of this content builder.
     *
     * @return title
     */
    @Override
    public String getTitle() {
        return TITLE;
    }

    /*
     * OSGI stuff
     */

    @Activate
    private void activate(Map<String, Object> configuration) {
        this.name = PropertiesUtil.toString(configuration.get(ContentBuilder.PROPERTY_NAME), NAME);
    }

    protected void bindContentHandlerPlugin(final ContentBuilderPlugin contentBuilderPlugin,
                                            final Map<?, ?> properties) {
        final String taskname = (String) properties.get(ContentBuilderPlugin.PN_TASK);
        final int serviceRanking = properties.containsKey(Constants.SERVICE_RANKING) ? (Integer) properties.get(
                Constants.SERVICE_RANKING) : 0;

        contentHandlerPlugins.add(new ContentBuilderPluginWrapper(contentBuilderPlugin, taskname, serviceRanking));
        Collections.sort(contentHandlerPlugins, new Comparator<ContentBuilderPluginWrapper>() {

            @Override
            public int compare(final ContentBuilderPluginWrapper cBPW1,
                               final ContentBuilderPluginWrapper cBPW2) {
                return Integer.compare(cBPW1.serviceRanking, cBPW2.serviceRanking);
            }
        });
    }

    protected void unbindContentHandlerPlugin(final ContentBuilderPlugin contentBuilderPlugin,
                                              final Map<?, ?> properties) {
        final String taskname = (String) properties.get(ContentBuilderPlugin.PN_TASK);
        for (final ContentBuilderPluginWrapper wrapper : contentHandlerPlugins) {
            if (wrapper.taskname.equals(taskname)) {
                contentHandlerPlugins.remove(wrapper);
                break;
            }
        }
    }

    /**
     * Wrapper class for a {@code ContentBuilderPlugin} to keep track of service ranking properties.
     */
    private static class ContentBuilderPluginWrapper {
        private final ContentBuilderPlugin contentBuilderPlugin;
        private final String taskname;
        private final int serviceRanking;

        public ContentBuilderPluginWrapper(final ContentBuilderPlugin contentBuilderPlugin, final String taskname,
                                           final int serviceRanking) {
            this.contentBuilderPlugin = contentBuilderPlugin;
            this.taskname = taskname;
            this.serviceRanking = serviceRanking;
        }
    }
}
