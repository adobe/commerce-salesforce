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
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.commons.json.JSONException;

import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;


public interface AttributeToJsonConverter {

    boolean canHandle(AttributeDescriptor attr, Page page, HierarchyNodeInheritanceValueMap properties);

    Object createAttributeJson(AttributeDescriptor attr,
                               Page page,
                               HierarchyNodeInheritanceValueMap properties)
            throws JSONException;

    /**
     * Get the correct content language in the correct format to be used with OCAPI.
     *
     * @param page the current page
     * @return the language for the give page
     */
    default String getLanguage(final Page page,
                               final HierarchyNodeInheritanceValueMap properties)
    {
        return isNotEmpty(properties.getInherited(JcrConstants.JCR_LANGUAGE, String.class))
                ? format(page.getLanguage(false))
                : null;
    }

    /**
     * Format a Java locale for usage with Demandware OCAPI. The OCAPI accepts locale's formatted like "de-DE", using
     * dash instead of underscore.
     *
     * @param locale the {@code Locale}
     * @return the formatted locale string
     */
    default String format(final Locale locale) {
        if (locale != null) {
            return StringUtils.replaceChars(locale.toString(), "_", "-");
        }
        return null;
    }
}
