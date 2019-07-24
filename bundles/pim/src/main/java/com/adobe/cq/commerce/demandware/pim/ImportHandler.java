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

package com.adobe.cq.commerce.demandware.pim;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.ValueMap;

/**
 * Services implementing the <code>ImportHandler</code> interface are called by the Demandware Commerce importer to
 * create  actual commerce entities such as products. Most methods must return the path to the created entity's node.
 * This path is then available via the <code>ImportContext</code>'s corresponding methods given the entity's code.
 */
public interface ImportHandler {

    /**
     * Creates a catalog with the given name.
     *
     * @param ctx       The importer context
     * @param catalogId The catalog id
     * @return Path of created catalog
     * @throws RepositoryException in case the catalog could not be created
     */
    String createCatalog(ImportContext ctx, String catalogId) throws RepositoryException;

    /**
     * Creates a category from the given values.
     *
     * @param ctx    The importer context
     * @param values The category's properties
     * @return Path of created category
     * @throws RepositoryException in case the category could not be created
     */
    String createCategory(ImportContext ctx, ValueMap values) throws RepositoryException;


    /**
     * Update a product node with the given values
     *
     * @param productNode     the pre-existing product node
     * @param ctx             The importer context
     * @param productValueMap The product's properties
     * @throws RepositoryException in case the product could not be updated
     */
    void updateProduct(ImportContext ctx, Node productNode, ValueMap productValueMap) throws RepositoryException;


    /**
     * Update a variant node with the given values
     *
     * @param variantNode     the pre-existing variant node
     * @param ctx             The importer context
     * @param variantValueMap The variant's properties
     * @throws RepositoryException in case the product variant could not be updated
     */
    void updateVariantProduct(ImportContext ctx, Node variantNode, ValueMap variantValueMap) throws RepositoryException;

    /**
     * Updates an asset for a product. This is usually a product image.
     *
     * @param ctx       the importer context
     * @param imageNode the node for the image (e.g. /var/commerce/products/catalog/product/image)
     * @param values    the product's properties
     * @throws RepositoryException in case the product asset could not be updated
     */
    void updateAsset(ImportContext ctx, Node imageNode, ValueMap values, String instanceId) throws RepositoryException;
}
