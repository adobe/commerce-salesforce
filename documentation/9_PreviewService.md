<!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  ~ Copyright 2018 Adobe Systems Incorporated
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
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->

## Preview Service 
Preview Mode is planned at AEM sites to view the components of the Salesforce Instance on the page. To do this, however, 
the correct Demandware client must be addressed for each page. Therefor a preview service configuration with 
following properties takes place:

#### Properties
* *"Preview endpoint"* - **mandatory**. Description for endpointPage (endpointPage)
* *"Preview endpoint for category pages"*- **mandatory**. Description for endpointSearch (endpointSearch)
* *"Preview template path"*- **mandatory**. Description for template (template)
* *"Preview default site"*- **mandatory**. Description for site (site)
* *"Enable preview cache"* - optional. Description for cache.enabled (cache.enabled)
* *"Caching time in seconds"*- optional. Description for cache.time (cache.time)
* *"Enable storefront protection"*- optional. Description for storefront.protected (storefront.protected)
* *"Protected storefront user"*- optional. Description for storefront.user (storefront.user)
* *"Protected storefront password"*- optional. Description for storefront.password (storefront.password)
* *"Instance id"*- **mandatory**. Preview Service instance id that corresponds to Replication Agent config (instance.id)

![Preview Service Configuration](/documentation/images/PreviewServiceConfiguration.png)

