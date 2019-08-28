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

## Multiple SFCC instances support  
Single AEM instance can connect to multiple SFCC instances. Each SFCC instance should have a unique identifier – **instanceId**, that is used to match configurations for different parts of the Connector. **instanceId** is needed to configure replication to SFCC, content preview of AEM pages in Edit Mode and product import. 

In the example below we have two sites hosted on AEM Author instance: **US SiteGenesis** and **EU SiteGenesis**. Each site is connected to its own Salesforce Commerce Cloud instance. 

![Multiple SFCC](/documentation/images/multipleSFCC.png)

### Replication
Content replication from AEM to SFCC is done via AEM Replication. Custom Replication Agent implementation can be found in *cq-commerce-demandware-replication* bundle. Sample configured agents are installed with *content-sample* and *content-multi-sample* packages.
For each SFCC instance separate Replication Agent should be configured. Replication Agent should have custom Resource Type:
*sling:resourceType commerce/demandware/components/replication/demandware*

![Replication Agent Edit Dialog](/documentation/images/ReplicationAgent.png)

Agent edit dialog will have two tabs. To enable replication next properties should be configured:
* Name - agent name
* Enabled - should be checked
* Serilalization Type - should be "Demandware ContentBuilder". 
* Agent User Id - system user that have access to content that will be replicated. When creating a user please 
* URI - must be prefixed with "demandware://" and contain instanceId, for example: "demandware://sitegenesis"

