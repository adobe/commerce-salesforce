/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe Systems Incorporated
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
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.adobe.cq.commerce.demandware;

public interface DemandwareCommerceConstants {
    
    /**
     * supported delivery types
     **/
    String TYPE_OCAPI = "ocapi";
    String TYPE_WEBDAV = "webdav";
    
    String DWRE_RENDERING_SELECTOR = "vm";
    
    /**
     * base attributes for internal JSON object
     **/
    String ATTR_API_TYPE = "api-type";
    String ATTR_API_ENDPOINT = "api-endpoint";
    String ATTR_WEBDAV_SHARE = "webdav-endpoint";
    String ATTR_CONTENT_TYPE = "content-type";
    String ATTR_PAYLOAD = "payload";
    
    String ATTR_CONTEXT = "context";
    String ATTR_ID = "id";
    String ATTR_LIBRARY = "library_id";
    String ATTR_SLOT = "slot_id";
    String ATTR_SITE = "site_id";
    String ATTR_CONFIGURATION = "configuration_id";
    
    String ATTR_FOLDER = "folder-id";
    String ATTR_PATH = "path";
    String ATTR_SCOPE = "scope";
    String ATTR_TEMPLATE = "template";
    String ATTR_DESCRIPTION = "description";
    String ATTR_BODY = "c_body";
    String ATTR_BODY_TYPE = "_type";
    String ATTR_BODY_TYPE_TEXT = "markup_text";
    String ATTR_BODY_SOURCE = "source";
    String ATTR_SLOT_CONTENT = "slot_content";
    String ATTR_SLOT_CONTENT_TYPE = "type";
    String ATTR_SLOT_SCHEDULE = "schedule";
    String ATTR_SLOT_CUSTOMER_GROUPS = "customer_groups";
    String ATTR_SLOT_CALLOUT_MSG = "callout_msg";
    String ATTR_SLOT_RANK = "rank";
    
    String ATTR_SIZE = "size";
    String ATTR_DATA = "data";
    String ATTR_MIMETYPE = "mimetype";
    String ATTR_BASE64 = "base64";
    String ATTR_LOCALE = "locale";
    
    /**
     * special, Demandware specify page properties which need to be mapped
     */
    String PN_DWRE_SITE = "dwreSite";
    String PN_DWRE_LIBRARY = "dwreLibrary";
    String PN_DWRE_TEMPLATE_PATH = "dwreTemplatePath";
    String PN_DWRE_FOLDER = "dwreFolder";
    String PN_DWRE_SLOT_TYPE = "dwreSlotType";
    String PN_DWRE_SLOT_ID = "dwreSlotId";
    String PN_DWRE_SLOT_CONTENT_TYPE = "dwreSlotContentType";
    String PN_DWRE_SLOT_CATEGORY_ID = "dwreSlotCategoryId";
    String PN_DWRE_SLOT_FOLDER_ID = "dwreSlotFolderId";
    String PN_DWRE_INSTANCE_ID = "dwreInstanceId";
}
