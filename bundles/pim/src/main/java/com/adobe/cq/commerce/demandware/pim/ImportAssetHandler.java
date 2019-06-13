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

import java.util.Map;

import com.adobe.granite.asset.api.Asset;

/**
 * Central import context used by the Demandware Commerce importer and <code>ImportHandler</code>.
 */
public interface ImportAssetHandler {

    /**
     * Create an asset. The properties parameter contains all the information needed to retrieve and create the asset
     * . Details on how to do that are up to the implementation. Also which renditions are created and how.
     *
     * @param ctx the <code>ImportContext</code>
     * @param properties the properties
     * @return the new or updated asset
     */
    Asset retrieveAsset(ImportContext ctx, Map<String, Object> properties);
}
