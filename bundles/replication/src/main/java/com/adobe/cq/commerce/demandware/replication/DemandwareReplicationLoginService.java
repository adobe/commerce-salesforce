/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe Systems Incorporated and others
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
package com.adobe.cq.commerce.demandware.replication;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Map;

import static java.util.Collections.singletonMap;

/**
 * <p>
 * Central service to provide login to JCR for all replication related
 * SFCC services.
 *
 * <p>
 * <b>Note</b>: if a method named create* is called the caller is responsible for
 * cleaning up resources. => close the resource resolver.
 * For example:
 *
 * <pre>{@code
 * try (final ResourceResolver resolver = replicationLogin.createResourceResolver) {
 *     ...
 * } catch(DemandwareReplicationException dre) {
 *     ...
 * }
 * }</pre>
 */
@Component(service = DemandwareReplicationLoginService.class)
@Designate(ocd = DemandwareReplicationLoginService.Configuration.class)
public class DemandwareReplicationLoginService {

    @Reference
    private ResourceResolverFactory rrf;

    private Configuration configuration;

    /**
     * Create a service resource resolver for replication services.
     *
     * Note: You are responsible to close this resolver.
     *
     * @return the service resource resolver
     * @throws DemandwareReplicationException if login fails
     */
    public ResourceResolver createResourceResolver() {
        try {
            final Map<String, Object> info = singletonMap(
                    ResourceResolverFactory.SUBSERVICE, configuration.subserviceId());
            return rrf.getServiceResourceResolver(info);
        } catch (final LoginException e) {
            throw new DemandwareReplicationException(
                    "Failed to obtain resource resolver for subservice ID '" + configuration.subserviceId() + "'", e);
        }
    }

    @Activate
    private void activate(final Configuration configuration) {
        this.configuration = configuration;
    }

    @ObjectClassDefinition(name = "Demandware replication resource resolver factory")
    protected @interface Configuration {

        @AttributeDefinition(
                name = "Subservice ID",
                description = "The subservice ID to use during login."
        )
        String subserviceId() default "replication";
    }
}
