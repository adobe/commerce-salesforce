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

package com.adobe.cq.commerce.demandware.replication.content.attributemapping.impl;

import com.adobe.cq.commerce.demandware.replication.content.attributemapping.AttributeDescriptor;
import org.apache.commons.lang3.text.StrMatcher;
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AttributeDescriptorFactory {

    private static final Logger LOG = LoggerFactory
            .getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());


    // AEM 6.4 does not include org.apache.commons.text. Therefore,
    // the deprecated StrTokenizer is used.
    private final StrTokenizer tokenizer =
            new StrTokenizer("", ';', '"')
                .setIgnoreEmptyTokens(false)
                .setEmptyTokenAsNull(false)
                .setTrimmerMatcher(StrMatcher.trimMatcher());


    public List<AttributeDescriptor> fromOsgiConfig(final String[] config) {
        if(config==null || config.length==0) {
            return Collections.emptyList();
        }

        final List<AttributeDescriptor> mapping = new ArrayList<>(config.length);
        for(String configLine: config) {
            final String[] components = tokenizer.reset(configLine).getTokenArray();
            if(components.length<2) {
                LOG.warn("Ignoring config line '{}' on attribute mapping since it must have at least 2 parts. It has {}.",
                        configLine, components.length);
            } else {
                if (components.length > 4) {
                    LOG.warn("Config lines in attribute mappings can only have four parts but the provided line '{}' has {} parts.",
                            configLine, components.length);
                }

                final String sourceName = components[0];
                final String targetName = components[1];
                final String converterId = components.length > 2 ? components[2] : "";
                final String defaultValue = components.length > 3 ? components[3] : "";

                mapping.add(new AttributeDescriptor(sourceName,
                                                    targetName,
                                                    converterId,
                                                    defaultValue));
            }
        }
        return mapping;
    }
}
