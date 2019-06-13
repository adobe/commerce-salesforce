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

package com.adobe.cq.commerce.demandware.components;

import com.adobe.cq.commerce.demandware.PreviewService;
import com.adobe.cq.sightly.WCMUsePojo;

/**
 * PoJo for use of @{link PreviewService} in a Sightly component.
 */
public class PreviewHelper extends WCMUsePojo {

    private PreviewService previewService;

    @Override
    public void activate() throws Exception {
        previewService = getSlingScriptHelper().getService(PreviewService.class);

    }

    public String getPreviewContent() {
        return previewService != null ? previewService.previewComponent(getResource(), true) : "";
    }

    public String getPreviewContentNoCache() {
        return previewService != null ? previewService.previewComponent(getResource(), false) : "";
    }

    public String getPreviewCategoryContent() {
        return previewService != null ? previewService.previewCategoryComponent(getResource(), true) : "";
    }

    public String getPreviewCategoryContentNoCache() {
        return previewService != null ? previewService.previewCategoryComponent(getResource(), false) : "";
    }
}