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
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.collection.ProductCollection;
import com.adobe.cq.commerce.api.collection.ProductCollectionManager;
import com.adobe.cq.commerce.demandware.pim.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.pim.ImportContext;
import com.adobe.cq.commerce.demandware.pim.ImportHandler;
import com.adobe.cq.commerce.pim.api.ProductImporter;
import com.adobe.cq.commerce.pim.common.AbstractProductImporter;

/**
 * Imports products from a Demandware XML export file.
 */
@Component(metatype = true, label = "Demandware Product Importer")
@Service
@Properties(value = {
        @Property(name = "service.description", value = "Demandware product importer"),
        @Property(name = "commerceProvider", value = "demandware", propertyPrivate = true)
})
public class DemandwareProductImporter extends AbstractProductImporter implements ProductImporter {

    private static final Logger LOG = LoggerFactory.getLogger(DemandwareProductImporter.class);
    /**
     * Internal log writer.
     */
    protected Writer logWriter = new Writer() {
        final StringBuffer buffer = new StringBuffer();

        @Override
        public void write(final char[] cbuf, final int off, final int len) throws IOException {
            buffer.append(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            if (buffer.length() > 0) {
                logMessage(buffer.toString(), buffer.charAt(0) == 'E');
                buffer.setLength(0);
            }
        }

        @Override
        public void close() throws IOException {
            // nothing to do
        }
    };
    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private ImportHandler importHandler;
    private InputStream xmlStream = null;
    private boolean addToCollection;
    private String collectionPath;
    private Map<String, String> sourceProducts = new HashMap<String, String>();

    @Override
    protected boolean validateInput(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        ResourceResolver resourceResolver = request.getResourceResolver();

        String provider = request.getParameter("provider");
        if (provider == null || provider.length() == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No commerce provider specified.");
            return false;
        }

        String csvPath = request.getParameter("csvPath");
        try {
            Resource csvResource = resourceResolver.getResource(csvPath);
            Node source = csvResource.adaptTo(Node.class);
            xmlStream = source.getProperty(
                    JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_DATA).getBinary().getStream();
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Catalog XML [" + csvPath + "] not found.");
            return false;
        }

        String storePath = request.getParameter("storePath");
        String storeName = request.getParameter("storeName");
        if (StringUtils.isEmpty(storePath) || StringUtils.isEmpty(storeName)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Destination not specified.");
            return false;
        }

        addToCollection = "true".equals(request.getParameter("addToCollection"));
        collectionPath = request.getParameter("collectionPath");

        return true;
    }

    @Override
    protected void doImport(ResourceResolver resourceResolver, Node storeRoot, boolean incrementalImport)
            throws RepositoryException, IOException {

        final PrintWriter writer = new PrintWriter(logWriter);

        if (xmlStream != null) {
            try {
                final Resource storeRootResource = resourceResolver.getResource(storeRoot.getPath());
                final ImportContext ctx = new DemandwareImporterContext(storeRootResource, resourceResolver, writer);
                logMessage("Read xml data ...", false);
                DemandwareXMLReader reader = new DemandwareXMLReader();
                reader.readFromXML(xmlStream, ctx);

                logMessage("Write catalog ...", false);
                importHandler.createCatalog(ctx, ctx.getCatalogId());
                checkpoint(resourceResolver.adaptTo(Session.class), true);

                logMessage("Write catalog structure ...", false);
                createCategoryTree(ctx, ctx.getCatalogId());
                checkpoint(resourceResolver.adaptTo(Session.class), true);

                logMessage("Write product data ...", false);
                createProducts(ctx);
                checkpoint(resourceResolver.adaptTo(Session.class), true);
            } catch (XMLStreamException e) {
                logMessage("Error parsing xml", true);
                LOG.error("Error parsing xml", e);
            }
        }

        if (addToCollection && !sourceProducts.isEmpty()) {
            manageProductCollection(resourceResolver, collectionPath, sourceProducts);
        }

        writer.flush();
    }

    private void createCategoryTree(final ImportContext ctx, final String categoryId) throws RepositoryException {
        final List<String> childCategories = ctx.getChildCategories(categoryId);
        if (childCategories != null) {
            for (String childCategoryId : childCategories) {
                importHandler.createCategory(ctx, ctx.getCategory(childCategoryId));
                createCategoryTree(ctx, childCategoryId);
            }
        }
    }

    private void createProducts(ImportContext ctx) throws RepositoryException {
        final Map<String, String> productMapping = ctx.getProductMappings();
        for (Map.Entry<String, String> entry : productMapping.entrySet()) {
            importProduct(ctx, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Get the product data of a product and import it into the repository.
     *
     * @param ctx        The <code>ImporterContext</code>.
     * @param productId  The id of the product.
     * @param categoryId The id of the parent category.
     * @throws RepositoryException in cate the product import fails
     */
    protected void importProduct(final ImportContext ctx, final String productId, final String categoryId)
            throws RepositoryException {
        LOG.debug("import product {} ", productId);

        final String productBasePath = ImporterUtil.getProductPath(productId,
                ctx.getCategoryPath(categoryId));

        final Session importSession = ctx.getResourceResolver().adaptTo(Session.class);

        // create the product node of not existing & update the product data
        final Resource productResource = ctx.getResourceResolver().getResource(productBasePath);
        final Node productNode;

        boolean productUpdate = false;
        if (productResource == null) {
            productNode = createProduct(productBasePath, importSession);
        } else {
            productNode = productResource.adaptTo(Node.class);
            productUpdate = true;
        }

        // update product attributes
        final ValueMap productValueMap = ctx.getProduct(productId);
        importHandler.updateProduct(ctx, productNode, productValueMap);

        // update the product image
        if (productValueMap.containsKey(DemandwareCommerceConstants.ATTRIBUTE_ASSETS)) {
            MultiMap assets = (MultiMap) productValueMap.get(DemandwareCommerceConstants.ATTRIBUTE_ASSETS);
            if (assets.containsKey("0")) {
                List<Map<String, Object>> productAssets = (List<Map<String, Object>>) assets.get("0");
                if (productAssets != null) {
                    importAsset(ctx, productNode, productAssets, productId, categoryId);
                    checkpoint(importSession, false);
                }
            }
        }

        checkpoint(importSession, false);
        if (productUpdate) {
            productUpdated(productNode);
            LOG.debug("Updated product {} at {}.", productId, productNode.getPath());
        } else {
            LOG.debug("Created product {} at {}.", productId, productNode.getPath());
        }

        // product collections
        sourceProducts.put(productNode.getPath(), "add");

        // handle variants
        if (productValueMap.containsKey(DemandwareCommerceConstants.ATTRIBUTE_VARIANTS)) {
            importVariant(ctx, productValueMap, productNode);
        }
    }

    /**
     * Create or update the product variant data.
     *
     * @param ctx             The <code>ImporterContext</code>.
     * @param productData     the product date value map
     * @param baseProductNode the base product <code>Node</code>
     * @throws RepositoryException in case the product variant import fails
     */
    protected void importVariant(final ImportContext ctx, final Map<String, Object> productData, final Node
            baseProductNode) throws RepositoryException {
        final Set<String> variantIds = (Set<String>) productData.get(DemandwareCommerceConstants.ATTRIBUTE_VARIANTS);
        final Session importSession = ctx.getResourceResolver().adaptTo(Session.class);
        LOG.debug("Create {} variant(s) for {}", variantIds.size(), baseProductNode.getPath());

        for (String variantId : variantIds) {
            // create the product node of not existing & update the product data
            boolean variantUpdate = false;
            Node variantNode = baseProductNode.hasNode(variantId) ? baseProductNode.getNode(variantId) : null;
            if (variantNode == null) {
                variantNode = createVariant(baseProductNode, variantId);
            } else {
                variantUpdate = true;
            }

            // update variant attributes
            final ValueMap variantData = ctx.getProduct(variantId);
            importHandler.updateVariantProduct(ctx, variantNode, variantData);


            // update the variant image
            if (productData.containsKey(DemandwareCommerceConstants.ATTRIBUTE_ASSETS)) {
                MultiMap assets = (MultiMap) productData.get(DemandwareCommerceConstants.ATTRIBUTE_ASSETS);
                final Map<String, String> variantAttributes = (Map<String, String>) variantData.get(
                        DemandwareCommerceConstants.ATTRIBUTE_ATTR_MAPPINGS_CUSTOM);

                for (Map.Entry<String, String> variantAttribute : variantAttributes.entrySet()) {
                    if (assets.containsKey(variantAttribute.getValue())) {
                        List<Map<String, Object>> productAssets = (List<Map<String, Object>>) assets.get
                                (variantAttribute.getValue());
                        if (productAssets != null) {
                            importAsset(ctx, variantNode, productAssets, variantId, ctx.getProductMappings().get(
                                    productData.get(DemandwareCommerceConstants.ATTRIBUTE_PRODUCT_ID)));
                            checkpoint(importSession, false);
                        }
                    }
                }
            }

            checkpoint(importSession, false);
            if (variantUpdate) {
                variantUpdated(variantNode);
                LOG.debug("Updated product variantId {} at {}.", variantId, variantNode.getPath());
            } else {
                LOG.debug("Created product variantId {} at {}.", variantId, variantNode.getPath());
            }
        }
    }

    protected void importAsset(final ImportContext ctx, final Node baseNode,
                               final List<Map<String, Object>> productAssets, String productId, String categoryId)
            throws RepositoryException {
        final Node assetsNode = JcrUtils.getOrAddNode(baseNode, "assets", JcrConstants
                .NT_UNSTRUCTURED);
        for (Map<String, Object> productAsset : productAssets) {
            Node assetNode = JcrUtils.getOrCreateUniqueByPath(assetsNode, "asset", JcrConstants
                    .NT_UNSTRUCTURED);
            productAsset.put(DemandwareCommerceConstants.ATTRIBUTE_CATEGORY_ID, categoryId);
            productAsset.put(DemandwareCommerceConstants.ATTRIBUTE_PRODUCT_ID, productId);
            importHandler.updateAsset(ctx, assetNode, new ValueMapDecorator(productAsset));
        }
    }

    private void manageProductCollection(ResourceResolver resourceResolver, String collectionPath,
                                         Map<String, String> sourceProducts) {
        ProductCollectionManager collectionMgr = resourceResolver.adaptTo(ProductCollectionManager.class);
        ProductCollection productCollection = collectionMgr.getCollection(collectionPath);
        if (productCollection == null) {
            LOG.error("The collection at {} is not defined.", collectionPath);
            return;
        }
        Set<String> productPaths = sourceProducts.keySet();
        for (String productPath : productPaths) {
            String operation = sourceProducts.get(productPath);
            if ("add".equals(operation) || "update".equals(operation)) {
                try {
                    productCollection.add(productPath);
                    LOG.debug("Added product at {} to the collection {}", productPath, collectionPath);
                } catch (CommerceException e) {
                    LOG.error("Failed to add product at {} to the collection {}", productPath, collectionPath);
                }
            } else if ("remove".equals(operation)) {
                try {
                    productCollection.remove(productPath);
                    LOG.debug("Removed product at {} to the collection {}", productPath, collectionPath);
                } catch (CommerceException e) {
                    LOG.error("Failed to remove product at {} to the collection {}", productPath, collectionPath);
                }
            }
        }
    }
}
