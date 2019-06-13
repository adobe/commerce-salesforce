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

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import com.adobe.cq.commerce.demandware.pim.DemandwareCommerceConstants;
import com.adobe.cq.commerce.demandware.pim.ImportContext;

/**
 * Demandware XML parser.
 */
public class DemandwareXMLReader {

    public void readFromXML(InputStream is, ImportContext ctx) throws XMLStreamException, RepositoryException {
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        XMLStreamReader reader = null;
        try {
            reader = inputFactory.createXMLStreamReader(is);
            readCatalog(reader, ctx);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void readCatalog(XMLStreamReader reader, ImportContext ctx) throws XMLStreamException, RepositoryException {
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    final String elementName = reader.getLocalName();
                    if (elementName.equals("catalog")) {
                        ctx.setCatalogId(reader.getAttributeValue(null, "catalog-id"));
                    } else if (elementName.equals("category")) {
                        ctx.addCategory(readCategory(reader));
                    } else if (elementName.equals("category-assignment")) {
                        ctx.addProductMapping(readProductMapping(reader));
                    } else if (elementName.equals("product")) {
                        ctx.addProduct(readProduct(reader));
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    break;
            }
        }
    }

    private ValueMap readCategory(XMLStreamReader reader) throws XMLStreamException {
        final ValueMap category = new ValueMapDecorator(new HashMap<String, Object>());
        category.put(DemandwareCommerceConstants.ATTRIBUTE_CATEGORY_ID, reader.getAttributeValue(null, "category-id"));
        final Map<String, Object> attributes = new HashMap<String, Object>();

        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    final String elementName = reader.getLocalName();
                    if (elementName.equals("display-name")) {
                        final String lang = reader.getAttributeValue(0);
                        if (StringUtils.equals(lang, "x-default")) {
                            attributes.put("jcr:title", readCharacters(reader));
                        } else {
                            attributes.put("jcr:title." + lang, readCharacters(reader));
                        }
                    } else if (elementName.equals("online-flag")) {
                        attributes.put("online-flag", readCharacters(reader));
                    } else if (elementName.equals("parent")) {
                        category.put(DemandwareCommerceConstants.ATTRIBUTE_PARENT_CATEGORY_ID, readCharacters(reader));
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    category.put(DemandwareCommerceConstants.ATTRIBUTE_MAPPINGS, attributes);
                    return category;
            }
        }
        throw new XMLStreamException("Premature end of file");
    }

