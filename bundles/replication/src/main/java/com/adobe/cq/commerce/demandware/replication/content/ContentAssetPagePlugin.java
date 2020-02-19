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

import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.replication.ContentBuilderPlugin;
import com.adobe.cq.commerce.demandware.replication.content.attributemapping.AttributeDescriptor;
import com.adobe.cq.commerce.demandware.replication.content.attributemapping.AttributeToJsonConverter;
import com.adobe.cq.commerce.demandware.replication.content.attributemapping.impl.AttributeDescriptorFactory;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * <code>ContentBuilderPlugin</code> to export and map a AEM pages of the configured resource type to a Demandware
 * Content Asset. This plugin maps all configured page properties to the appropriate attribute of the JSON payload.
 * The plugin also sets the OCAPI endpoint and meta information for replication content assets.
 * This plugin will not render any body attribute.
 */
@Component(service = ContentBuilderPlugin.class, immediate = true,
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property= {
                   ContentBuilderPlugin.PN_TASK + "=ContentAssetPagePlugin",
                   Constants.SERVICE_RANKING + ":Integer=10"
           })
@Designate(ocd=ContentAssetPagePlugin.Configuration.class)
public class ContentAssetPagePlugin extends AbstractContentBuilderPlugin {

    private static final Logger LOG = LoggerFactory
            .getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());



    @Reference
    LiveRelationshipManager liveRelationshipManager;

    private String defaultRenderingTemplate;
    private String defaultContentLibrary;
    private List<AttributeDescriptor> attributeDescriptors;

    @Reference(service = AttributeToJsonConverter.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            bind = "bindAttrConverter", unbind = "unbindAttrConverter")
    private RankedServices<AttributeToJsonConverter> attributeConverters =
            new RankedServices<>(Order.ASCENDING);

    @Override
    public JSONObject create(final ReplicationAction action, Resource resource, JSONObject content)
        throws JSONException {
        JSONObject delivery = content;
        if (delivery == null) {
            delivery = new JSONObject();
        }

        // map page attributes
        final Page page = resource.adaptTo(Page.class);
        LOG.debug("Transform page {} into content asset", page.getPath());
        final HierarchyNodeInheritanceValueMap pageProperties =
                new HierarchyNodeInheritanceValueMap(page.getContentResource());

        // get base values for site, library and language attributes
        final String language = getLanguage(page);
        final String site = pageProperties.getInherited(
            DemandwareCommerceConstants.PN_DWRE_SITE, String.class);
        final String library = pageProperties.getInherited(
            DemandwareCommerceConstants.PN_DWRE_LIBRARY, String.class);
        // get DWRE template for page
        String template = pageProperties.getInherited(
                DemandwareCommerceConstants.PN_DWRE_TEMPLATE_PATH, defaultRenderingTemplate);
        if (StringUtils.isNotEmpty(template)) {
            template = StringUtils.appendIfMissing(template, ".vs", ".vs");
        }

        // add meta data
        addAPIType(DemandwareCommerceConstants.TYPE_OCAPI, delivery);
        addPayloadType("content-asset", delivery);
        delivery.put(DemandwareCommerceConstants.ATTR_API_ENDPOINT, api);
        delivery.put(DemandwareCommerceConstants.ATTR_LIBRARY, StringUtils.defaultIfEmpty(library,
            defaultContentLibrary));
        delivery.put(DemandwareCommerceConstants.ATTR_ID, getContentAssetName(resource));

        if (action.getType() == ReplicationActionType.ACTIVATE) {
            // check for folder assignments
            if (pageProperties.containsKey(DemandwareCommerceConstants.PN_DWRE_FOLDER)) {
                delivery.put(DemandwareCommerceConstants.ATTR_FOLDER,
                        new JSONArray(Arrays.asList(pageProperties.get(DemandwareCommerceConstants.PN_DWRE_FOLDER, String[].class))));
            }

            // construct payload and map page data
            JSONObject pageData = getJSONPayload(delivery);

            // add defaults
            if (StringUtils.isNotEmpty(template)) {
                pageData.put(DemandwareCommerceConstants.ATTR_TEMPLATE, template);
            }

            // map configured page to content asset attributes
            for (final AttributeDescriptor attribute : attributeDescriptors) {
                final AttributeToJsonConverter converter =
                        findConverter(attribute, page, pageProperties);
                if(converter!=null) {
                    pageData.put(attribute.getTargetName(),
                            converter.createAttributeJson(attribute, page, pageProperties));
                }
            }

            delivery.put(DemandwareCommerceConstants.ATTR_PAYLOAD, pageData);
        }
        LOG.debug("Delivery for page {}: {}", page.getPath(), delivery.toString());
        return delivery;
    }

    private AttributeToJsonConverter findConverter(AttributeDescriptor attr,
                                                   Page page,
                                                   HierarchyNodeInheritanceValueMap pageProperties)
    {
        for(AttributeToJsonConverter conv: attributeConverters) {
            if(conv.canHandle(attr, page, pageProperties)) {
                return conv;
            }
        }
        return null;
    }

    /**
     * Get the content asset name based on the page name of the live relation source page (if there is one) or
     * current page resource name. <br/>
     * If we have a multi language / multi region site we always use the name of the source page as id for the
     * content asset, as we need to bring all language pages down to one content asset within Demandware.
     *
     * @param resource the current page resource
     * @return the conten asset name
     */
    private String getContentAssetName(Resource resource) {
        if (liveRelationshipManager.hasLiveRelationship(resource)) {
            try {
                LiveRelationship lr = liveRelationshipManager.getLiveRelationship(resource, false);
                return StringUtils.substringAfterLast(lr.getSourcePath(), "/");
            } catch (WCMException wcme) {
                LOG.debug("Error retrieving live relationship", wcme);
            }
        }
        return resource.getName();
    }


    @Activate
    private void activate(final Configuration config) {
        supportedResourceTypes = ArrayUtils.isEmpty(config.resourcetypes_supported())
                ? Collections.emptyList()
                : Arrays.asList(config.resourcetypes_supported());
        LOG.debug("Config - content asset resource types: {}",
                StringUtils.join(supportedResourceTypes));

        ignoredResourceTypes = ArrayUtils.isEmpty(config.resourcetypes_ignored())
                ? Collections.emptyList()
                : Arrays.asList(config.resourcetypes_ignored());
        LOG.debug("Config - ignored resource types: {}", StringUtils.join(ignoredResourceTypes));

        api = config.api();
        defaultContentLibrary = config.default_library();
        defaultRenderingTemplate = config.default_template();
        attributeDescriptors = new AttributeDescriptorFactory()
                .fromOsgiConfig(config.attributes_mapping());
    }

    private void bindAttrConverter(AttributeToJsonConverter conv, Map<String, Object> props) {
        attributeConverters.bind(conv, props);
    }

    private void unbindAttrConverter(AttributeToJsonConverter conv, Map<String, Object> props) {
        attributeConverters.unbind(conv, props);
    }


    @ObjectClassDefinition(name = "Demandware ContentBuilder Plugin for Page > Content Asset transformation")
    @interface Configuration {
        @AttributeDefinition(
                name = "Content Asset Resource Types",
                description = "List of Sling resource types which this plugin is applied"
        )
        String[] resourcetypes_supported() default {};

        @AttributeDefinition(
                name = "Ignored Resource Types",
                description = "Optional list of Sling resource types which are ignored by this plugin"
        )
        String[] resourcetypes_ignored() default {};

        @AttributeDefinition(
                name = "Default Content Library",
                description = "The default library used for the content assets"
        )
        String default_library() default "";

        @AttributeDefinition(
                name = "Default Rendering Template",
                description = "The default rendering template used if no site/page specific is available"
        )
        String default_template() default "";

        @AttributeDefinition(
                name = "Default OCAPI API Pathe",
                description = "The path of the OCAPI content asset API"
        )
        String api() default "/libraries/{library_id}/content/{id}";

        @AttributeDefinition(
                name = "Attribute Mapping",
                description = "Page attribute mapping: <JCR property name>;<Demandware attribute name>;<optional type> for multi value fields i18n or site"
        )
        String[] attributes_mapping() default {
                "jcr:title;name;i18n",
                "pageTitle;page_title;i18n",
                "jcr:description;description;i18n",
                "dwreOnline;online;site",
                "dwreSearchable;searchable;site"
        };
    }
}
