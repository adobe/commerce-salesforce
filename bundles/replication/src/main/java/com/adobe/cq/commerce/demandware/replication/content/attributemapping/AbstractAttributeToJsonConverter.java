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

package com.adobe.cq.commerce.demandware.replication.content.attributemapping;

import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;


/**
 * Basic implementation for attribute converters.
 *
 * Respects default values.
 */
public abstract class AbstractAttributeToJsonConverter implements AttributeToJsonConverter {

    @Override
    public Object createAttributeJson(final AttributeDescriptor attr,
                                      final Page page,
                                      final HierarchyNodeInheritanceValueMap properties)
            throws JSONException {
        final Object value = fixStringValue(getValue(attr, page, properties));
        return isMultivalued()
                ? createMultiValueJSONObject(getMultivalueKey(attr, page, properties), value)
                : value;
    }

    /**
     * Obtains value for this attribute conversion.
     *
     * The default implementation simply copies the input value to the output
     * of if there is no input value the default value to the output.
     *
     * @param attr
     * @param page
     * @param properties
     * @return
     */
    protected Object getValue(final AttributeDescriptor attr,
                              final Page page,
                              final HierarchyNodeInheritanceValueMap properties) {
        final Object value = properties.get(attr.getSourceName());
        return value==null
                ? attr.getDefaultValue()
                : value;
    }

    protected String getMultivalueKey(AttributeDescriptor attr,
                                      final Page page,
                                      HierarchyNodeInheritanceValueMap properties) {
        throw new UnsupportedOperationException(
                getClass().getCanonicalName() + ".getMultivalueKey has not been implemented.");
    }


    /**
     * If this method returns true the return value of createAttributeJson will
     * be a JSONObject containing a dictionary. Otherwise createAttributeJson
     * will directly create the property value in question.
     *
     * @return boolean indicating whether this the resulting JSON is supposed
     * to be a dictionary.
     */
    protected boolean isMultivalued() {
        return false;
    }

    /**
     * Create "default" JSON object used for Demandware JSON object
     *
     * @param key   the JSON object key
     * @param value the JSON object value
     * @return the value encapsulated into default object
     * @throws JSONException in case JSON parsing fails
     */
    protected JSONObject createMultiValueJSONObject(String key, Object value)
            throws JSONException {

        return new JSONObject().put(
                defaultIfEmpty(key, "default"), value);
    }

    /**
     * If value is a string and starts with either 'true' or 'false'
     * convert it to a Boolean. Otherwise leave untouched.
     *
     * Override to support other boolean representations or add other
     * conversions to support arbitrary representations of value.
     *
     * @param value
     * @return
     */
    protected Object fixStringValue(Object value) {
        if (value instanceof String) {
            final String strValue = (String) value;
            if (StringUtils.startsWithAny(StringUtils.lowerCase(strValue), "true", "false")) {
                return Boolean.valueOf(strValue);
            }
        }
        return value;
    }

    @Override
    public boolean canHandle(AttributeDescriptor attr,
                             Page page,
                             HierarchyNodeInheritanceValueMap properties) {
        return isNotEmpty(attr.getDefaultValue())
                || properties.containsKey(attr.getSourceName());
    }
}
