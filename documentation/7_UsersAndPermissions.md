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

# Demandware Replication Service

This system user is responsible for all operations on repository needed for Demandware Connector functionality.

## Permissions

##### Read /content
User has read access to /content tree. It is needed to be able to create payload for replication of pages and assets.

##### Read and Write /home/users/system/demandware
This folder contains all users, related to Demandware Connector. Apart from "Demandware Replication Service" itself it should contain at least one more system user, that is assigned to "Demandware Replication Agent". In content-sample module it is "dwre-replication-agent-sitegenesis". This user ensures that Replication Agent handles only content it is assigned to (via users permissions).

The write access is needed to create an "oath" subnode under "Replication Agent User" node. For content-sample example it can be found at /home/users/system/demandware/dwre-replication-agent-sitegenesis/oauth
This node is used to store OAuth token, that can be reused during communication with SFCC instance.