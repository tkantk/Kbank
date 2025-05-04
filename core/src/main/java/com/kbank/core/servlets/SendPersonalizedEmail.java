package com.kbank.core.servlets;

import com.google.gson.JsonObject;
import com.kbank.core.schedulers.NewsLetterGeneratorScheduledTask;
import com.kbank.core.services.AEPUtilService;
import com.kbank.core.services.AIGeneratedPersonalizedDataService;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "kbank/components/sendPersonalizedEmail",
        extensions = "json",
        methods = HttpConstants.METHOD_GET
)
public class SendPersonalizedEmail extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(SendPersonalizedEmail.class);



    @Reference
    private AIGeneratedPersonalizedDataService aiGeneratedPersonalizedDataService;

    @Reference
    private NewsLetterGeneratorScheduledTask newsLetterGeneratorScheduledTask;

    @Reference
    private AEPUtilService aepUtilService;


    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();
        String type = request.getParameter("type");

        try {
            // Get the ResourceResolver
            ResourceResolver resourceResolver = request.getResourceResolver();

            // Get the Session
            Session session = resourceResolver.adaptTo(Session.class);

            // Get the UserManager
            UserManager userManager = resourceResolver.adaptTo(UserManager.class);

            // Get the Authorizable object for the current user
            assert userManager != null;
            assert session != null;
            Authorizable authorizable = userManager.getAuthorizable(session.getUserID());

            // Get the user's profile
            assert authorizable != null;
            Node profileNode = (Node) session.getItem(authorizable.getPath() + "/profile");
            if ("bulkEmail".equalsIgnoreCase(type)) {
                // Send the bulk email
                log.info("Sending bulk email");
                newsLetterGeneratorScheduledTask.runOnDemand(request);
                jsonResponse.addProperty("status", "success");
                jsonResponse.addProperty("message", "Bulk Email Sent Successfully");
            } else if (profileNode.hasProperty("interests")) {
                String interests = aepUtilService.getInterestsFromAEP(profileNode.getProperty("email").getString());
                String name = profileNode.getProperty("givenName").getString() + " " + profileNode.getProperty("familyName").getString();

                if ("email".equalsIgnoreCase(type)) {
                    // Send the personalized email
                    log.info("Sending personalized email");
                    jsonResponse = aiGeneratedPersonalizedDataService.getPersonalizedAIGeneratedData(interests, type, profileNode.getProperty("email").getString(), name, request);
                    jsonResponse.addProperty("status", "success");
                    jsonResponse.addProperty("message", "Personalized Email Sent Successfully");
                } else if ("articlelist".equalsIgnoreCase(type)) {
                    // Send the personalized notification
                    log.info("Sending personalized notification");
                    jsonResponse = aiGeneratedPersonalizedDataService.getPersonalizedAIGeneratedData(interests, type, null, null, request);
                    jsonResponse.addProperty("status", "success");
                    jsonResponse.addProperty("message", "Approval Email Sent Successfully");
                } else if ("approval".equalsIgnoreCase(type)) {
                    // Send the personalized email
                    log.info("Sending personalized email");
                    jsonResponse = aiGeneratedPersonalizedDataService.getPersonalizedAIGeneratedData(interests, type, profileNode.getProperty("email").getString(), name, request);
                    jsonResponse.addProperty("status", "success");
                    jsonResponse.addProperty("message", "Approval Email Sent Successfully");
                } else {
                    log.error("Invalid type parameter");
                    jsonResponse.addProperty("status", "error");
                    jsonResponse.addProperty("message", "Invalid type parameter");
                }
            } else {
                log.error("Invalid type parameter");
                jsonResponse.addProperty("status", "error");
                jsonResponse.addProperty("message", "Invalid type parameter");
            }

        } catch (RepositoryException | URISyntaxException | InterruptedException | LoginException |
                 IllegalBlockSizeException | NoSuchPaddingException | BadPaddingException | NoSuchAlgorithmException |
                 InvalidKeyException e) {
            log.error("Error getting user profile details", e);
            jsonResponse.addProperty("status", "error");
            jsonResponse.addProperty("message", "Error sending personalized email");
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }

        out.print(jsonResponse);
    }

}
