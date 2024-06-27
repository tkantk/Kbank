package com.kbank.core.utils.impl;

import com.kbank.core.utils.KbankEncryptData;
import com.kbank.core.utils.KbankUserServiceUtil;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.*;

@Component(service = KbankUserServiceUtil.class, immediate = true)
public class KbankUserServiceUtilImpl implements KbankUserServiceUtil {

    private static final String USER_PATH = "/home/users/kbank";
    private static final String PROFILE_NODE = "profile";

    @Override
    public void setDataOnUserProfile(String userID, String updateNodeName, String data, ResourceResolver resourceResolver) throws PersistenceException, RepositoryException {
        Resource userResource = getUserResourceById(userID, resourceResolver);
        if (userResource != null) {
            Resource profileResource = userResource.getChild(PROFILE_NODE);
            if (profileResource != null) {
                ModifiableValueMap profileProperties = profileResource.adaptTo(ModifiableValueMap.class);
                assert profileProperties != null;
                profileProperties.put(updateNodeName, data);
                resourceResolver.commit();
            }
        }
    }

    /**
     * Get the user profile data by user ID (rep:authorizableId)
     *
     * @param userId The user ID
     * @return The user profile data
     */
    public String getUserProfileDataById(String userId, String profileParameter, ResourceResolver resourceResolver) throws RepositoryException {
        Resource userResource = getUserResourceById(userId, resourceResolver);
        if (userResource != null) {
            Resource profileResource = userResource.getChild(PROFILE_NODE);
            if (profileResource != null) {
                return profileResource.getValueMap().get(profileParameter, String.class);
            }
        }
        return null;
    }

    @Override
    public void deleteUserProfileDataById(String userId, String profileParameter, ResourceResolver resourceResolver) throws RepositoryException, PersistenceException {
        Resource userResource = getUserResourceById(userId, resourceResolver);
        if (userResource != null) {
            Resource profileResource = userResource.getChild(PROFILE_NODE);
            if (profileResource != null) {
                ModifiableValueMap profileProperties = profileResource.adaptTo(ModifiableValueMap.class);
                assert profileProperties != null;
                profileProperties.remove(profileParameter);
                resourceResolver.commit();
            }
        }
    }

    /**
     * Get the user resource by user ID (rep:authorizableId)
     *
     * @param userId The user ID
     * @return The user resource
     */
    public Resource getUserResourceById(String userId, ResourceResolver resourceResolver) throws RepositoryException {
        Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            return null;
        }

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        String queryString = "SELECT * FROM [rep:User] AS u WHERE ISDESCENDANTNODE(u, '/home/users') AND u.[profile/email] = '" + userId + "'";
        Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);

        QueryResult result = query.execute();
        RowIterator rowIterator = result.getRows();

        if (rowIterator.hasNext()) {
            Row row = rowIterator.nextRow();
            String path = row.getPath();
            return resourceResolver.getResource(path);
        }
        return null;
    }
}
