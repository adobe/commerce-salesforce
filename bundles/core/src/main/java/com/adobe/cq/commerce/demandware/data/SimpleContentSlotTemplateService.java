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

package com.adobe.cq.commerce.demandware.data;

import java.util.Dictionary;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.demandware.ContentSlotTemplateService;

@Component(metatype = true, policy = ConfigurationPolicy.REQUIRE, label = "Demandware Simple Template Service")
@Service
public class SimpleContentSlotTemplateService implements ContentSlotTemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleContentSlotTemplateService.class);

    @Property(value = {"T1;T1"}, label = "Available templates", description = "Add available templates." +
            " Format: <key>;<display name>", cardinality = Integer.MAX_VALUE)
    private static final String TEMPLATES = "templates";

    private Map<String, String> templates = new TreeMap<>();

    @Override
    public Map<String, String> getTemplates() {
        return templates;
    }

    @Activate
    protected void activate(final ComponentContext ctx) {
        final Dictionary<?, ?> config = ctx.getProperties();
        final String[] mapping = PropertiesUtil.toStringArray(config.get(TEMPLATES));
        for (final String s : mapping) {
            final String[] components = StringUtils.split(s, ';');
            if (components.length >= 2) {
                templates.put(components[0], components[1]);
            }
        }
    }
}
