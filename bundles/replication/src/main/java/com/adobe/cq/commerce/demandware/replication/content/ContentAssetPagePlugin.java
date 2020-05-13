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
import com.adobe.cq.commerce.demandware.replication.content.attributemapping.impl.DwreFolderAttributesLookupService;
import com.adobe.cq.commerce.demandware.replication.content.resolution.ContentAssetNameResolver;
import com.adobe.cq.commerce.demandware.replication.content.resolution.LocaleResolver;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.wcm.api.Page;
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

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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

    @Reference(
            cardinality = MANDATORY,
            policy = DYNAMIC,
            policyOption = GREEDY)
    private volatile ContentAssetNameResolver assetNames;

    @Reference(
            cardinality = MANDATORY,
            policy = DYNAMIC,
            policyOption = GREEDY)
    private volatile LocaleResolver localeResolver;

    private String defaultRenderingTemplate;
    private String defaultContentLibrary;
    private List<AttributeDescriptor> attributeDescriptors;

    @Reference(service = AttributeToJsonConverter.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = DYNAMIC,
            policyOption = GREEDY,
            bind = "bindAttrConverter", unbind = "unbindAttrConverter")
    private RankedServices<AttributeToJsonConverter> attributeConverters =
            new RankedServices<>(Order.ASCENDING);

    @Reference
    private DwreFolderAttributesLookupService folderAttributes;

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
        delivery.put(DemandwareCommerceConstants.ATTR_ID, assetNames.resolve(resource));
        delivery.put(DemandwareCommerceConstants.ATTR_LOCALE, localeResolver.resolveOcapiLocaleString(page));

        if (action.getType() == ReplicationActionType.ACTIVATE) {
            // check for folder assignments
            final List<String> dwreFolders = pageProperties.containsKey(DemandwareCommerceConstants.PN_DWRE_FOLDER)
                    ? Arrays.asList(pageProperties.get(DemandwareCommerceConstants.PN_DWRE_FOLDER, String[].class))
                    : Collections.emptyList();
            if (!dwreFolders.isEmpty()) {
                delivery.put(DemandwareCommerceConstants.ATTR_FOLDER, new JSONArray(dwreFolders));
            }

            // construct payload and map page data
            JSONObject pageData = getJSONPayload(delivery);

            // add defaults
            if (StringUtils.isNotEmpty(template)) {
                pageData.put(DemandwareCommerceConstants.ATTR_TEMPLATE, template);
            }

            // map configured page to content asset attributes
            final Set<AttributeDescriptor> attributes = gatherAttributeDescriptors(dwreFolders);
            for (final AttributeDescriptor attribute : attributes) {
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

    private Set<AttributeDescriptor> gatherAttributeDescriptors(List<String> dwreFolders) {
        final Set<AttributeDescriptor> attributes = new HashSet<>(attributeDescriptors);
        folderAttributes.getDescriptors(dwreFolders).stream().forEach(
                attr -> {
                    if(!attributes.add(attr)) {
                        attributes.remove(attr);
                        attributes.add(attr);}
                }
        );
        return attributes;
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
                description = "Page attribute mapping: <JCR property name>;<Demandware attribute name>;<converter ID>:<default value>. Last two parameters are optional."
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
