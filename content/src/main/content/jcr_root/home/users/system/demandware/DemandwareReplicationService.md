# Demandware Replication Service

This system user is responsible for all operations on repository needed for Demandware Connector functionality.

## Permissions

User has read access to /content tree. It is needed to be able to create payload for replication of pages and assets.

User has read and write access to /home/users/system/demandware. 
This folder contains all users, related to Demandware Connector. Apart from "Demandware Replication Service" itself it should contain at least one more system user, that is assigned to "Demandware Replication Agent". In content-sample module it is "dwre-replication-agent-sitegenesis". This user ensures that Replication Agent handles only content it is assigned to (via users permissions).

The write access is needed to create an "oath" subnode under "Replication Agent User" node. For content-sample example it will be under /home/users/system/demandware/dwre-replication-agent-sitegenesis/oauth
This node is used to store OAuth token, that can be reused during communication with SFCC instance.