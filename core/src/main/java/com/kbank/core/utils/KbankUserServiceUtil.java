package com.kbank.core.utils;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;

public interface KbankUserServiceUtil {

    public void setDataOnUserProfile(String userID, String updateNodeName, String data, ResourceResolver resourceResolver) throws PersistenceException, RepositoryException;
    public String getUserProfileDataById(String userId, String profileParameter, ResourceResolver resourceResolver) throws RepositoryException;
    public void deleteUserProfileDataById(String userId, String profileParameter, ResourceResolver resourceResolver) throws RepositoryException, PersistenceException;
}
