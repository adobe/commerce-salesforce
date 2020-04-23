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
package com.adobe.cq.commerce.demandware.replication.content.resolution.impl;

import com.adobe.cq.commerce.demandware.replication.content.resolution.LocaleResolver;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import java.util.Locale;

@Component(
        service=LocaleResolver.class,
        property = Constants.SERVICE_RANKING + ":Integer=0"
)
public class DefaultLocaleResolver implements LocaleResolver {

    @Override
    public Locale resolveLocale(Page page) {
        final HierarchyNodeInheritanceValueMap inheritedValues
                = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        return StringUtils.isNotEmpty(inheritedValues.getInherited(JcrConstants.JCR_LANGUAGE, String.class))
                ? page.getLanguage(false)
                : null;
    }

}
