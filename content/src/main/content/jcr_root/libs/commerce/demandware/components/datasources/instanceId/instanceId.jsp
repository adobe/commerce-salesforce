<%--
/*******************************************************************************
 * Copyright 2018 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
--%>
<%@page session="false"
        import="java.util.ArrayList,
                java.util.HashMap,
                java.util.Map,
                org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ResourceMetadata,
                org.apache.sling.api.resource.ResourceResolver,
                org.apache.sling.api.wrappers.ValueMapDecorator,
                com.adobe.cq.commerce.demandware.DemandwareClientProvider,
                com.adobe.cq.commerce.demandware.DemandwareClient,
                com.adobe.granite.ui.components.ds.DataSource,
                com.adobe.granite.ui.components.ds.EmptyDataSource,
                com.adobe.granite.ui.components.ds.SimpleDataSource,
                com.adobe.granite.ui.components.ds.ValueMapResource" %>
<%
%><%@include file="/libs/granite/ui/global.jsp" %><%
    final ResourceResolver resolver = resourceResolver;
    final DemandwareClientProvider demandwareClientProvider = sling.getService(DemandwareClientProvider.class);
    
    final ArrayList<Resource> resourceList = new ArrayList<Resource>();
    if (demandwareClientProvider != null) {
        Map<String, DemandwareClient> demandwareClientMap = demandwareClientProvider.getDemandwareClients();
        int count = 0;
        for (DemandwareClient value : demandwareClientMap.values()) {
            count++;
            HashMap map = new HashMap();
            map.put("text", value.getInstanceId());
            map.put("value", value.getInstanceId());
            ValueMapResource instanceIdResource = new ValueMapResource(resourceResolver, new ResourceMetadata(), "",
                    new ValueMapDecorator(map));
            resourceList.add(instanceIdResource);
        }
    }
    
    DataSource ds;
    if (resourceList.size() == 0) {
        ds = EmptyDataSource.instance();
    } else {
        
        ds = new SimpleDataSource(resourceList.iterator());
    }
    
    request.setAttribute(DataSource.class.getName(), ds);
%>