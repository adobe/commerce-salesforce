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
package com.adobe.cq.commerce.demandware.replication.content.resolution.impl;

import com.adobe.cq.commerce.demandware.replication.content.resolution.ContentAssetNameResolver;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;


/**
 * Get the content asset name based on the page name of the live relation source page (if there is one) or
 * current page resource name.
 */
@Component(
        service = ContentAssetNameResolver.class,
        property = Constants.SERVICE_RANKING + ":Integer=0"
)
public class DefaultContentAssetNameResolver implements ContentAssetNameResolver {

    private static final Logger LOG = LoggerFactory.getLogger(
               MethodHandles.lookup().lookupClass());


    @Reference
    private volatile LiveRelationshipManager liveRelationshipManager;

    @Override
    public String resolve(Resource resource) {
        if (liveRelationshipManager.hasLiveRelationship(resource)) {
            try {
                LiveRelationship lr = liveRelationshipManager.getLiveRelationship(resource, false);
                return StringUtils.substringAfterLast(lr.getSourcePath(), "/");
            } catch (WCMException wcme) {
                LOG.debug("Error retrieving live relationship", wcme);
            }
        }
        return resource.getName();
    }
}
