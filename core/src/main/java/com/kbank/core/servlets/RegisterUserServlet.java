package com.kbank.core.servlets;

import com.google.gson.JsonObject;
import com.kbank.core.services.AEPUtilService;
import com.kbank.core.services.AIGeneratedPersonalizedDataService;
import com.kbank.core.services.GenericRestClient;
import com.kbank.core.services.ResourceResolverService;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.oak.spi.security.principal.PrincipalImpl;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "kbank/components/register",
        extensions = "json",
        methods = HttpConstants.METHOD_POST
)
@ServiceDescription("Servlet to register user")
public class RegisterUserServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserServlet.class);

    @Reference
    private ResourceResolverService resourceResolverService;

    @Reference
    private GenericRestClient genericRestClient;

    @Reference
    private AIGeneratedPersonalizedDataService aiGeneratedPersonalizedDataService;

    @Reference
    private AEPUtilService aepUtilService;

    private static final String USER_CREATION_PATH = "/home/users/kbank";


    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject jsonResponse = new JSONObject();

        String username = request.getParameter("j_email");
        String password = request.getParameter("j_password");
        String firstName = request.getParameter("j_firstname");
        String lastName = request.getParameter("j_lastname");
        String email = request.getParameter("j_email");
        String country = request.getParameter("j_country");
        String[] interests = request.getParameterValues("j_interests");
        String approverEmail = request.getParameter("j_rmemail");
        try (ResourceResolver resourceResolver = resourceResolverService.getResourceResolver()) {
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                jsonResponse.put("error", "Username and password must be provided");
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                out.print(jsonResponse);
                return;
            }

            if (resourceResolver != null) {
                UserManager userManager = resourceResolver.adaptTo(UserManager.class);
                if (userManager != null) {
                    Authorizable existingUser = userManager.getAuthorizable(username);

                    if (existingUser != null) {
                        jsonResponse.put("error", "User already exists");
                        response.setStatus(SlingHttpServletResponse.SC_CONFLICT);
                    } else {
                        User user = userManager.createUser(username, password, new PrincipalImpl(username), USER_CREATION_PATH);

                        // Set additional properties
                        Node userNode = resourceResolver.getResource(user.getPath()).adaptTo(Node.class);
                        Session session = resourceResolver.adaptTo(Session.class);
                        ValueFactory valueFactory = session.getValueFactory();
                        if (userNode != null) {
                            //userNode.addNode("profile");
                            if (firstName != null && !firstName.isEmpty()) {

                                Value firstNameValue = valueFactory.createValue(firstName, PropertyType.STRING);
                                user.setProperty("profile/givenName", firstNameValue);
                            }
                            if (lastName != null && !lastName.isEmpty()) {
                                Value lastNameValue = valueFactory.createValue(lastName, PropertyType.STRING);
                                user.setProperty("profile/familyName", lastNameValue);
                            }
                            if (email != null && !email.isEmpty()) {
                                Value emailValue = valueFactory.createValue(email, PropertyType.STRING);
                                user.setProperty("profile/email", emailValue);
                            }
                            if (interests != null && interests.length > 0) {
                                Value interestValue = valueFactory.createValue(String.join(", ", interests), PropertyType.STRING);
                                user.setProperty("profile/interests", interestValue);
                            }
                            if (country != null && !country.isEmpty()) {
                                Value countryValue = valueFactory.createValue(country, PropertyType.STRING);
                                user.setProperty("profile/country", countryValue);
                            }
                            if (approverEmail != null && !approverEmail.isEmpty()) {
                                Value approverEmailValue = valueFactory.createValue(approverEmail, PropertyType.STRING);
                                user.setProperty("profile/approver", approverEmailValue);
                            }

                            /*log.info("Generating personalized content");
                            JsonObject aiContent = aiGeneratedPersonalizedDataService.getPersonalizedAIGeneratedData(String.join(", ", interests), "data", null, null, request);
                            Value aiContentValue = valueFactory.createValue(aiContent.toString(), PropertyType.STRING);
                            user.setProperty("profile/aidata", aiContentValue);*/

                            // Add the user to the specified group
                            Group group = (Group) userManager.getAuthorizable("kbank-user-group");
                            if (group != null) {
                                group.addMember(user);
                                log.info("User {} added to group {}.", user.getID(), group.getID());
                            } else {
                                log.info("Failed to get group");
                                jsonResponse.put("error", "Failed to add user to group");
                            }
                            resourceResolver.commit();
                            sendRegisterDataToAEP(username, firstName, lastName, email, country, interests);
                            jsonResponse.put("message", "User registered successfully");
                            response.setStatus(SlingHttpServletResponse.SC_OK);
                        } else {
                            jsonResponse.put("error", "Failed to adapt user node");
                            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        }
                    }
                } else {
                    jsonResponse.put("error", "Failed to get UserManager");
                    response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                jsonResponse.put("error", "Failed to get ResourceResolver");
                response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (LoginException e) {
            log.error("LoginException occurred while getting ResourceResolver", e);
            try {
                jsonResponse.put("error", "Internal server error");
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            }
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (RepositoryException e) {
            log.error("RepositoryException occurred while setting user properties", e);
            try {
                jsonResponse.put("error", "Internal server error");
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            }
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Exception occurred while registering user", e);
            try {
                jsonResponse.put("error", "Internal server error");
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            }
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        //response.sendRedirect("/content/kbank/us/en.html");
        out.write(jsonResponse.toString());
    }

    private void sendRegisterDataToAEP(String username, String firstName, String lastName, String email, String country, String[] interests) {
        // Send user registration data to Adobe Experience Platform
        try {
            final String url = "https://dcs.adobedc.net/collection/2d40237daa1d3f1bd77fc6f23f5e406a137f003cf77d64ae8684dd07a99ec817";
            JsonObject aepResponse = genericRestClient.post(url, aepUtilService.createAEPRequestData(username, firstName, lastName, email, country, interests), JsonObject.class, aepUtilService.generateHeadersForAEP());
            log.info("User registration data sent to AEP: {}", aepResponse);
        } catch (Exception e) {
            log.error("Failed to send user registration data to AEP", e);
        }
    }
}
