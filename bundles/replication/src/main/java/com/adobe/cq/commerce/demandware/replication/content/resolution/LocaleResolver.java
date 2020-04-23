/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe Systems Incorporated and others
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
package com.adobe.cq.commerce.demandware.replication.content.resolution;

import com.day.cq.wcm.api.Page;

import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.replaceChars;

public interface LocaleResolver {

    /**
     * Resolve the locale for a given page.
     *
     * @param page The page to look up the locale for.
     * @return
     */
    Locale resolveLocale(Page page);


    /**
     * Standard implementation to format the resolved locale in ocapi format
     * using formatForOcapi.
     * If the locale is null this method will return null.
     * @param page The page to look up the OCAPI formatted locale for.
     * @return
     */
    default String resolveOcapiLocaleString(Page page) {
        final Locale locale = resolveLocale(page);
        return locale==null
                ? null
                : formatForOcapi(locale);
    }


    /**
     * Format a Java locale for usage with Salesforce CC OCAPI.<br>
     *
     * The OCAPI accepts locale's formatted like "de-DE", using
     * dash, while Java uses the underscore.<br>
     *
     * Thus this default implementation replaces underscores by dashes.
     *
     * @param locale the {@code Locale}
     * @return the formatted locale string
     * @throws IllegalArgumentException if locale is null
     */
    default String formatForOcapi(final Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("'locale' must not be null");
        }

        return replaceChars(locale.toString(), "_", "-");
    }
}
