package com.kbank.core.models.impl;

import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.google.gson.Gson;
import com.kbank.core.models.DataLayer;
import com.kbank.core.services.ResourceResolverService;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {DataLayer.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class DataLayerImpl implements DataLayer {

    @Inject
    @Optional
    private String pageName;

    @Inject @Named("jcr:title")
    private String title;

    @Inject @Source("script-bindings")
    private Page currentPage;

    @Inject @Named("cq:tags")
    private String[] pageType;

    @Inject @Named("jcr:language")
    private String language;

    @Inject @Optional
    private String primaryCategory;

    @Inject @Optional
    private String subCategory;

    @Inject
    private SlingHttpServletRequest request;

    @Inject @Source("osgi-services")
    private ResourceResolverService resourceResolverService;

    // This is a placeholder for the actual data that will be returned by the implementation
    private Map<String, Object> digitalData;

    private List<String> tagTitles;

    private static final String LANGUAGE_PROPERTY = "jcr:language";

    private static final String CONTENT_PARENT_PATH = "/content/kbank/us/en/personal";

    @PostConstruct
    protected void init() throws LoginException {
        digitalData = new HashMap<>();

        Map<String, Object> cart = new HashMap<>();
        cart.put("productsInCart", 0);

        Map<String, Object> page = new HashMap<>();

        Map<String, Object> attributes = new HashMap<>();
        page.put("attributes", attributes);

        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("pageName", currentPage.getName());
        pageInfo.put("internalPageName", currentPage.getName());
        pageInfo.put("title", title);
        pageInfo.put("destinationURL", constructFullUrl());
        pageInfo.put("hierarchie1", extractHierarchyFromUrl(currentPage.getPath()));
        page.put("pageInfo", pageInfo);

        Map<String, Object> category = new HashMap<>();
        if (pageType != null && pageType.length > 0) {
            ResourceResolver resourceResolver = resourceResolverService.getResourceResolver();
            TagManager tagManager = resourceResolver.adaptTo(TagManager.class);
            tagTitles = new ArrayList<>();
            for (String tagId : pageType) {
                Tag tag = tagManager.resolve(tagId);
                if (tag != null) {
                    tagTitles.add(tag.getTitle());
                }
            }
            category.put("type", String.join(", ", tagTitles));
        }

        category.put("version", "1.0");
        category.put("subCategory", subCategory);
        page.put("category", category);

        page.put("components", new ArrayList<>());

        Map<String, Object> currentUserProfile = getCurrentUserProfile();
        ArrayList<Map<String, Object>> userArray = new ArrayList<>();
        Map<String, Object> user = new HashMap<>();
        ArrayList<Map<String, Object>> profileArray = new ArrayList<>();
        Map<String, Object> profile = new HashMap<>();
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("userId", currentUserProfile.get("userId"));
        userAttributes.put("username", currentUserProfile.get("userId"));
        userAttributes.put("loggedIn", currentUserProfile.get("loggedIn"));
        profile.put("attributes", userAttributes);
        Map<String, Object> profileInfo = new HashMap<>();
        if (tagTitles != null && !tagTitles.isEmpty()) {
            profileInfo.put("interests", String.join(", ", tagTitles));
        }
        profile.put("profileInfo", profileInfo);
        profileArray.add(profile);
        user.put("profile", profileArray);
        userArray.add(user);

        digitalData.put("cart", cart);
        digitalData.put("language", language == null ? getLanguageFromFallbackPath() : language);
        digitalData.put("page", page);
        digitalData.put("user", userArray);
    }

    public String getData() {
        // Convert map to JSON
        Gson gson = new Gson();
        final String digitalDataJson = gson.toJson(digitalData);
        return digitalDataJson;
    }

    private String constructFullUrl() {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        // Only append the port if it's not the default port for the scheme
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }

        url.append(requestUri);

        if (queryString != null) {
            url.append("?").append(queryString);
        }

        return url.toString();
    }

    private String extractHierarchyFromUrl(String requestUri) {
        if (requestUri.startsWith("/content/")) {
            String[] parts = requestUri.split("/");
            if (parts.length > 2) {
                String[] hierarchyParts = new String[parts.length - 2];
                System.arraycopy(parts, 2, hierarchyParts, 0, parts.length - 2);
                return String.join(":", hierarchyParts);
            }
        }
        return "";
    }

    private Map<String, Object> getCurrentUserProfile() {
        Map<String, Object> userProfile = new HashMap<>();
        try(ResourceResolver resourceResolver = resourceResolverService.getResourceResolver()) {
            Session session = resourceResolver.adaptTo(Session.class);
            UserManager userManager = resourceResolver.adaptTo(UserManager.class);
            Session currentUserSession = request.getResourceResolver().adaptTo(Session.class);

            if (session != null) {
                User currentUser = (User) userManager.getAuthorizable(currentUserSession.getUserID());
                if (currentUser != null && !"anonymous".equals(currentUser.getID()) && session.getNode(currentUser.getPath()).hasNode("profile") ) {
                    String userId = currentUser.getID();
                    String interets = currentUser.getProperty("profile/interests")[0].getString();
                    Boolean loggedIn = Boolean.TRUE;
                    userProfile.put("userId", userId);
                    userProfile.put("interests", interets);
                    userProfile.put("loggedIn", loggedIn);
                } else {
                    String userId = "";
                    Boolean loggedIn = Boolean.FALSE;
                    userProfile.put("userId", userId);
                    userProfile.put("loggedIn", loggedIn);
                }
            }
        } catch (Exception e) {
            // Handle exception
        }
        return userProfile;
    }

    private String getLanguageFromFallbackPath() {
        try (ResourceResolver resourceResolver = resourceResolverService.getResourceResolver()) {
            Resource fallbackResource = resourceResolver.getResource(CONTENT_PARENT_PATH);
            if (fallbackResource != null) {
                Node fallbackNode = fallbackResource.adaptTo(Node.class);
                if (fallbackNode != null && fallbackNode.hasProperty(LANGUAGE_PROPERTY)) {
                    return fallbackNode.getProperty(LANGUAGE_PROPERTY).getString();
                }
            }
        } catch (RepositoryException | LoginException e) {
            e.printStackTrace();
        }

        return null; // Or return a default language if preferred
    }

}
