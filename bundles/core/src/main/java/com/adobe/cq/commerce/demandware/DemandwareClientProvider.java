package com.adobe.cq.commerce.demandware;

import com.day.cq.replication.AgentConfig;

public interface DemandwareClientProvider {
	DemandwareClient getAnyClient();
	DemandwareClient getClientForSpecificInstance(AgentConfig config);
}
