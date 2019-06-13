<%@page session="false"%><%--
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
--%><%@page contentType="application/json"
            pageEncoding="utf-8"
            import="java.util.Comparator,
                    java.util.Map,
                    java.util.TreeMap,
                    com.day.cq.commons.TidyJSONWriter,
                    com.day.cq.replication.ContentBuilder" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%

    TreeMap<String, String> options = new TreeMap<String, String>(new Comparator<String>(){

        /**
         * Special comparator that ensures, that "durbo" comes before "flush" comes before
         * all other keys.
         */
        public int compare(String o1, String o2) {
            if (o1.equals("durbo")) o1 = "!";
            if (o2.equals("durbo")) o2 = "!";
            if (o1.equals("flush")) o1 = "!!";
            if (o2.equals("flush")) o2 = "!!";
            return o1.compareTo(o2);
        }
    });

    // we make sure, that
   Object[] builders = sling.getServices(ContentBuilder.class, null);
    if (builders != null) {
        for (Object o: builders) {
            ContentBuilder b = (ContentBuilder) o;
            String name = b.getName();
            String title = b.getTitle();
            if (name.equals("flush")) {
                title = "Dispatcher Flush";
            } else if (name.equals("durbo")) {
                title = "Default";
            }
            options.put(name, title);
        }
    }

    TidyJSONWriter w = new TidyJSONWriter(out);
    w.setTidy(true);
    w.array();
    for (Map.Entry<String, String> e: options.entrySet()) {
        w.object();
        w.key("value").value(e.getKey());
        w.key("text").value(e.getValue());
        w.endObject();
    }
    w.endArray();
%>
