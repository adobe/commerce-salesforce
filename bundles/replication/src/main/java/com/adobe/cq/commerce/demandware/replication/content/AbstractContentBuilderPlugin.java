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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;

import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.replication.ContentBuilderPlugin;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.wcm.api.Page;

/**
 * Abstract {@code ContentBuilderPlugin} sharing a common set a of methods used by most plugins.
 */
public abstract class AbstractContentBuilderPlugin implements ContentBuilderPlugin {

    protected List<String> supportedResourceTypes;
    protected List<String> ignoredResourceTypes;
    protected String api;

    @Override
    public boolean canHandle(final ReplicationAction action, final Resource resource) {
        if (resource != null) {
            final Resource contentResource = resource.getChild(JcrConstants.JCR_CONTENT);
            if (contentResource != null) {
                // check if current resource on the resource type ignore list
                if (ignoredResourceTypes != null) {
                    for (String ignoredResourceType : ignoredResourceTypes) {
                        if (contentResource.isResourceType(ignoredResourceType)) {
                            return false;
                        }
                    }
                }

                // check if current resource is on the supported page resource type white list
                if (supportedResourceTypes != null) {
                    for (String supportedResourceType : supportedResourceTypes) {
                        if (contentResource.isResourceType(supportedResourceType)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected void addAPIType(final String type, final JSONObject delivery) throws JSONException {
        if (!delivery.has(DemandwareCommerceConstants.ATTR_API_TYPE)) {
            delivery.put(DemandwareCommerceConstants.ATTR_API_TYPE, type);
        }
    }

    protected void addPayloadType(final String type, final JSONObject delivery) throws JSONException {
        if (!delivery.has(DemandwareCommerceConstants.ATTR_CONTENT_TYPE)) {
            delivery.put(DemandwareCommerceConstants.ATTR_CONTENT_TYPE, type);
        }
    }

    protected JSONObject getJSONPayload(JSONObject delivery) {
        JSONObject pageData = delivery.optJSONObject(DemandwareCommerceConstants.ATTR_PAYLOAD);
        if (pageData == null) {
            pageData = new JSONObject();
        }
        return pageData;
    }

    /**
     * Get the correct content language in the correct format to be used with OCAPI.
     *
     * @param page the current page
     * @return the language for the give page
     */
    protected final String getLanguage(final Page page) {
        return StringUtils.isNotEmpty(
            new HierarchyNodeInheritanceValueMap(page.getContentResource()).getInherited(JcrConstants.JCR_LANGUAGE,
                String.class)) ? getFormattedLocale(page.getLanguage(false)) : null;
    }

    /**
     * Format a Java locale for usage with Demandware OCAPI. The OCAPI accepts locale's formatted like "de-DE", using
     * dash instead of underscore.
     *
     * @param locale the {@code Locale}
     * @return the formatted locale string
     */
    protected final String getFormattedLocale(final Locale locale) {
        if (locale != null) {
            return StringUtils.replaceChars(locale.toString(), "_", "-");
        }
        return null;
    }

    /**
     * Create "default" JSON object used for Demandware JSON object
     *
     * @param key   the JSON object key
     * @param value the JSON object value
     * @return the value encapsulated into default object
     * @throws JSONException in case JSON parsing fails
     */
    protected JSONObject createMultiValueJSONObject(String key, Object value) throws JSONException {
        return new JSONObject().put(StringUtils.defaultIfEmpty(key, "default"), ensureBoolean(value));
    }

    /**
     * Create MarkupText JSON object used for various Demandware richt text fields JSON object
     *
     * @param markupContent the markup source value
     * @param languageKey   the language key, will fall back to "default" if null
     * @return the value encapsulated into default object
     * @throws JSONException in case JSON parsing fails
     */
    protected JSONObject createMarkupTextJSONObject(final Object markupContent, final String languageKey) throws JSONException {
        final JSONObject markupText = new JSONObject();
        markupText.put(DemandwareCommerceConstants.ATTR_BODY_TYPE, DemandwareCommerceConstants.ATTR_BODY_TYPE_TEXT);
        markupText.put(DemandwareCommerceConstants.ATTR_BODY_SOURCE, ensureBoolean(markupContent));
        return new JSONObject().put(StringUtils.defaultIfEmpty(languageKey, "default"), markupText);
    }


    protected Object ensureBoolean(Object value) {
        if (value instanceof String) {
            final String strValue = (String) value;
            if (StringUtils.startsWithAny(StringUtils.lowerCase(strValue), "true", "false")) {
                return Boolean.valueOf(strValue);
            }
        }
        return value;
    }

    /**
     * Helper to ease service config mapping.
     *
     * @param configKey the config key
     * @param ctx       the {@code ComponentContext}
     * @return the mapped config attribute values
     */
    protected Map<String, String> setupMapping(final String configKey, final ComponentContext ctx) {
        final Map<String, String> result = new HashMap<>();
        final String[] mapping = PropertiesUtil.toStringArray(ctx.getProperties().get(configKey));
        for (final String s : mapping) {
            final String[] components = StringUtils.split(s, ';');
            if (components.length >= 2) {
                if (components.length == 3) {
                    result.put(components[0], components[1] + ";" + components[2]);
                } else {
                    result.put(components[0], components[1]);
                }
            }
        }
        return result;
    }
}
