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

## Troubleshooting
Check **error.log** and **project-dwre.log** for detailed messages.

#### Replication Agent not valid
If configured Replication Agent is not valid - check if Replication Bundle is active in OSGI Console.
If Replication Bundle is not active and missing dependencies - check if AEM6.4.5 Service Pack is installed.

#### Page Replication failed
Go to [OSGI Console](http://localhost:4502/system/console/configMgr) and verify there is **"Demandware Client"** config for instanceId, configured in Replication Agent.
Verify that Demandware Client config is valid, mandatory properties are *"Instance endpoint ip or hostname""*, *"Asset download endpoint"* and *"Instance id"*.
**Note!** Invalid configuration will be listed in /system/console/configMgr. To view valid configured clients go to [Components](http://localhost:4502/system/console/components), search for "DemandwareClient" and check the status=active.

Go to OSGI Console and verify there is **"Demandware Access Token Provider"** config for instanceId, configured in Replication Agent.

Verify that system user, configured in Agent dialog exists and have jcr:read permissions to replicated content AND jcr:read and jcr:write permissions to self-node. 
For example check users from Sample Content.

Verify dwreSite property is configured for the page (or parent) and such Site exists in SFCC.


#### Asset Replication failed

Verify WebDav username and password configured in Demandware Client.
Verify "dwreLibrary" property is set in /content/dam/path_to_asset or parent and specified Library exists in SFCC.
Verify "DAMAssetPlugin" OSGI config. If needed, modify **"The relative path of the WEBDAV share for static assets""** property.