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

package com.adobe.cq.commerce.demandware.pim.impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.demandware.DemandwareClient;
import com.adobe.cq.commerce.demandware.pim.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.pim.ImportAssetHandler;
import com.adobe.cq.commerce.demandware.pim.ImportContext;
import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.asset.api.RenditionHandler;

@Component(label = "Demandware Product Import Image Asset Handler", metatype = true)
@Service
@Properties(value = {
    @Property(name = Constants.SERVICE_RANKING, intValue = 0, propertyPrivate = true),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Demandware specific import product image handler " +
        "implementation", propertyPrivate = true)
})
public class DemandwareImageAssetHandler implements ImportAssetHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DemandwareImageAssetHandler.class);

    @Property
    private static final String DOWNLOAD_ENDPOINT = "downloadEndpoint";

    @Reference
    DemandwareClient demandwareClient;

    private String downloadEndpoint;

    @Override
    public Asset retrieveAsset(ImportContext ctx, Map<String, Object> properties) {
        if (StringUtils.isBlank(downloadEndpoint)) {
            LOG.error("Missing download endpoint, can not download any asset");
            return null;
        }

        if (!properties.containsKey(DemandwareCommerceConstants.ATTRIBUTE_ASSET_PATH) && StringUtils.isBlank
            ((String) properties.get(DemandwareCommerceConstants.ATTRIBUTE_ASSET_PATH))) {
            LOG.error("Missing asset path");
            return null;
        }
        final String relativeImagePath = (String) properties.get(DemandwareCommerceConstants.ATTRIBUTE_ASSET_PATH);
        final String endPoint = DemandwareClient.DEFAULT_SCHEMA + demandwareClient.getEndpoint() + downloadEndpoint +
            StringUtils.prependIfMissing(relativeImagePath, "/", "/");

        // call Demandware to render preview for component
        CloseableHttpResponse responseObj = null;
        CloseableHttpClient httpClient = demandwareClient.getHttpClient();
        try {
            final RequestBuilder requestBuilder = RequestBuilder.get();

            requestBuilder.setUri(endPoint);
            final HttpUriRequest requestObj = requestBuilder.build();
            responseObj = httpClient.execute(requestObj);
            if (responseObj != null && responseObj.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                AssetManager assetManager = ctx.getResourceResolver().adaptTo(AssetManager.class);

                final String assetViewType = (String) properties.get(DemandwareCommerceConstants.ATTRIBUTE_ASSET_TYPE);
                String filePath = (String) properties.get(DemandwareCommerceConstants.ATTRIBUTE_ASSET_PATH);
                if (StringUtils.equals(assetViewType, "original")) {
                    filePath = StringUtils.substringAfterLast(filePath, "/");
                }

                final String assetPath = "/content/dam/" + ctx.getBaseResource().getName() + "/products/"
                    + properties.get(DemandwareCommerceConstants.ATTRIBUTE_CATEGORY_ID) + "/"
                    + properties.get(DemandwareCommerceConstants.ATTRIBUTE_PRODUCT_ID) + "/"
                    + filePath;


                Asset asset;
                if (assetManager.assetExists(assetPath)) {
                    asset = assetManager.getAsset(assetPath);
                } else {
                    asset = assetManager.createAsset(assetPath);
                }
                Map<String, Object> config = new HashMap<String, Object>();
                config.put(RenditionHandler.PROPERTY_ID, "jcr.default");
                config.put(RenditionHandler.PROPERTY_RENDITION_MIME_TYPE, responseObj.getEntity().getContentType()
                    .getValue());
                asset.setRendition("original", responseObj.getEntity().getContent(), config);
                LOG.debug("Attached asset {} to {}", asset.getPath(),
                    properties.get(DemandwareCommerceConstants.ATTRIBUTE_PRODUCT_ID));
                return asset;
            }
        } catch (IOException e) {
            LOG.error("Failed to download asset", e);
        } finally {
            HttpClientUtils.closeQuietly(responseObj);
            HttpClientUtils.closeQuietly(httpClient);
        }
        return null;
    }

    /* OSGI stuff */

    @Activate
    protected void activate(final ComponentContext ctx) {
        final Dictionary<?, ?> config = ctx.getProperties();
        downloadEndpoint = PropertiesUtil.toString(config.get(DOWNLOAD_ENDPOINT), "");
    }
}
