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
  
## Demandware Client 
DemandwareClient is responsible for connecting to Salesforce Commerce Cloud. It provides HttpClient and all required endpoints. DemandwareClient is an OSGI configuration factory. To configure a new connection to a SFCC instance add an OSGI config. Below is the example from cq-commerce-demandware-multi-sample-content package installation. 

![DemandwareClient configuration factory](/documentation/images/DemandwareClientConfigFactory.png)

Demandware Client will not be activated is any of mandatory properties is blank.

#### Properties
* *"Instance endpoint ip or hostname"* - **mandatory**. Hostname of your SFCC instance. Should be entered **without** scheme (http, https etc). Example: **myinstance.demandware.net**
* *"Socket timeout"* - optional.
* *"HTTP connection timeout"* - optional.
* *"Local network interface to be used"* - optional.
* *"SSL version"* - default value is "TLSv1.1".
* *"Keystore type"* - default value is "JKS". 
* *"Keystore path"* - optional.
* *"Keytsore password"* - optional.
* *"Asset download endpoint"* - **mandatory**. Used to upload assets from SFCC during PIM import into AEM.
* *"WebDAV instance endpoint"* - optional. Used to replicate Assets from DAM to SFCC WebDAV folder. 
* *"WebDAV user"* - optional. 
* *"WebDAV user password"* - optional.
* *"Instance id"* - **mandatory**. Unique ID for SFCC instance, used to connect this Client configuration with other parts of Connector: Replication Agent, PIM module, Preview functionality.

![DemandwareClient configuration](/documentation/images/DemandwareClientConfig.png)

