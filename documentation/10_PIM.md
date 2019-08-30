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

## Product Import Managment  
Products can be imported and stored within the instance. Therefor we need to import XML files that contain the product information. 
Once these products are imported, its easy to add them on specific sites by choosing the product id. 
[AEM Tutorial Commerce Video](https://helpx.adobe.com/experience-manager/kt/commerce/using/demandware-feature-video-understand.html)

### How to import XML product files
-	Configure *"Asset download endpoint"* within DemandwareClient Configuration
-	Within AEM navigate to Commerce ->  [Products](http://localhost:4502/aem/products.html/var/commerce/products)
-	Click “Create” -> “Import Products” and a dialogue opens
-	Configure the Demandware store name, select the instance id of corresponding Demandware Client and add your XML file
-	Click import and all products are imported and found within Products or in the [repository](http://localhost:4502/crx/de/index.jsp#/var/commerce/products)


