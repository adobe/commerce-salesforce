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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Iterator;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
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
import com.adobe.cq.commerce.demandware.replication.ContentBuilderPlugin;
import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.Rendition;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;

/**
 * <code>ContentBuilderPlugin</code> to export AEM DAM assets to static assets files on a Demandware WebDAV folder.
 */
@Component(label = "Demandware ContentBuilder Plugin for DAM Asset > static file asset transformation", policy =
        ConfigurationPolicy.REQUIRE, immediate = true, metatype = true)
@Service(value = ContentBuilderPlugin.class)
@Properties({@Property(name = ContentBuilderPlugin.PN_TASK, value = "DAMAssetPlugin", propertyPrivate = true),
        @Property(name = Constants.SERVICE_RANKING, intValue = 30)})
public class DAMAssetPlugin extends AbstractContentBuilderPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(DAMAssetPlugin.class);

    private static final String DEFAULT_WEBDAV_ENDPOINT = "/on/demandware.servlet/webdav/Sites/Libraries/{library_id}/{scope}";
    private static final String DEFAULT_ASSET_RENDITION = "original";
    private static final String DEFAULT_ASSET_SCOPE = "default";

    @Property(label = "The relative path of the WEBDAV share for static assets", value = DEFAULT_WEBDAV_ENDPOINT)
    private static final String WEBDAV_ENDPOINT = "api";

    @Property(label = "The asset rendition to be exported", value = DEFAULT_ASSET_RENDITION)
    private static final String ASSET_RENDITION = "asset.rendition";

    @Property(label = "The asset default library to be used")
    private static final String ASSET_LIBRARY = "asset.library";

    @Property(label = "The asset default scope to be used", value = DEFAULT_ASSET_SCOPE)
    private static final String ASSET_SCOPE = "asset.scope";


    private String assetRendition;
    private String defaultAssetLibrary;
    private String defaultAssetScope;

    @Override
    public boolean canHandle(final ReplicationAction action, final Resource resource) {
        return resource.adaptTo(Asset.class) != null;
    }

    @Override
    public JSONObject create(final ReplicationAction action, final Resource resource, final JSONObject content)
            throws JSONException {
        JSONObject delivery = content;
        if (delivery == null) {
            delivery = new JSONObject();
        }

        Asset asset = resource.adaptTo(Asset.class);
        if (asset != null) {

            final HierarchyNodeInheritanceValueMap properties = new HierarchyNodeInheritanceValueMap(resource);

            // add meta data
            addAPIType(DemandwareCommerceConstants.TYPE_WEBDAV, delivery);
            addPayloadType("static-asset", delivery);
            delivery.put(DemandwareCommerceConstants.ATTR_WEBDAV_SHARE, api);
            delivery.put(DemandwareCommerceConstants.ATTR_LIBRARY,
                    properties.getInherited(DemandwareCommerceConstants.PN_DWRE_LIBRARY, defaultAssetLibrary));
            delivery.put(DemandwareCommerceConstants.ATTR_SCOPE,
                    properties.getInherited(JcrConstants.JCR_LANGUAGE, defaultAssetScope));
            delivery.put(DemandwareCommerceConstants.ATTR_ID, asset.getName());
            delivery.put(DemandwareCommerceConstants.ATTR_PATH, asset.getPath());

            // activate? then add the asset data, otherwise not needed
            if (action.getType() == ReplicationActionType.ACTIVATE) {
                // construct payload and map page data
                JSONObject assetData = getJSONPayload(delivery);

                // get rendition and serialize it
                Rendition rendition = findWebRendition(asset, assetRendition);
                if (rendition != null) {
                    assetData.put(DemandwareCommerceConstants.ATTR_SIZE, rendition.getSize());
                    assetData.put(DemandwareCommerceConstants.ATTR_MIMETYPE, rendition.getMimeType());
                    assetData.put(DemandwareCommerceConstants.ATTR_BASE64, true);
                    final Base64InputStream inputStream = new Base64InputStream(rendition.getStream(), true);
                    try {
                        assetData.put(DemandwareCommerceConstants.ATTR_DATA, IOUtils.toString(inputStream));
                    } catch (IOException e) {
                        LOG.error("Unable to serialize asset data {}", resource.getPath(), e);
                    }
                } else {
                    LOG.error("Can not extract asset for {}", resource.getPath());
                }

                delivery.put(DemandwareCommerceConstants.ATTR_PAYLOAD, assetData);
            }
        } else {
            LOG.warn("Resource {} can not adapted to an asset", resource.getPath());
        }
        return delivery;
    }

    private Rendition findWebRendition(Asset asset, String renditionName) {
        final Iterator<? extends Rendition> renditions = asset.listRenditions();
        while (renditions.hasNext()) {
            Rendition rendition = renditions.next();
            if (StringUtils.startsWith(rendition.getName(), renditionName)) {
                return rendition;
            }
        }
        return asset.getRendition(DEFAULT_ASSET_RENDITION);
    }

    @Activate
    protected void activate(final ComponentContext ctx) {
        final Dictionary<?, ?> config = ctx.getProperties();
        api = PropertiesUtil.toString(config.get(WEBDAV_ENDPOINT), DEFAULT_WEBDAV_ENDPOINT);
        assetRendition = PropertiesUtil.toString(config.get(ASSET_RENDITION), DEFAULT_ASSET_RENDITION);
        defaultAssetLibrary = PropertiesUtil.toString(config.get(ASSET_LIBRARY), "");
        defaultAssetScope = PropertiesUtil.toString(config.get(ASSET_SCOPE), DEFAULT_ASSET_SCOPE);
    }
}
