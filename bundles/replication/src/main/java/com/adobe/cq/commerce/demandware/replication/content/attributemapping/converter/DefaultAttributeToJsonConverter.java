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
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component(service = AttributeToJsonConverter.class, immediate = true,
           property = {
               "service.ranking:Integer=10000"
           })
public class DefaultAttributeToJsonConverter extends AbstractAttributeToJsonConverter {

    @Override
    public boolean canHandle(AttributeDescriptor attr, Page page, HierarchyNodeInheritanceValueMap properties) {
        return super.canHandle(attr, page, properties)
                && (isEmpty(attr.getConverterId()) || equalsIgnoreCase(attr.getConverterId(),"default"));
    }
}
