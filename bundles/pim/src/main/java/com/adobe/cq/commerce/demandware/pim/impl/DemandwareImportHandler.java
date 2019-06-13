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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.framework.Constants;

import com.adobe.cq.commerce.demandware.pim.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.pim.ImportAssetHandler;
import com.adobe.cq.commerce.demandware.pim.ImportContext;
import com.adobe.cq.commerce.demandware.pim.ImportHandler;
import com.adobe.granite.asset.api.Asset;
import com.day.cq.commons.jcr.JcrUtil;

@Component(label = "Demandware Product Import Handler", metatype = false)
@Service
@Properties(value = {
    @Property(name = Constants.SERVICE_RANKING, intValue = 0, propertyPrivate = true),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Demandware specific import handler implementation",
        propertyPrivate = true)
})
public class DemandwareImportHandler implements ImportHandler {

    private static final String DEFAULT_PRODUCT_RESOURCE_TYPE = "commerce/components/product";
    private static final String DEFAULT_VARIANT_RESOURCE_TYPE = "commerce/components/product";
    private static final String DEFAULT_ASSET_RESOURCE_TYPE = "commerce/components/product/image";


    @Reference
    private ImportAssetHandler importAssetHandler;

    @Override
    public String createCatalog(ImportContext ctx, String catalogId) throws RepositoryException {
        ctx.setCatalogId(catalogId);
        final Session session = ctx.getResourceResolver().adaptTo(Session.class);
        final String catalogPath = ctx.getBaseResource().getPath() + "/" + catalogId;
        final Node catalogNode = JcrUtil.createPath(catalogPath, "sling:Folder", session);

        final Calendar instance = Calendar.getInstance();
        catalogNode.setProperty("cq:lastImported", instance);
        ctx.addCategoryPath(catalogId, catalogNode.getPath());
        ctx.info("Created catalog", catalogNode.getPath());

        return catalogNode.getPath();
    }

    @Override
    public String createCategory(ImportContext ctx, ValueMap values)
        throws RepositoryException {

        final String parentCategoryId = values.get(DemandwareCommerceConstants.ATTRIBUTE_PARENT_CATEGORY_ID,
            String.class);
        final String parentCategoryPath = parentCategoryId != null ? ctx.getCategoryPath(parentCategoryId)
            : ctx.getCategoryPath(ctx.getCatalogId());
        final String categoryId = values.get(DemandwareCommerceConstants.ATTRIBUTE_CATEGORY_ID, String.class);

        final Session session = ctx.getResourceResolver().adaptTo(Session.class);
        final String path = parentCategoryPath + "/" + JcrUtil.createValidName(categoryId);

        // create category node
        final Node categoryNode = JcrUtil.createPath(path, "sling:Folder", session);

        // write category attributes
        final ValueMap attributes = new ValueMapDecorator((Map<String, Object>) values.get
            (DemandwareCommerceConstants.ATTRIBUTE_MAPPINGS));
        updateProperties(categoryNode, attributes);

        ctx.addCategoryPath(categoryId, categoryNode.getPath());
        ctx.info("Created category", categoryNode.getPath());
        return categoryNode.getPath();
    }

    @Override
    public void updateProduct(ImportContext ctx, Node productNode, ValueMap productValueMap)
        throws RepositoryException {
        productNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, DEFAULT_PRODUCT_RESOURCE_TYPE);

        // write product properties
        final ValueMap properties = new ValueMapDecorator(
            (Map<String, Object>) productValueMap.get(DemandwareCommerceConstants.ATTRIBUTE_MAPPINGS));
        updateProperties(productNode, properties);

        // write product attributes (available variant axis and defining)
        if (productValueMap.containsKey(DemandwareCommerceConstants.ATTRIBUTE_ATTR_MAPPINGS)) {
            final Map<String, Object> attributes = (Map<String, Object>) productValueMap
                .get(DemandwareCommerceConstants.ATTRIBUTE_ATTR_MAPPINGS);
            for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
                final Map<String, Object> attributeValue = (Map<String, Object>) attribute.getValue();
                final String axis = (String) attributeValue.get(DemandwareCommerceConstants.ATTRIBUTE_ATTR_MAPPINGS_ID);
                ImporterUtil.registerVariantAxis(productNode, JcrUtil.createValidName(axis));
                JcrUtil.setProperty(productNode, "variant." + JcrUtil.createValidName(axis) + ".name",
                    attributeValue.get("attribute-title"));
            }
        }
    }

    @Override
    public void updateVariantProduct(ImportContext ctx, Node variantNode, ValueMap variantValueMap)
        throws RepositoryException {
        variantNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, DEFAULT_VARIANT_RESOURCE_TYPE);
        ValueMap properties = new ValueMapDecorator(
            (Map<String, Object>) variantValueMap.get(DemandwareCommerceConstants.ATTRIBUTE_MAPPINGS));
        updateProperties(variantNode, properties);
        properties = new ValueMapDecorator((Map<String, Object>) variantValueMap.get(DemandwareCommerceConstants
            .ATTRIBUTE_ATTR_MAPPINGS_CUSTOM));
        updateProperties(variantNode, properties);
    }

    @Override
    public void updateAsset(ImportContext ctx, Node imageNode, ValueMap values) throws RepositoryException {
        imageNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, DEFAULT_ASSET_RESOURCE_TYPE);
        imageNode.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());

        final Asset asset = importAssetHandler.retrieveAsset(ctx, values);
        if (asset != null) {
            imageNode.setProperty("fileReference", asset.getPath());
            ctx.info("Attached asset", asset.getPath());
        }
    }

    /**
     * Writes all product properties into JCR.
     */
    private void updateProperties(final Node node, final ValueMap values) throws RepositoryException {
        for (final String key : values.keySet()) {
            final Object value = values.get(key);
            if (value instanceof List<?> && ((List<?>) value).size() > 0) {
                final List<?> list = (List<?>) value;
                if (list.get(0) instanceof String) {
                    node.setProperty(key, list.toArray(new String[list.size()]));
                }
            } else if (value instanceof String) {
                node.setProperty(key, (String) value);
            } else if (value instanceof Boolean) {
                node.setProperty(key, (Boolean) value);
            } else if (value instanceof Double) {
                node.setProperty(key, (Double) value);
            } else if (value instanceof Long) {
                node.setProperty(key, (Long) value);
            } else if (value instanceof BigDecimal) {
                node.setProperty(key, (BigDecimal) value);
            } else if (value instanceof Date) {
                final Calendar cal = Calendar.getInstance();
                cal.setTime((Date) value);
                node.setProperty(key, cal);
            }
        }
    }
}
