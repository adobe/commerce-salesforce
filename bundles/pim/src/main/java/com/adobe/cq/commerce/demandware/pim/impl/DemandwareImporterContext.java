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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.commerce.demandware.pim.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.pim.ImportContext;

/**
 * ImportContext context implementation.
 */
public class DemandwareImporterContext implements ImportContext {

    protected Resource baseResource;
    protected ResourceResolver resolver;
    protected PrintWriter writer;

    protected String catalogId;

    protected Map<String, ValueMap> categoryCache;
    protected Map<String, String> categoryPathCache;
    protected MultiMap categoryMapping;
    protected Map<String, String> productMapping;
    protected Map<String, ValueMap> productCache;


    public DemandwareImporterContext(final Resource baseResource, final ResourceResolver resolver,
                                     final PrintWriter writer) {
        this.baseResource = baseResource;
        this.resolver = resolver;
        this.writer = writer;

        this.categoryCache = new HashMap<String, ValueMap>();
        this.categoryPathCache = new HashMap<String, String>();
        this.categoryMapping = new MultiValueMap();
        this.productMapping = new HashMap<String, String>();
        this.productCache = new HashMap<String, ValueMap>();
    }

    @Override
    public Resource getBaseResource() {
        return baseResource;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    @Override
    public String getCatalogId() {
        return catalogId;
    }

    @Override
    public void setCatalogId(final String catalogId) {
        this.catalogId = catalogId;
    }

    @Override
    public void addCategory(ValueMap category) {
        final String categoryId = category.get(DemandwareCommerceConstants.ATTRIBUTE_CATEGORY_ID, String.class);
        final String parentCategoryId = category.get(DemandwareCommerceConstants.ATTRIBUTE_PARENT_CATEGORY_ID, String
                .class);
        categoryCache.put(categoryId, category);
        if (StringUtils.isNotBlank(parentCategoryId)) {
            categoryMapping.put(parentCategoryId, categoryId);
        } else {
            categoryMapping.put(getCatalogId(), categoryId);
        }

    }

    @Override
    public ValueMap getCategory(String categoryId) {
        return categoryCache.get(categoryId);
    }

    @Override
    public List<String> getChildCategories(final String parentCategoryId) {
        return (List<String>) categoryMapping.get(parentCategoryId);
    }

    @Override
    public void addCategoryPath(String categoryId, String path) {
        categoryPathCache.put(categoryId, path);
    }

    @Override
    public void addProductMapping(ValueMap mapping) {
        final String categoryId = mapping.get(DemandwareCommerceConstants.ATTRIBUTE_CATEGORY_ID, String.class);
        final String productId = mapping.get(DemandwareCommerceConstants.ATTRIBUTE_PRODUCT_ID, String.class);
        final boolean primary = mapping.containsKey(DemandwareCommerceConstants.ATTRIBUTE_CATEGORY_PRIMARY);
        if (primary) {
            productMapping.put(productId, categoryId);
        }
    }

    @Override
    public void addProduct(ValueMap product) {
        final String productId = product.get(DemandwareCommerceConstants.ATTRIBUTE_PRODUCT_ID, String.class);
        productCache.put(productId, product);
    }

    @Override
    public ValueMap getProduct(String productId) {
        return productCache.get(productId);
    }

    @Override
    public Map<String, String> getProductMappings() {
        return productMapping;
    }

    @Override
    public String getCategoryPath(String categoryId) {
        return categoryPathCache.get(categoryId);
    }

    @Override
    public void info(String op, String msg) {
        if (writer != null) {
            writer.append(op).append(" ").append(msg);
            writer.flush();
        }
    }
}
