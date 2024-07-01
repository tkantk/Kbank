package com.kbank.core.models.impl;

import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kbank.core.models.DataLayer;
import com.kbank.core.services.AEPUtilService;
import com.kbank.core.services.GenericRestClient;
import com.kbank.core.services.ResourceResolverService;
import com.kbank.core.servlets.RegisterUserServlet;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.Cookie;
import java.util.*;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {DataLayer.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class DataLayerImpl implements DataLayer {

    private static final Logger log = LoggerFactory.getLogger(DataLayer.class);

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

    @Inject
    private SlingHttpServletResponse response;

    @Inject @Source("osgi-services")
    private ResourceResolverService resourceResolverService;

    @Inject @Source("osgi-services")
    private AEPUtilService aepUtilService;

    @Inject @Source("osgi-services")
    private GenericRestClient genericRestClient;

    // This is a placeholder for the actual data that will be returned by the implementation
    private Map<String, Object> digitalData;

    private List<String> tagTitles;

    private static final String LANGUAGE_PROPERTY = "jcr:language";

    private static final String CONTENT_PARENT_PATH = "/content/kbank/us/en/personal";

    private static final String COOKIE_NAME = "interests";
    private static final String DELIMITER = "|";

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
        try {
            if (StringUtils.equalsIgnoreCase(currentUserProfile.get("loggedIn").toString(),"true") &&
                    tagTitles != null && !tagTitles.isEmpty() && !StringUtils.equalsIgnoreCase("Kbank HomePage", tagTitles.get(0))) {
                String currentInterests = handleInterestsCookie(String.join(", ", tagTitles));
                sendRegisterDataToAEP(currentUserProfile.get("userId").toString(), currentUserProfile.get("firstName").toString(),
                        currentUserProfile.get("lastName").toString(), currentUserProfile.get("userId").toString(),
                        currentUserProfile.get("country").toString(), currentInterests.split("\\|"));
            }
        } catch (Exception e) {
            log.error("Failed to send user registration data to AEP {}", e.getMessage());
        }

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
                    userProfile.put("firstName", currentUser.getProperty("profile/givenName")[0].getString());
                    userProfile.put("lastName", currentUser.getProperty("profile/familyName")[0].getString());
                    userProfile.put("country", currentUser.getProperty("profile/country")[0].getString());
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
            log.error("Failed to get language from fallback path {}", e.getMessage());
        }

        return null; // Or return a default language if preferred
    }

    private void sendRegisterDataToAEP(String username, String firstName, String lastName, String email, String country, String[] interests) {
        // Send user registration data to Adobe Experience Platform
        try {
            final String url = "https://dcs.adobedc.net/collection/2d40237daa1d3f1bd77fc6f23f5e406a137f003cf77d64ae8684dd07a99ec817";
            assert genericRestClient != null;
            assert aepUtilService != null;
            JsonObject aepResponse = genericRestClient.post(url, aepUtilService.createAEPRequestData(username, firstName, lastName, email, country, interests), JsonObject.class, aepUtilService.generateHeadersForAEP());
            log.info("User registration data sent to AEP: {}", aepResponse);
        } catch (Exception e) {
            log.error("Failed to send user registration data to AEP", e);
        }
    }

    public String handleInterestsCookie(String newInterest) {
        Cookie interestsCookie = getInterestsCookie();
        String updatedInterests = newInterest;
        if (interestsCookie != null) {
            // Cookie exists, add new interest
            String currentInterests = decodeValue(interestsCookie.getValue());
            Set<String> interestsSet = new HashSet<>(Arrays.asList(currentInterests.split("\\" + DELIMITER)));
            interestsSet.add(newInterest);
            updatedInterests = String.join(DELIMITER, interestsSet);
            updateCookie(updatedInterests);
        } else {
            // Cookie doesn't exist, create it
            createCookie(newInterest);
        }
        return updatedInterests;
    }

    private Cookie getInterestsCookie() {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void updateCookie(String updatedInterests) {
        String encodedValue = encodeValue(updatedInterests);
        Cookie updatedCookie = new Cookie(COOKIE_NAME, encodedValue);
        updatedCookie.setMaxAge(-1); // Set as session cookie
        updatedCookie.setPath("/");
        response.addCookie(updatedCookie);
    }

    private void createCookie(String initialInterest) {
        String encodedValue = encodeValue(initialInterest);
        Cookie newCookie = new Cookie(COOKIE_NAME, encodedValue);
        newCookie.setMaxAge(-1); // Set as session cookie
        newCookie.setPath("/");
        response.addCookie(newCookie);
    }

    private String encodeValue(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    private String decodeValue(String value) {
        try {
            return new String(Base64.getDecoder().decode(value));
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

}
