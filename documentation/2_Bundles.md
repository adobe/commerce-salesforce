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

### Bundles Overview
Backend implementation consists of 6 bundles.

![Bundles](/documentation/images/bundles.png)

#### core bundle 
Provides functionality to connect to SFCC instance and preview of SFCC content on AEM pages in Edit Mode.

#### replication bundle 
Have a custom Replication Agent implementation, which allows to push content from AEM to SFCC.

#### pim bundle 
Is responsible for Product Import. It imports products and assets from SFCC into AEM.

#### commerce bundle
Contains CommerceService implementation for Demandware (SFCC).

#### init bundle 
The bundle contains code to import templates **from AEM instance to SFCC**. The tool can be found [Here](http://localhost:4502/etc/demandware/init.html).
It is possible to configure which exactly templates are imported.
The bundle is a part of the Sample Content. It is not installed with main *cq-commerce-demandware-content* package.

#### libs bundle
Installs custom libraries.