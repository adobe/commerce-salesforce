<%@page session="false"
        contentType="text/html"
        pageEncoding="utf-8"
        import="com.day.cq.replication.Agent,
            com.day.cq.replication.AgentManager" %><%--
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
--%><%
%><%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %><%
%><cq:defineObjects /><%

    String agentName = currentPage.getName();
    AgentManager agentMgr = sling.getService(AgentManager.class);
    Agent agent = agentName == null ? null : agentMgr.getAgents().get(agentName);
    String title = agent == null ? null : agent.getConfiguration().getName();
    if (title == null) {
        title = agentName;
    }
%><html><head>
    <style type="text/css">
        code {
            font-family:lucida console, courier new, monospace;
            font-size:12px;
            white-space:nowrap;
        }
    </style>
    <title>AEM Replication | Log for <%= xssAPI.encodeForHTML(title) %></title>
</head>
<body bgcolor="white"><code><%
    if (agent == null) {
        %>no such agent: <%= xssAPI.encodeForHTML(agentName) %><br><%
    } else {
        for (String line: agent.getLog().getLines()) {
            // convert time 
            String date = "";
            int idx = line.indexOf(' ');
            if (idx > 0) {
                try {
                    long time = Long.parseLong(line.substring(0, idx));
                    line = line.substring(idx);
                    date = com.day.cq.wcm.commons.Constants.DATE_DEFAULT.format(time);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            %><%= date %><%= xssAPI.encodeForHTML(line) %><br>
<%
        }
    }
%></code>
<a name="end"></a>
</body></html>
