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
import org.apache.commons.lang3.ArrayUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.*;

import static java.util.Collections.*;

@Component(service = DwreFolderAttributesAssignment.class)
@Designate(ocd = DwreFolderAttributesAssignment.Configuration.class, factory = true)
public class DwreFolderAttributesAssignment {

    private final AttributeDescriptorFactory attributeDescriptorFactory
            = new AttributeDescriptorFactory();

    private volatile Set<String> folders = synchronizedSet(new HashSet<>());
    private volatile List<AttributeDescriptor> attributeDescriptors =
            synchronizedList(new ArrayList<>());

    public Set<String> getFolders() {
        return unmodifiableSet(folders);
    }

    public boolean isApplicable(final String dwreFolder) {
        return isApplicable(singletonList(dwreFolder));
    }

    public boolean isApplicable(final Collection<String> dwreFolders) {
        for(final String folder: dwreFolders) {
            if(folders.contains(folder)) {
                return true;
            }
        }
        return false;
    }

    public List<AttributeDescriptor> getAttributeDescriptors() {
        return unmodifiableList(attributeDescriptors);
    }

    @Activate
    private void activate(final Configuration config) {
        folders.clear();
        attributeDescriptors.clear();

        folders.addAll(ArrayUtils.isEmpty(config.folders())
                ? Collections.emptyList()
                : Arrays.asList(config.folders()));
        attributeDescriptors.addAll(ArrayUtils.isEmpty(config.attributes_mapping())
                ? Collections.emptyList()
                : attributeDescriptorFactory.fromOsgiConfig(config.attributes_mapping()));
    }

    @ObjectClassDefinition(name = "Demandware ContentBuilder Plugin for Page > Folder Attribute Assignments")
    @interface Configuration {

        @AttributeDefinition(
                name = "Priority",
                description = "Highest priority will be applied latest, thus possibly overwriting lower priority assignments."
        )
        int service_ranking() default 0;

        @AttributeDefinition(
                name = "Folders",
                description = "List of demandware folders the given attributes are valid for."
        )
        String[] folders() default {};

        @AttributeDefinition(
                name = "Attribute Mapping",
                description = "Page attribute mapping: <JCR property name>;<Demandware attribute name>;<converter ID>:<default value>. Last two parameters are optional."
        )
        String[] attributes_mapping() default {};

        @AttributeDefinition(
                name = "invisible"
        )
        String webconsole_configurationFactory_nameHint() default "{service.ranking}, {folders} -> {attributes.mapping}";
    }
}
