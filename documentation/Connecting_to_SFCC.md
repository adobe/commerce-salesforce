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

# Connecting to Salesforce Commerce Cloud 

## Multiple SFCC instances support  
Single AEM instance can connect to multiple SFCC instances. Each SFCC instance should have a unique identifier – **instanceId**, that is used to match configurations for different parts of the Connector. **instanceId** is needed to configure replication to SFCC, content preview of AEM pages in Edit Mode and product import. 

In example below we have two sites hosted on AEM Author instance: **US SiteGenesis** and **EU SiteGenesis**. Each site is connected to its own Salesforce Commerce Cloud instance. 

## Demandware Client 
DemandwareClient is responsible for connecting to Salesforce Commerce Cloud. It provides HttpClient and different endpoints. DemandwareClient is an OSGI configuration factory. To configure a new connection to a SFCC instance add an OSGI config. Below is the example from cq-commerce-demandware-multi-sample-content package installation. 

## Demandware Access Token Provider 
This configuration factory that must be configured to enable connection between AEM and Salesforce Commerce Cloud. 
//TODO

## Content mapping 
//TODO