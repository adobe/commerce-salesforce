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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.day.cq.commons.jcr.JcrUtil;

/**
 * Helper to collect various utility methods used by the importer.
 */
public class ImporterUtil {

    /**
     * Create a valid product path based on the parent category and product identifier.
     *
     * @param productId          The id of the product.
     * @param parentCategoryPath The path of the parent category.
     * @return the product path in /var/commerce/products/...
     */
    public static String getProductPath(final String productId, final String parentCategoryPath) {
        return parentCategoryPath + "/" + JcrUtil.createValidName(productId);
    }

    /**
     * Creates or updates the product axis property 'cq:productVariantAxes'.
     *
     * @param node the product or variant node
     * @param axis then name of the axis
     * @throws RepositoryException in case of an error
     */
    public static void registerVariantAxis(Node node, final String axis) throws RepositoryException {

        // find the product node first
        while (node.getProperty(CommerceConstants.PN_COMMERCE_TYPE).getString().equals("variant")) {
            node = node.getParent();
        }

        // create / update the axis property
        final String[] axes;
        if (node.hasProperty(CommerceConstants.PN_PRODUCT_VARIANT_AXES)) {
            final Value[] values = node.getProperty(CommerceConstants.PN_PRODUCT_VARIANT_AXES).getValues();
            axes = new String[values.length + 1];
            for (int i = 0; i < values.length; i++) {
                axes[i] = values[i].getString();
                if (axes[i].equals(axis)) {
                    return;
                }
            }
            axes[values.length] = axis;
        } else {
            axes = new String[1];
            axes[0] = axis;
        }
        node.setProperty(CommerceConstants.PN_PRODUCT_VARIANT_AXES, axes);
    }
}
