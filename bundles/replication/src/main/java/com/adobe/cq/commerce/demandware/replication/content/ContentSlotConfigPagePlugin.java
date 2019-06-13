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
import java.util.Date;
import java.util.Dictionary;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
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

/**
 * <code>ContentBuilderPlugin</code> to export and map a AEM pages for content slot configurations of the configured
 * resource type to a Demandware content slot configurations.
 */
@Component(label = "Demandware ContentBuilder Plugin for Page > Content Slot Configuration transformation", policy =
    ConfigurationPolicy.REQUIRE, immediate = true, metatype = true)
@Service(value = ContentBuilderPlugin.class)
@Properties({@Property(name = ContentBuilderPlugin.PN_TASK, value = "ContentSlotConfigPagePlugin", propertyPrivate =
    true),
    @Property(name = Constants.SERVICE_RANKING, intValue = 20)})
public class ContentSlotConfigPagePlugin extends AbstractContentBuilderPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(ContentSlotConfigPagePlugin.class);


    private static final String DEFAULT_CONTENT_SLOT_CONFIG_API = "/sites/{site_id}/slots/{slot_id}/slot_configurations/{id}";

    @Property(label = "Content Slot Configuration Resource Types", description = "List of Sling resource types which " +
        "this plugin is applied", cardinality = Integer.MAX_VALUE)
    private static final String CONTENT_ASSET_RESOURCE_TYPES = "resourcetypes.supported";

    @Property(label = "Default OCAPI API Path", description = "The path of the OCAPI content asset API", value =
        DEFAULT_CONTENT_SLOT_CONFIG_API)
    private static final String CONTENT_SLOT_CONFI_API = "api";

    @Override
    public JSONObject create(final ReplicationAction action, Resource resource, JSONObject content)
        throws JSONException {
        JSONObject delivery = content;
        if (delivery == null) {
            delivery = new JSONObject();
        }

        // map page attributes
        final Page page = resource.adaptTo(Page.class);
        LOG.debug("Transform page {} into content content slot configuration", page.getPath());
        final ValueMap pageProperties = new HierarchyNodeInheritanceValueMap(page.getContentResource());

        // get base values for site, library and language attributes
        final String language = getLanguage(page);
        final String site = ((HierarchyNodeInheritanceValueMap) pageProperties).getInherited(
            DemandwareCommerceConstants.PN_DWRE_SITE, String.class);
        final String slotType = pageProperties.get(DemandwareCommerceConstants.PN_DWRE_SLOT_TYPE, String.class);

        // add meta data
        addAPIType(DemandwareCommerceConstants.TYPE_OCAPI, delivery);
        addPayloadType("content-slot-config", delivery);
        delivery.put(DemandwareCommerceConstants.ATTR_API_ENDPOINT, api);
        delivery.put(DemandwareCommerceConstants.ATTR_ID, resource.getName());
        delivery.put(DemandwareCommerceConstants.ATTR_SLOT,
            pageProperties.get(DemandwareCommerceConstants.PN_DWRE_SLOT_ID, String.class));
        delivery.put(DemandwareCommerceConstants.ATTR_SITE, site);

        // add context information
        if (StringUtils.equals(slotType, "category")) {
            delivery.put(DemandwareCommerceConstants.ATTR_CONTEXT, slotType + "=" + pageProperties.get
                (DemandwareCommerceConstants.PN_DWRE_SLOT_CATEGORY_ID, String.class));
        } else if (StringUtils.equals(slotType, "folder")) {
            delivery.put(DemandwareCommerceConstants.ATTR_CONTEXT, slotType + "=" + pageProperties.get
                (DemandwareCommerceConstants.PN_DWRE_SLOT_FOLDER_ID, String.class));
        }


        if (action.getType() == ReplicationActionType.ACTIVATE) {
            // construct payload and map page data
            final JSONObject slotConfigData = getJSONPayload(delivery);

            // add common data
            slotConfigData.put(DemandwareCommerceConstants.ATTR_CONFIGURATION, resource.getName());
            slotConfigData.put("default", pageProperties.get("dwreDefault", false));
            slotConfigData.put("enabled", pageProperties.get("dwreEnabled", false));
            slotConfigData.put(DemandwareCommerceConstants.ATTR_SLOT_RANK, pageProperties.get("dwreSlotRank", Integer.class));
            slotConfigData.put(DemandwareCommerceConstants.ATTR_DESCRIPTION, pageProperties.get("jcr:description", String.class));
            slotConfigData.put(DemandwareCommerceConstants.ATTR_TEMPLATE, pageProperties.get(DemandwareCommerceConstants
                .PN_DWRE_TEMPLATE_PATH, String.class));
            slotConfigData.put(DemandwareCommerceConstants.ATTR_SLOT_CALLOUT_MSG, createMarkupTextJSONObject(pageProperties.get
                ("dwreSlotCallout", String.class), language));

            // add slot content
            final JSONObject slotContentData = getSlotContentData(pageProperties, language);
            slotConfigData.put(DemandwareCommerceConstants.ATTR_SLOT_CONTENT, slotContentData);

            // add slot schedule information
            final JSONObject slotScheduleData = getSlotScheduleData(pageProperties);
            slotConfigData.put(DemandwareCommerceConstants.ATTR_SLOT_SCHEDULE, slotScheduleData);

            // add customer groups to slot schedule information
            if (pageProperties.containsKey("dwreSlotScheduleCustomerGroups")) {
                slotConfigData.put(DemandwareCommerceConstants.ATTR_SLOT_CUSTOMER_GROUPS, new JSONArray(
                    Arrays.asList(pageProperties.get("dwreSlotScheduleCustomerGroups", String[].class))));
            }

            delivery.put(DemandwareCommerceConstants.ATTR_PAYLOAD, slotConfigData);
        }
        LOG.debug("Delivery for page {}: {}", page.getPath(), delivery.toString());
        return delivery;
    }

    private JSONObject getSlotContentData(final ValueMap pageProperties, final String language) throws JSONException {
        final JSONObject slotContentData = new JSONObject();
        final String slotContentType = pageProperties.get(DemandwareCommerceConstants.PN_DWRE_SLOT_CONTENT_TYPE,
            String.class);
        slotContentData.put(DemandwareCommerceConstants.ATTR_SLOT_CONTENT_TYPE, slotContentType);
        switch (slotContentType) {
            case "html":
                slotContentData.put("body", createMarkupTextJSONObject(pageProperties.get("dwreSlotHTML", String.class), language));
                break;
            case "products":
                String[] productPaths = pageProperties.get("dwreSlotProducts", String[].class);
                if (productPaths != null) {
                    for (int i = 0; i < productPaths.length; i++) {
                        productPaths[i] = StringUtils.substringAfterLast(productPaths[i], "/");
                    }
                    slotContentData.put("product_ids", new JSONArray(Arrays.asList(productPaths)));
                }
                break;
            case "categories":
                slotContentData.put("category_ids", new JSONArray(Arrays.asList(pageProperties.get
                    ("dwreSlotCategories", String[].class))));
                break;
            case "content_assets":
                slotContentData.put("content_asset_ids", new JSONArray(Arrays.asList(pageProperties.get
                    ("dwreSlotContentAssets", String[].class))));
                break;
        }
        return slotContentData;
    }

    private JSONObject getSlotScheduleData(final ValueMap pageProperties) throws JSONException {
        final JSONObject slotScheduleData = new JSONObject();
        if (pageProperties.containsKey("onTime")) {
            final Date from = pageProperties.get("onTime", Date.class);
            slotScheduleData.put("start_date",
                DateFormatUtils.format(from, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern()));
        }
        if (pageProperties.containsKey("offTime")) {
            final Date to = pageProperties.get("offTime", Date.class);
            slotScheduleData.put("end_date",
                DateFormatUtils.format(to, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern()));
        }

        final JSONObject recurrenceData = new JSONObject();
        if (pageProperties.containsKey("dwreSlotScheduleDayOfWeek")) {
            recurrenceData.put("day_of_week",
                new JSONArray(Arrays.asList(pageProperties.get("dwreSlotScheduleDayOfWeek", String[].class))));
        }
        if (pageProperties.containsKey("dwreSlotScheduleFromTime") && pageProperties.containsKey(
            "dwreSlotScheduleToTime")) {
            final Date from = pageProperties.get("dwreSlotScheduleFromTime", Date.class);
            final Date to = pageProperties.get("dwreSlotScheduleToTime", Date.class);
            final JSONObject timeOfDayData = new JSONObject();
            timeOfDayData.put("time_from", DateFormatUtils.format(from, DateFormatUtils.ISO_TIME_NO_T_FORMAT
                .getPattern()));
            timeOfDayData.put("time_to", DateFormatUtils.format(to, DateFormatUtils.ISO_TIME_NO_T_FORMAT.getPattern()));
            recurrenceData.put("time_of_day", timeOfDayData);
        }
        if (recurrenceData.length() > 0) {
            slotScheduleData.put("recurrence", recurrenceData);
        }
        return slotScheduleData;
    }


    @Activate
    protected void activate(final ComponentContext ctx) {
        final Dictionary<?, ?> config = ctx.getProperties();
        if (config.get(CONTENT_ASSET_RESOURCE_TYPES) != null) {
            supportedResourceTypes = Arrays.asList(
                PropertiesUtil.toStringArray(config.get(CONTENT_ASSET_RESOURCE_TYPES)));
        }
        api = PropertiesUtil.toString(config.get(CONTENT_SLOT_CONFI_API), DEFAULT_CONTENT_SLOT_CONFIG_API);
    }
}
