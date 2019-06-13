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

package com.adobe.cq.commerce.demandware.preview;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.engine.SlingRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.demandware.RenderService;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.wcm.api.WCMMode;

@Component(metatype = false)
@Service()
public class RenderServiceImpl implements RenderService {

    private static final Logger LOG = LoggerFactory.getLogger(RenderServiceImpl.class);

    @Reference
    private RequestResponseFactory requestResponseFactory;

    @Reference
    private SlingRequestProcessor requestProcessor;

    @Override
    public String render(final Resource resource, final String method, final String... selectors) {

        final String requestMethod = StringUtils.defaultString(method, "GET");
        final StringBuilder selectorBuilder = new StringBuilder();
        if (selectors != null) {
            for (int i = 0; i < selectors.length; i++) {
                selectorBuilder.append("." + selectors[i]);
            }
        }
        final String extension = selectorBuilder.toString() + ".html";

        final HttpServletRequest req = requestResponseFactory.createRequest(requestMethod, resource.getPath() +
                extension);
        WCMMode.DISABLED.toRequest(req);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse resp = requestResponseFactory.createResponse(out);

        try {
            requestProcessor.processRequest(req, resp, resource.getResourceResolver());
        } catch (ServletException e) {
            LOG.error("Error rendering resource " + resource.getPath(), e);
        } catch (IOException e) {
            LOG.error("Error rendering resource " + resource.getPath(), e);
        }
        return out.toString();
    }
}