    private ValueMap readProductMapping(XMLStreamReader reader) throws XMLStreamException {
        final ValueMap mapping = new ValueMapDecorator(new HashMap<String, Object>());
        mapping.put(DemandwareCommerceConstants.ATTRIBUTE_PRODUCT_ID, reader.getAttributeValue(null, "product-id"));
        mapping.put(DemandwareCommerceConstants.ATTRIBUTE_CATEGORY_ID, reader.getAttributeValue(null, "category-id"));

        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    final String elementName = reader.getLocalName();
                    if (elementName.equals("primary-flag")) {
                        mapping.put(DemandwareCommerceConstants.ATTRIBUTE_CATEGORY_PRIMARY,
                                Boolean.parseBoolean(readCharacters(reader)));
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    return mapping;
            }
        }
        throw new XMLStreamException("Premature end of file");

    }

    private ValueMap readProduct(XMLStreamReader reader) throws XMLStreamException {
        final ValueMap product = new ValueMapDecorator(new HashMap<String, Object>());
        product.put(DemandwareCommerceConstants.ATTRIBUTE_PRODUCT_ID, reader.getAttributeValue(null, "product-id"));
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("identifier", reader.getAttributeValue(null, "product-id"));

        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    final String elementName = reader.getLocalName();
                    if (elementName.equals("upc")) {
                        final String identifier = readCharacters(reader);
                        if (StringUtils.isNotBlank(identifier)) {
                            attributes.put("identifier", identifier);
                        }
                    } else if (elementName.equals("display-name")) {
                        final String lang = reader.getAttributeValue(0);
                        if (StringUtils.equals(lang, "x-default")) {
                            attributes.put("jcr:title", readCharacters(reader));
                        } else {
                            attributes.put("jcr:title." + lang, readCharacters(reader));
                        }
                    } else if (elementName.equals("short-description")) {
                        final String lang = reader.getAttributeValue(0);
                        if (StringUtils.equals(lang, "x-default")) {
                            attributes.put("summary", readCharacters(reader));
                        } else {
                            attributes.put("summary." + lang, readCharacters(reader));
                        }
                    } else if (elementName.equals("long-description")) {
                        final String lang = reader.getAttributeValue(0);
                        if (StringUtils.equals(lang, "x-default")) {
                            attributes.put("features", readCharacters(reader));
                        } else {
                            attributes.put("features." + lang, readCharacters(reader));
                        }
                    } else if (elementName.equals("page-attributes")) {
                        attributes.putAll(readProductPageAttributes(reader));
                    } else if (elementName.equals("variations")) {
                        final Map<String, Object> variations = readProductVariations(reader);
                        final Map<String, Object> productAttributes = (Map<String, Object>) variations.get
                                (DemandwareCommerceConstants.ATTRIBUTE_ATTR_MAPPINGS);
                        if (productAttributes != null && productAttributes.size() > 0) {
                            product.put(DemandwareCommerceConstants.ATTRIBUTE_ATTR_MAPPINGS, productAttributes);
                        }
                        final Set<String> productVariants = (Set<String>) variations.get
                                (DemandwareCommerceConstants.ATTRIBUTE_VARIANTS);
                        if (productVariants != null && productVariants.size() > 0) {
                            product.put(DemandwareCommerceConstants.ATTRIBUTE_VARIANTS, productVariants);
                        }
                    } else if (elementName.equals("custom-attributes")) {
                        final Map<String, String> customAttributes = readProductCustomAttributes(reader);
                        if (customAttributes != null && customAttributes.size() > 0) {
                            product.put(DemandwareCommerceConstants.ATTRIBUTE_ATTR_MAPPINGS_CUSTOM, customAttributes);
                        }
                    } else if (elementName.equals("images")) {
                        final MultiMap assets = readProductImages(reader);
                        if (assets != null && assets.size() > 0) {
                            product.put(DemandwareCommerceConstants.ATTRIBUTE_ASSETS, assets);
                        }
                    } else if (elementName.equals("online-flag")) {
                        attributes.put("online-flag", readCharacters(reader));
                    } else if (elementName.equals("available-flag")) {
                        attributes.put("available-flag", readCharacters(reader));
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if (reader.getLocalName().equals("product")) {
                        product.put(DemandwareCommerceConstants.ATTRIBUTE_MAPPINGS, attributes);
                        return product;
                    }
            }
        }
        throw new XMLStreamException("Premature end of file");
    }

    private Map<String, Object> readProductVariations(XMLStreamReader reader) throws XMLStreamException {
        final Map<String, Object> variations = new HashMap<String, Object>();
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    final String elementName = reader.getLocalName();
                    if (elementName.equals("attributes")) {
                        variations.put(DemandwareCommerceConstants.ATTRIBUTE_ATTR_MAPPINGS,
                                readProductVariationAttributes(reader));
                    } else if (elementName.equals("variants")) {
                        variations.put(DemandwareCommerceConstants.ATTRIBUTE_VARIANTS, readProductVariants(reader));
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if (reader.getLocalName().equals("variations")) {
                        return variations;
                    }
            }
        }
        throw new XMLStreamException("Premature end of file");
    }

    private Map<String, Object> readProductVariationAttributes(XMLStreamReader reader) throws XMLStreamException {
        final Map<String, Object> variationAttributes = new HashMap<String, Object>();
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    final String elementName = reader.getLocalName();
                    if (elementName.equals("variation-attribute")) {
                        final Map<String, Object> attribute = readProductVariationAttribute(reader);
                        variationAttributes.put(
                                (String) attribute.get(DemandwareCommerceConstants.ATTRIBUTE_ATTR_MAPPINGS_ID),
                                attribute);
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if (reader.getLocalName().equals("attributes")) {
                        return variationAttributes;
                    }
            }
        }
        throw new XMLStreamException("Premature end of file");
    }

    private Map<String, Object> readProductVariationAttribute(XMLStreamReader reader) throws XMLStreamException {
        final Map<String, Object> variationAttribute = new HashMap<String, Object>();
        variationAttribute.put("attribute-id", reader.getAttributeValue(null, "attribute-id"));
        variationAttribute.put("variation-attribute-id", reader.getAttributeValue(null, "variation-attribute-id"));

        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    final String elementName = reader.getLocalName();
                    if (elementName.equals("display-name")) {
                        final String lang = reader.getAttributeValue(0);
                        if (StringUtils.equals(lang, "x-default")) {
                            variationAttribute.put("attribute-title", readCharacters(reader));
                        } else {
                            variationAttribute.put("attribute-title." + lang, readCharacters(reader));
                        }
                    } else if (elementName.equals("variation-attribute-values")) {

                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if (reader.getLocalName().equals("variation-attribute")) {
                        return variationAttribute;
                    }
            }
        }
        throw new XMLStreamException("Premature end of file");
    }

    private Set<String> readProductVariants(XMLStreamReader reader) throws XMLStreamException {
        final Set<String> variants = new HashSet<String>();
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    final String elementName = reader.getLocalName();
                    if (elementName.equals("variant")) {
                        variants.add(reader.getAttributeValue(null, "product-id"));
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if (reader.getLocalName().equals("variants")) {
                        return variants;
                    }
            }
        }
        throw new XMLStreamException("Premature end of file");
    }

    private Map<String, String> readProductPageAttributes(XMLStreamReader reader) throws XMLStreamException {
        final Map<String, String> pageAttributes = new HashMap<String, String>();
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    final String elementName = reader.getLocalName();
                    if (elementName.equals("page-title")) {
                        final String lang = reader.getAttributeValue(0);
                        if (StringUtils.equals(lang, "x-default")) {
                            pageAttributes.put("jcr:description", readCharacters(reader));
                        } else {
                            pageAttributes.put("jcr:description." + lang, readCharacters(reader));
                        }
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    return pageAttributes;
            }
        }
        throw new XMLStreamException("Premature end of file");
    }

    private Map<String, String> readProductCustomAttributes(XMLStreamReader reader) throws XMLStreamException {
        final Map<String, String> customAttributes = new HashMap<String, String>();
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    final String elementName = reader.getLocalName();
                    if (elementName.equals("custom-attribute")) {
                        customAttributes.put(reader.getAttributeValue(null, "attribute-id"), readCharacters(reader));
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    return customAttributes;
            }
        }
        throw new XMLStreamException("Premature end of file");
    }

    private MultiMap readProductImages(XMLStreamReader reader) throws XMLStreamException {
        final MultiMap images = new MultiValueMap();
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    final String elementName = reader.getLocalName();
                    if (elementName.equals("image-group")) {
                        final Map<String, Object> image = new HashMap<String, Object>();
                        final String viewType = StringUtils.replace(reader.getAttributeValue(null, "view-type"),
                                "large", "original");
                        final String variationValue = StringUtils.defaultIfBlank(reader.getAttributeValue(null,
                                "variation-value"), "0");
                        final String imagePath = readProductImage(reader);
                        // at the moment we are only interested in the large image plus all other images, small &
                        // medium renditions can be ignored
                        boolean interestedImage = !StringUtils.contains("medium small swatch", viewType);

                        boolean equalToBaseAsset = false;
                        if (interestedImage && !StringUtils.equals(variationValue, "0")) {
                            final List<Map<String, Object>> baseAssets = (List<Map<String, Object>>) images.get("0");
                            if (baseAssets != null) {
                                for (Map<String, Object> baseAsset : baseAssets) {
                                    if (StringUtils.equals((String) baseAsset.get(DemandwareCommerceConstants
                                            .ATTRIBUTE_ASSET_PATH), imagePath)) {
                                        equalToBaseAsset = true;
                                    }
                                }
                            }
                        }

                        if (interestedImage && !equalToBaseAsset && StringUtils.isNotBlank(imagePath)) {
                            image.put(DemandwareCommerceConstants.ATTRIBUTE_ASSET_PATH, imagePath);
                            image.put(DemandwareCommerceConstants.ATTRIBUTE_ASSET_TYPE, viewType);
                            images.put(variationValue, image);
                        }
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if (reader.getLocalName().equals("images")) {
                        return images;
                    }
            }
        }
        throw new XMLStreamException("Premature end of file");
    }

    private String readProductImage(XMLStreamReader reader) throws XMLStreamException {
        String imagePath = null;
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    final String elementName = reader.getLocalName();
                    if (elementName.equals("image")) {
                        imagePath = reader.getAttributeValue(null, "path");
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    return imagePath;
            }
        }
        throw new XMLStreamException("Premature end of file");
    }

    private String readCharacters(XMLStreamReader reader) throws XMLStreamException {
        StringBuilder result = new StringBuilder();
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.CHARACTERS:
                case XMLStreamReader.CDATA:
                    result.append(reader.getText());
                    break;
                case XMLStreamReader.END_ELEMENT:
                    return result.toString();
            }
        }
        throw new XMLStreamException("Premature end of file");
    }
}
