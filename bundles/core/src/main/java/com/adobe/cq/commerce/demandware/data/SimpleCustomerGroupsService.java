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

import com.adobe.cq.commerce.demandware.CustomerGroupsService;

/**
 * Simple configurable service return a map of configured Demandware customer groups. (Demandware does not have an
 * OCAPI endpoint to retrieve customer groups directly)
 */
@Component(metatype = true, policy = ConfigurationPolicy.REQUIRE, label = "Demandware Simple Customer Groups Service")
@Service
public class SimpleCustomerGroupsService implements CustomerGroupsService {

    @Property(value = {"Everyone;Everyone"}, label = "Customer Groups", description = "Add available customer groups." +
            " Format: <key>;<display name>", cardinality = Integer.MAX_VALUE)
    private static final String CUSTOMER_GROUPS = "customergroups";

    private Map<String, String> customerGroups = new TreeMap<>();

    @Override
    public Map<String, String> getCustomerGroups() {
        return customerGroups;
    }

    @Activate
    protected void activate(final ComponentContext ctx) {
        final Dictionary<?, ?> config = ctx.getProperties();
        final String[] mapping = PropertiesUtil.toStringArray(config.get(CUSTOMER_GROUPS));
        for (final String s : mapping) {
            final String[] components = StringUtils.split(s, ';');
            if (components.length >= 2) {
                customerGroups.put(components[0], components[1]);
            }
        }
    }
}
