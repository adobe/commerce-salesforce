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

package com.adobe.cq.commerce.demandware.replication.utils;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ContentUtils.class);

    private static List<String> CONTENT_ASSET_PARSYS_RESOURCE_TYPES = Arrays.asList("commerce/demandware/components/placeholder/parsys");

    /**
     * Find the parsys to render.
     *
     * @param resource     the current resource
     * @param resourceTypes the optional resource types
     * @return the parsys resource
     */
    public static Resource getContentAssetParsys(final Resource resource, final List<String> resourceTypes) {
        List<String> resTypes;
        if ( resourceTypes == null || resourceTypes.isEmpty() ) {
            resTypes = CONTENT_ASSET_PARSYS_RESOURCE_TYPES;
        } else {
            resTypes = resourceTypes;
        }

        for ( String resType : resTypes ) {
            LOG.debug("Search for parsys resource of type {} in {}", resType, resource.getPath());
            if (resource.isResourceType(resType)) {
                LOG.debug("Found parsys resource of type {} in {}", resType, resource.getPath());
                return resource;
            }
        }

        Resource contentAssetResource = null;
        final Iterable<Resource> children = resource.getChildren();
        for (Resource child : children) {
            contentAssetResource = getContentAssetParsys(child, resourceTypes);
            if (contentAssetResource != null) {
                return contentAssetResource;
            }
        }
        return null;
    }
}
