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

package com.adobe.cq.commerce.demandware.replication.content;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.RenderService;
import com.adobe.cq.commerce.demandware.replication.ContentBuilderPlugin;
import com.adobe.cq.commerce.demandware.replication.utils.ContentUtils;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.wcm.api.Page;

/**
 * <code>ContentBuilderPlugin</code> to export and map a AEM page body content (aka. parsys) of the configured resource
 * type to a Demandware Content Asset body field.
 */
@Component(label = "Demandware ContentBuilder Plugin for Page parsys > Content Asset body transformation", policy =
    ConfigurationPolicy.REQUIRE, immediate = true, metatype = true)
@Service(value = ContentBuilderPlugin.class)
@Properties({@Property(name = ContentBuilderPlugin.PN_TASK, value = "ContentAssetPageBodyPlugin", propertyPrivate = true),
    @Property(name = Constants.SERVICE_RANKING, intValue = 11)})
public class ContentAssetPageBodyPlugin extends AbstractContentBuilderPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(ContentAssetPageBodyPlugin.class);

    @Property(label = "List of Sling resource types which this plugin is applied", cardinality = Integer.MAX_VALUE)
    private static final String CONTENT_ASSET_RESOURCE_TYPES = "resourcetypes.supported";

    @Property(label = "Optional list of Sling resource types which are ignored by this plugin", cardinality = Integer.MAX_VALUE)
    private static final String IGNORED_RESOURCE_TYPES = "resourcetypes.ignored";

    @Property(label = "Optional Sling resource of the parsys to be exported as content asset body", cardinality = Integer.MAX_VALUE)
    private static final String PARSYS_RESOURCE_TYPES = "resourcetypes.parsys";

    protected List<String> parsysResourceTypes = Arrays.asList("commerce/demandware/components/placeholder/parsys");

    @Reference
    private RenderService renderService;

    @Override
    public boolean canHandle(final ReplicationAction action, final Resource resource) {
        return super.canHandle(action, resource) && action.getType() == ReplicationActionType.ACTIVATE;
    }

    @Override
    public JSONObject create(final ReplicationAction action, final Resource resource, final JSONObject content)
        throws JSONException {
        JSONObject delivery = content;
        if (delivery == null) {
            delivery = new JSONObject();
        }

        // add meta data
        addAPIType(DemandwareCommerceConstants.TYPE_OCAPI, delivery);

        final Page page = resource.adaptTo(Page.class);
        LOG.debug("Transform page {} into content asset body", page.getPath());
        final String language = getLanguage(page);

        // add body content
        JSONObject pageData = getJSONPayload(delivery);

        final Resource contentAssetResource = ContentUtils.getContentAssetParsys(page.getContentResource(), parsysResourceTypes);
        if (contentAssetResource != null) {
            LOG.debug("Content asset body resource is {}", contentAssetResource.getPath());
            final String renderedPageBodyContent = renderService.render(contentAssetResource, null, DemandwareCommerceConstants.DWRE_RENDERING_SELECTOR);
            if (StringUtils.isNotEmpty(renderedPageBodyContent)) {
                pageData.put(DemandwareCommerceConstants.ATTR_BODY, createMarkupTextJSONObject(renderedPageBodyContent, language));
            } else {
                LOG.warn("Content asset body resource {} could not be pre-rendered.", contentAssetResource.getPath());
            }
        } else {
            LOG.debug("Could not detect content asset body resource");
        }
        LOG.debug("Delivery for page {}: {}", page.getPath(), delivery.toString());

        return delivery;
    }

    @Activate
    protected void activate(final ComponentContext ctx) {
        final Dictionary<?, ?> config = ctx.getProperties();
        if (config.get(CONTENT_ASSET_RESOURCE_TYPES) != null) {
            supportedResourceTypes = Arrays.asList(
                PropertiesUtil.toStringArray(config.get(CONTENT_ASSET_RESOURCE_TYPES)));
            LOG.debug("Config - content asset resource types: {}", StringUtils.join(supportedResourceTypes));
        }
        if (config.get(IGNORED_RESOURCE_TYPES) != null) {
            ignoredResourceTypes = Arrays.asList(PropertiesUtil.toStringArray(config.get(IGNORED_RESOURCE_TYPES)));
            LOG.debug("Config - ignored resource types: {}", StringUtils.join(ignoredResourceTypes));
        }
        if (config.get(PARSYS_RESOURCE_TYPES) != null) {
            final String[] parsysResourceTypesConfig = PropertiesUtil.toStringArray(config.get(PARSYS_RESOURCE_TYPES));
            if (ArrayUtils.isNotEmpty(parsysResourceTypesConfig) && StringUtils.isNoneEmpty(StringUtils.join(parsysResourceTypesConfig))) {
                parsysResourceTypes = Arrays.asList(PropertiesUtil.toStringArray(config.get(PARSYS_RESOURCE_TYPES)));
                LOG.debug("Config - content asset parsys resource types: {}", StringUtils.join(parsysResourceTypes));
            } else {
                LOG.debug("Config - content asset parsys resource types not configured, use default");
            }
        }
    }
}
