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
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.osgi.service.component.annotations.*;

import java.util.*;

@Component(service = DwreFolderAttributesLookupService.class)
public class DwreFolderAttributesLookupService {

    @Reference(service = DwreFolderAttributesAssignment.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            bind = "bindAttrAssignment", unbind = "unbindAttrAssignment")
    private RankedServices<DwreFolderAttributesAssignment> assignments =
            new RankedServices<>(Order.ASCENDING);


    /**
     * Look up attributes for given demandware folders.
     *
     * @param dwreFolders The demandware classification folders
     * @return Folder specific attributes if any or empty list.
     */
    public List<AttributeDescriptor> getDescriptors(Collection<String> dwreFolders) {
        final Set<AttributeDescriptor> descriptors = new HashSet<>();
        for(DwreFolderAttributesAssignment assignment: assignments) {
            if(assignment.isApplicable(dwreFolders)) {
                descriptors.addAll(assignment.getAttributeDescriptors());
            }
        }
        return descriptors.isEmpty()
                ? Collections.emptyList()
                : new ArrayList<>(descriptors);
    }

    private void bindAttrAssignment(DwreFolderAttributesAssignment assignment, Map<String, Object> props) {
        assignments.bind(assignment, props);
    }

    private void unbindAttrAssignment(DwreFolderAttributesAssignment assignment, Map<String, Object> props) {
        assignments.unbind(assignment, props);
    }
}
