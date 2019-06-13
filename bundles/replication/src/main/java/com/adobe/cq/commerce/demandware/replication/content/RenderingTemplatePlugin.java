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

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

import com.adobe.cq.commerce.demandware.RenderService;
import com.adobe.cq.commerce.demandware.replication.ContentBuilderPlugin;
import com.adobe.cq.commerce.demandware.DemandwareCommerceConstants;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.wcm.api.Page;

/**
 * <code>ContentBuilderPlugin</code> to export and map a AEM pages of the configured resource type to a Demandware
 * Content Asset. This plugin maps all configured page properties to the appropriate attribute of the JSON payload.
 * The plugin also sets the OCAPI endpoint and meta information for replication content assets.
 * This plugin will not render any body attribute.
 */
@Component(label = "Demandware ContentBuilder Plugin for Page > Velocity rendering template transformation", policy =
        ConfigurationPolicy.REQUIRE, immediate = true, metatype = true)
@Service(value = ContentBuilderPlugin.class)
@Properties({@Property(name = ContentBuilderPlugin.PN_TASK, value = "RenderingTemplatePlugin", propertyPrivate = true),
        @Property(name = Constants.SERVICE_RANKING, intValue = 90)})
public class RenderingTemplatePlugin extends AbstractContentBuilderPlugin {

    private static final String DEFAULT_WEBDAV_ENDPOINT = "/on/demandware.servlet/webdav/Sites/Dynamic/{site_id}";
    private static final String DEFAULT_TEMPLATE_SITE = "SiteGenesis";

    @Property(label = "The Sling resource types which is plugin is applied", cardinality = Integer.MAX_VALUE)
    private static final String CONTENT_ASSET_RESOURCE_TYPES = "resourcetypes";

    @Property(label = "The relative path of the WEBDAV share for static assets", value = DEFAULT_WEBDAV_ENDPOINT)
    private static final String WEBDAV_ENDPOINT = "api";

    @Property(label = "The site to be published.", value = DEFAULT_TEMPLATE_SITE)
    private static final String TEMPLATE_SITE = "site";

    @Reference
    private RenderService renderService;

    private String defaultSite;

    @Override
    public JSONObject create(final ReplicationAction action, final Resource resource, final JSONObject content)
            throws JSONException {
        JSONObject delivery = content;
        if (delivery == null) {
            delivery = new JSONObject();
        }

        // map page attributes
        final Page page = resource.adaptTo(Page.class);
        final ValueMap pageProperties = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        final String site = ((HierarchyNodeInheritanceValueMap) pageProperties).getInherited(
                DemandwareCommerceConstants.PN_DWRE_SITE, String.class);
        final String templatePath = StringUtils.appendIfMissing(StringUtils.defaultIfEmpty(
                page.getProperties().get(DemandwareCommerceConstants.PN_DWRE_TEMPLATE_PATH, String.class),
                page.getPath()), ".vs", ".vs");

        // add meta data
        addAPIType(DemandwareCommerceConstants.TYPE_WEBDAV, delivery);
        addPayloadType("static-asset", delivery);
        delivery.put(DemandwareCommerceConstants.ATTR_WEBDAV_SHARE, api);
        delivery.put(DemandwareCommerceConstants.ATTR_SITE, StringUtils.defaultIfEmpty(site,
                defaultSite));
        delivery.put(DemandwareCommerceConstants.ATTR_ID, page.getName());
        delivery.put(DemandwareCommerceConstants.ATTR_PATH, templatePath);

        // activate? render and add template data
        if (action.getType() == ReplicationActionType.ACTIVATE) {
            // construct payload and render page to template
            JSONObject templateData = getJSONPayload(delivery);
            final String bodyContent = renderService.render(resource, null, DemandwareCommerceConstants.DWRE_RENDERING_SELECTOR);
            if (StringUtils.isNotEmpty(bodyContent)) {
                templateData.put(DemandwareCommerceConstants.ATTR_SIZE, bodyContent.length());
                templateData.put(DemandwareCommerceConstants.ATTR_MIMETYPE,
                        ContentType.APPLICATION_XHTML_XML.getMimeType());
                templateData.put(DemandwareCommerceConstants.ATTR_DATA, bodyContent);
                delivery.put(DemandwareCommerceConstants.ATTR_PAYLOAD, templateData);
            }
        }
        return delivery;
    }

    @Activate
    protected void activate(final ComponentContext ctx) {
        final Dictionary<?, ?> config = ctx.getProperties();
        supportedResourceTypes = Arrays.asList(PropertiesUtil.toStringArray(config.get(CONTENT_ASSET_RESOURCE_TYPES)));
        api = PropertiesUtil.toString(config.get(WEBDAV_ENDPOINT), DEFAULT_WEBDAV_ENDPOINT);
        defaultSite = PropertiesUtil.toString(config.get(TEMPLATE_SITE), DEFAULT_TEMPLATE_SITE);
    }
}
