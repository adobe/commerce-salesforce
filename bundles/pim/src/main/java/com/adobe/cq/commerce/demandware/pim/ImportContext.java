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

import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

/**
 * Central import context used by the Demandware Commerce importer and <code>ImportHandler</code>.
 */
public interface ImportContext {

    /**
     * Returns the import's base resource.
     *
     * @return The resource
     */
    Resource getBaseResource();

    /**
     * Returns the resource resolver.
     *
     * @return The resource resolver
     */
    ResourceResolver getResourceResolver();

    /**
     * Get the catalog id of the master or sales catalog.
     *
     * @return the catalog id
     */
    String getCatalogId();

    /**
     * Set the catalog id.
     *
     * @param catalogId the catalog id
     */
    void setCatalogId(String catalogId);

    /**
     * Adds a category's path to the cache.
     *
     * @param category The category's data
     */
    void addCategory(ValueMap category);

    /**
     * Returns all child categories of the given category.
     *
     * @param parentCategoryId the parent category's identifier
     * @return A list of all child category identifiers
     */
    List<String> getChildCategories(String parentCategoryId);

    /**
     * Returns a category's data from cache.
     *
     * @param categoryId the category's identifier
     * @return the category data
     */
    ValueMap getCategory(String categoryId);

    /**
     * Adds a category's path to the cache.
     *
     * @param categoryId The category's identifier
     * @param path       The category's path
     */
    void addCategoryPath(String categoryId, String path);

    /**
     * Returns a category's path from cache.
     *
     * @param categoryId The category's identifier
     * @return The path
     */
    String getCategoryPath(String categoryId);

    /**
     * Adds a product to category mapping to the cache.
     *
     * @param valueMap the mapping data
     */
    void addProductMapping(ValueMap valueMap);

    /**
     * Get all product to category mappings.
     *
     * @return the product to category mappings
     */
    Map<String, String> getProductMappings();

    /**
     * Adds a product to the cache.
     *
     * @param valueMap the product data.
     */
    void addProduct(ValueMap valueMap);


    /**
     * Returns a product's data from the cache
     * @param productId the product identifier
     * @return the product data
     */
    ValueMap getProduct(String productId);

    /**
     * Writes an info message to the import log, e.g.:
     *
     * {@code A /content/geometrixx-outdoors/en_US/men A /content/geometrixx-outdoors/en_US/women ... }
     *
     * @param op  The operation that was performed, e.g. A for added
     * @param msg The log message
     */
    void info(String op, String msg);
}
