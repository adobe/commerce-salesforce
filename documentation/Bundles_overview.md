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

![Bundles](https://github.com/ackoch/commerce-salesforce/blob/master/documentation/images/bundles.png?raw=true "Bundles")
![Bundles](https://raw.githubusercontent.com/ackoch/commerce-salesforce/master/documentation/images/bundles.png "Bundles")
documentation/images/bundles.png
![Bundles](/documentation/images/bundles.png)
![Bundles](/documentation/images/bundles.png?raw=true)
![Bundles](documentation/images/bundles.png?raw=true)
![alt text](https://github.com/adam-p/markdown-here/raw/master/src/common/images/icon48.png "Logo Title Text 1")

#### core bundle 
provides functionality to connect to SFCC instance and preview of SFCC content on AEM pages in Edit Mode.

#### replication bundle 
have a custom Replication Agent implementation, which allows to push content from AEM to SFCC.

#### pim bundle 
is responsible for Product Import. It imports products and assets from SFCC into AEM.