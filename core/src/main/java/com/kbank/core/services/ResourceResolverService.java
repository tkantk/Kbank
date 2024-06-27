package com.kbank.core.services;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

public interface ResourceResolverService {

    public ResourceResolver getResourceResolver() throws LoginException;

}
