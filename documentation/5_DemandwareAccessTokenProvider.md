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

## Demandware Access Token Provider 
This configuration factory that must be configured to enable connection between AEM and Salesforce Commerce Cloud.
One config required for each SFCC instance.

When DemandwareClient send a request to SFCC instance it use and OAuth token. Provider allows Client to obtain a token and store it in CRX for reusing.
Token is stored under "Replication Agent system user" node. For example in Sample Content:  
/home/users/system/demandware/dwre-replication-agent-sitegenesis/oauth

If token is not stored, but replication is successful check if user has write permissions on his own node.

### Properties

![Demandware Access Token Provider](/documentation/images/DemandwareAccessTokenProvider.png)

