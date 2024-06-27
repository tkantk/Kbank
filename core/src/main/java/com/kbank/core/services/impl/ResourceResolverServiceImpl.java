package com.kbank.core.services.impl;

import com.kbank.core.services.ResourceResolverService;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(service = ResourceResolverService.class, immediate = true)
public class ResourceResolverServiceImpl implements ResourceResolverService {

    private static final Logger log = LoggerFactory.getLogger(ResourceResolverServiceImpl.class);
 
        private static final String SYSTEM_USER = "kbank-service-user";
 
    @Reference
    ResourceResolverFactory resourceResolverFactory;
 
    @Override
    public ResourceResolver getResourceResolver() throws LoginException {
 
        Map<String, Object> param = new HashMap<>();
        param.put(ResourceResolverFactory.SUBSERVICE, SYSTEM_USER);
        ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(param);
        log.info("resourceResolver.getUserID(): {}", resourceResolver.getUserID());
        return resourceResolver;
    }
}
