/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe Systems Incorporated and others
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
package com.adobe.cq.commerce.demandware.replication.content.naming;

import org.apache.sling.api.resource.Resource;

/**
 * If we have a multi language / multi region site content assest that belong
 * together have to share the same resource id on salesforce.
 *
 * The default strategy in case of live relationships is to use the name of
 * the source resource.
 *
 * Implement this service interface and register as an OSGi service with a
 * service ranking above 0 to knock out the default strategy with your own.
 */
public interface ContentAssetNameResolver {

    String resolve(Resource resource);

}
