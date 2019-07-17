package com.adobe.cq.commerce.demandware.replication.utils;

import com.day.cq.replication.AgentConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import java.util.Optional;

@Component
@Service(value = DemandwareInstanceIdProvider.class)
public class DemandwareInstanceIdProvider {
	private static final String DWRE_SCHEME = "demandware://";

	public String getInstanceId(final AgentConfig config) {
		return Optional.ofNullable(config.getTransportURI())
				.map(uri -> uri.replace(DWRE_SCHEME, StringUtils.EMPTY))
				.orElse(StringUtils.EMPTY);
	}
}
