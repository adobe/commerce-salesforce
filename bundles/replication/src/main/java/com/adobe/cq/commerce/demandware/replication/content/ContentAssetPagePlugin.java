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

package com.adobe.cq.commerce.demandware.replication.content;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.replication.ContentBuilderPlugin;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

/**
 * <code>ContentBuilderPlugin</code> to export and map a AEM pages of the configured resource type to a Demandware
 * Content Asset. This plugin maps all configured page properties to the appropriate attribute of the JSON payload.
 * The plugin also sets the OCAPI endpoint and meta information for replication content assets.
 * This plugin will not render any body attribute.
 */
@Component(label = "Demandware ContentBuilder Plugin for Page > Content Asset transformation", policy =
    ConfigurationPolicy.REQUIRE, immediate = true, metatype = true)
@Service(value = ContentBuilderPlugin.class)
@Properties({@Property(name = ContentBuilderPlugin.PN_TASK, value = "ContentAssetPagePlugin", propertyPrivate = true),
    @Property(name = Constants.SERVICE_RANKING, intValue = 10)})
public class ContentAssetPagePlugin extends AbstractContentBuilderPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(ContentAssetPagePlugin.class);
    private static final String DEFAULT_CONTENT_ASSET_API = "/libraries/{library_id}/content/{id}";

    @Property(label = "Content Asset Resource Types", description = "List of Sling resource types which this plugin " +
        "is applied", cardinality = Integer.MAX_VALUE)
    private static final String CONTENT_ASSET_RESOURCE_TYPES = "resourcetypes.supported";

    @Property(label = "Ignored Resource Types", description = "Optional list of Sling resource types which are " +
        "ignored by this plugin", cardinality = Integer.MAX_VALUE)
    private static final String IGNORED_RESOURCE_TYPES = "resourcetypes.ignored";

    @Property(label = "Default Content Library", description = "The default library used for the content assets")
    private static final String CONTENT_ASSET_LIBRARY = "default.library";

    @Property(label = "Default Rendering Template", description = "The default rendering template used if no site / " +
        "page specific is available")
    private static final String CONTENT_ASSET_TEMPLATE = "default.template";

    @Property(label = "Default OCAPI API Path", description = "The path of the OCAPI content asset API", value =
        DEFAULT_CONTENT_ASSET_API)
    private static final String CONTENT_ASSET_API = "api";

    @Property(value = {"jcr:title;name;i18n", "pageTitle;page_title;i18n", "jcr:description;description;i18n",
        "dwreOnline;online;site", "dwreSearchable;searchable;site"},
        label = "Attribute Mapping", description = "Page attribute mapping: <JCR property name>;<Demandware " +
        "attribute name>;<optional type> for multi value fields i18n or site")
    private static final String CONTENT_ASSEST_ATTRIBUTE_MAPPING = "attributes.mapping";

    @Reference
    LiveRelationshipManager liveRelationshipManager;

    private String defaultRenderingTemplate;
    private String defaultContentLibrary;
    private Map<String, String> attributeMapping;

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
        final ValueMap pageProperties = new HierarchyNodeInheritanceValueMap(page.getContentResource());

        // get base values for site, library and language attributes
        final String language = getLanguage(page);
        final String site = ((HierarchyNodeInheritanceValueMap) pageProperties).getInherited(
            DemandwareCommerceConstants.PN_DWRE_SITE, String.class);
        final String library = ((HierarchyNodeInheritanceValueMap) pageProperties).getInherited(
            DemandwareCommerceConstants.PN_DWRE_LIBRARY, String.class);
        // get DWRE template for page
        String template = ((HierarchyNodeInheritanceValueMap) pageProperties)
            .getInherited(DemandwareCommerceConstants.PN_DWRE_TEMPLATE_PATH, defaultRenderingTemplate);
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
                delivery.put(DemandwareCommerceConstants.ATTR_FOLDER, new JSONArray(Arrays.asList(pageProperties.get
                    (DemandwareCommerceConstants.PN_DWRE_FOLDER, String[].class))));
            }

            // construct payload and map page data
            JSONObject pageData = getJSONPayload(delivery);

            // add defaults
            if (StringUtils.isNotEmpty(template)) {
                pageData.put(DemandwareCommerceConstants.ATTR_TEMPLATE, template);
            }

            // map configured page to content asset attributes
            for (final Map.Entry<String, String> attribute : attributeMapping.entrySet()) {
                if (pageProperties.containsKey(attribute.getKey())) {
                    final String attrValue = attribute.getValue();
                    if (StringUtils.contains(attrValue, ";")) {
                        final String type = StringUtils.substringAfter(attrValue, ";");
                        switch (type) {
                            case "i18n":
                                pageData.put(StringUtils.substringBefore(attrValue, ";"),
                                    createMultiValueJSONObject(language, pageProperties.get(attribute.getKey())));
                                break;
                            case "site":
                                pageData.put(StringUtils.substringBefore(attrValue, ";"),
                                    createMultiValueJSONObject(site, pageProperties.get(attribute.getKey())));
                                break;
                            default:
                                pageData.put(StringUtils.substringBefore(attrValue, ";"),
                                    pageProperties.get(attribute.getKey()));
                        }
                    } else {
                        pageData.put(attrValue, pageProperties.get(attribute.getKey()));
                    }
                }
            }
            delivery.put(DemandwareCommerceConstants.ATTR_PAYLOAD, pageData);
        }
        LOG.debug("Delivery for page {}: {}", page.getPath(), delivery.toString());
        return delivery;
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
    protected void activate(final ComponentContext ctx) {
        final Dictionary<?, ?> config = ctx.getProperties();
        if (config.get(CONTENT_ASSET_RESOURCE_TYPES) != null) {
            supportedResourceTypes = Arrays.asList(
                PropertiesUtil.toStringArray(config.get(CONTENT_ASSET_RESOURCE_TYPES)));
            LOG.debug("Config - content asset resource types: {}", StringUtils.join(supportedResourceTypes));
        }
        if (config.get(IGNORED_RESOURCE_TYPES) != null) {
            ignoredResourceTypes = Arrays.asList(PropertiesUtil.toStringArray(config.get(IGNORED_RESOURCE_TYPES)));
            LOG.debug("Config - ignored resource types: {}", StringUtils.join(ignoredResourceTypes));
        }
        api = PropertiesUtil.toString(config.get(CONTENT_ASSET_API), DEFAULT_CONTENT_ASSET_API);
        defaultContentLibrary = PropertiesUtil.toString(config.get(CONTENT_ASSET_LIBRARY), StringUtils.EMPTY);
        defaultRenderingTemplate = PropertiesUtil.toString(config.get(CONTENT_ASSET_TEMPLATE), StringUtils.EMPTY);
        attributeMapping = setupMapping(CONTENT_ASSEST_ATTRIBUTE_MAPPING, ctx);
    }
}
