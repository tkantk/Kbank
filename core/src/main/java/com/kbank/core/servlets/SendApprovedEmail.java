package com.kbank.core.servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kbank.core.schedulers.NewsLetterGeneratorScheduledTask;
import com.kbank.core.services.AEPUtilService;
import com.kbank.core.services.AIGeneratedPersonalizedDataService;
import com.kbank.core.services.EmailService;
import com.kbank.core.services.ResourceResolverService;
import com.kbank.core.utils.KbankUserServiceUtil;
import org.apache.commons.lang3.StringUtils;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "kbank/components/sendApprovedEmail",
        extensions = "json",
        methods = HttpConstants.METHOD_GET
)
public class SendApprovedEmail extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(SendApprovedEmail.class);

    @Reference
    private KbankUserServiceUtil kbankUserServiceUtil;

    @Reference
    private EmailService emailService;

    @Reference
    private ResourceResolverService resourceResolverService;

    private static final String CONTENT_TEMPLATE_PATH = "/apps/kbank/components/email_templates/news-letter.html";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();
        String action = request.getParameter("type");
        String emailID = request.getParameter("emailId");

        try (ResourceResolver resourceResolver = resourceResolverService.getResourceResolver()) {
            if ("approve".equalsIgnoreCase(action)) {
                // Send the bulk email
                log.info("Sending approved email");
                String aiData = kbankUserServiceUtil.getUserProfileDataById(emailID, "aidata", resourceResolver);
                if (aiData == null) {
                    log.error("AI data not found for user with email ID: {}", emailID);
                    jsonResponse.addProperty("status", "error");
                    jsonResponse.addProperty("message", "AI data not found for user with email ID: " + emailID);
                    out.print(jsonResponse.toString());
                    return;
                } else {
                    log.info("AI data found for user with email ID: {}", emailID);
                    createAndSendEmail(aiData, emailID, resourceResolver);
                    jsonResponse.addProperty("status", "success");
                    jsonResponse.addProperty("message", "Personalized Email Sent Successfully");
                }
            } else if ("decline".equalsIgnoreCase(action)) {
                log.info("Declining generated email data for: {}", emailID);
                kbankUserServiceUtil.deleteUserProfileDataById(emailID, "aidata", resourceResolver);
                jsonResponse.addProperty("status", "success");
                jsonResponse.addProperty("message", "Declined the Personalized Email Content");
            } else {
                log.error("Invalid type parameter");
                jsonResponse.addProperty("status", "error");
                jsonResponse.addProperty("message", "Invalid action parameter");
            }

        } catch (RepositoryException | LoginException e) {
            log.error("Error getting user profile details", e);
            jsonResponse.addProperty("status", "error");
            jsonResponse.addProperty("message", "Error sending personalized email");
        }

        out.print(jsonResponse.toString());
    }

    private void createAndSendEmail(String aiData, String emailID, ResourceResolver resourceResolver) {
        Map<String, String> placeholders = new HashMap<>();
        JsonArray filteredResponse = new JsonArray();
        // Create a JsonParser instance
        JsonParser jsonParser = new JsonParser();
        // Parse the string to a JsonElement
        JsonElement jsonElement = jsonParser.parse(aiData);

        // Check if the parsed element is indeed a JsonArray
        if (jsonElement.isJsonArray()) {
            // Convert to JsonArray and return
            filteredResponse = jsonElement.getAsJsonArray();
        }

        LocalDate currentDate = LocalDate.now();
        // Define the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        placeholders.put("currentDate", currentDate.format(formatter));
        for (int i = 0; i < filteredResponse.size(); i++) {
            JsonObject article = filteredResponse.get(i).getAsJsonObject();
            if (i<=5) {
                placeholders.put("image" + i, article.get("urlToImage").getAsString());
                placeholders.put("headlineTitle" + i, article.get("heading").getAsString());
                placeholders.put("headlineLink" + i, article.get("url").getAsString());
                if (article.get("article").getAsString().length() > 50) {
                    placeholders.put("headlineDescription" + i, article.get("article").getAsString().substring(0, 50) + "...");
                }
            } else {
                placeholders.put("image" + i, article.get("urlToImage").getAsString());
                placeholders.put("viewTitle" + i, article.get("heading").getAsString());
                placeholders.put("viewLink" + i, article.get("url").getAsString());
                if (article.get("article").getAsString().length() > 50) {
                    placeholders.put("viewDescription" + i, article.get("article").getAsString().substring(0, 50) + "...");
                }
            }
        }
        placeholders.put("name", getName(emailID));
        emailService.sendEmail(emailID, "Market Watch: Momentum missing ahead of expiry", placeholders, CONTENT_TEMPLATE_PATH);
    }

    public String getName(String email) {
        String fullName = StringUtils.EMPTY;
        try(ResourceResolver resourceResolver = resourceResolverService.getResourceResolver()) {
            String firstName = kbankUserServiceUtil.getUserProfileDataById(email, "givenName", resourceResolver);
            String lastName = kbankUserServiceUtil.getUserProfileDataById(email, "familyName", resourceResolver);
            fullName = firstName + " " + lastName;
        } catch (LoginException | RepositoryException e) {
            throw new RuntimeException(e);
        }
        return fullName;
    }

}
