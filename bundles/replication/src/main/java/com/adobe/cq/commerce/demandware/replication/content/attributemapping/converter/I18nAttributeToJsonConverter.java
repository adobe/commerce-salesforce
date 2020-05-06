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

package com.adobe.cq.commerce.demandware.replication.content.attributemapping.converter;

import com.adobe.cq.commerce.demandware.replication.content.attributemapping.AbstractAttributeToJsonConverter;
import com.adobe.cq.commerce.demandware.replication.content.attributemapping.AttributeDescriptor;
import com.adobe.cq.commerce.demandware.replication.content.attributemapping.AttributeToJsonConverter;
import com.adobe.cq.commerce.demandware.replication.content.resolution.LocaleResolver;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.wcm.api.Page;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

@Component(service = AttributeToJsonConverter.class, immediate = true,
        property = {
                "service.ranking:Integer=0"
        })
public class I18nAttributeToJsonConverter extends AbstractAttributeToJsonConverter {

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = DYNAMIC, policyOption = GREEDY)
    private volatile LocaleResolver localeResolver;

    @Override
    public boolean canHandle(AttributeDescriptor attr, Page page, HierarchyNodeInheritanceValueMap properties) {
        return super.canHandle(attr, page, properties)
                && equalsIgnoreCase("i18n", attr.getConverterId());
    }

    @Override
    protected boolean isMultivalued() {
        return true;
    }

    @Override
    protected String getMultivalueKey(final AttributeDescriptor attr,
                                      final Page page,
                                      final HierarchyNodeInheritanceValueMap properties) {
        return localeResolver.resolveOcapiLocaleString(page);
    }

    @Override
    protected Object getValue(final AttributeDescriptor attr,
                              final Page page,
                              final HierarchyNodeInheritanceValueMap properties) {
        final Object value = properties.get(attr.getSourceName());
        return value==null
                ? attr.getDefaultValue()
                : value;
    }
}
