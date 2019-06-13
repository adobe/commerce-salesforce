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

package com.adobe.cq.commerce.demandware;

import org.apache.sling.api.resource.Resource;

/**
 * Component preview service to render preview for Demandware placeholder components. The
 * rendered preview can be cached to avoid unnecessary backend requests.
 */
public interface PreviewService {

    /**
     * Get the prerendered content fragment form the backend service.
     *
     * @param resource the component resource
     * @param useCache <code>true</code> to cache the prerendered content fragment
     * @return the prerendered content fragment
     */
    String previewComponent(Resource resource, boolean useCache);

    /**
     * Get the prerendered content fragment form the backend service.
     *
     * @param resource  the component resource
     * @param useCache  <code>true</code> to cache the prerendered content fragment
     * @param selectors optional selectors used to render
     * @return the prerendered content fragment
     */
    String previewComponent(Resource resource, boolean useCache, String... selectors);

    /**
     * Get the prerendered content fragment backed by a shop category form the backend service.
     *
     * @param resource the component resource
     * @param useCache <code>true</code> to cache the prerendered content fragment
     * @return the prerendered content fragment
     */
    String previewCategoryComponent(Resource resource, boolean useCache);
}
