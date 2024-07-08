package com.kbank.core.services.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kbank.core.services.*;
import com.kbank.core.utils.KbankEncryptData;
import com.kbank.core.utils.KbankUserServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component(service = AIGeneratedPersonalizedDataService.class, immediate = true)
@Designate(ocd = AIGeneratedPersonalizedDataServiceImpl.Config.class)
public class AIGeneratedPersonalizedDataServiceImpl implements AIGeneratedPersonalizedDataService {

    private static Logger log = LoggerFactory.getLogger(AIGeneratedPersonalizedDataServiceImpl.class);

    @ObjectClassDefinition(name = "AI Generated Personalized Data Service", description = "AI Generated Personalized Data Service")
    public @interface Config {
        String approverCampaignID() default "c891c9c1-9955-49eb-bae8-daa96a6cb2c3";
        String articleCampaignID() default "14ffaa18-3c4a-4f71-bd1c-98d14c638e5a";
    }

    @Reference
    private EmailService emailService;

    @Reference
    private GenericRestClient genericRestClient;

    @Reference
    private ResourceResolverService resourceResolverService;

    @Reference
    private KbankEncryptData kbankEncryptData;

    @Reference
    private KbankUserServiceUtil KbankUserServiceUtil;

    @Reference
    private AJOUtilService ajoUtilService;

    @Reference
    private FireFlyService fireFlyService;

    private String approverCampaignID;
    private String articleCampaignID;

    private static final String CONTENT_TEMPLATE_PATH = "/apps/kbank/components/email_templates/news-letter.html";
    private static final String APPROVAL_TEMPLATE_PATH = "/apps/kbank/components/email_templates/approval-template.html";

    private static String endpoint = "https://dvvmj3dkzh.execute-api.us-east-2.amazonaws.com/prod/searcharticles";

    @Activate
    protected void activate(Config config) {
        this.approverCampaignID = config.approverCampaignID();
        this.articleCampaignID = config.articleCampaignID();
    }

    @Override
    public JsonObject getPersonalizedAIGeneratedData(String interests, String type, String emailID, String name, SlingHttpServletRequest request) throws LoginException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, RepositoryException, URISyntaxException, InterruptedException {

        JsonObject response = new JsonObject();

       JsonObject requestData = new JsonObject();
        requestData.addProperty("topics", interests);
        requestData.addProperty("numArticles", 30);
        JsonArray aiResponse;
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String fireFlyToken = fireFlyService.getFireFlyToken();
        try {
            aiResponse = genericRestClient.post(endpoint, requestData, JsonArray.class, headers);
        } catch (Exception e) {
            response.addProperty("error", "Error in AI response");
            return response;
        }
        JsonArray filteredResponse = getFilteredResponse(aiResponse, fireFlyToken);
        if ("email".equalsIgnoreCase(type)) {
            JsonObject placeholders = new JsonObject();

            LocalDate currentDate = LocalDate.now();
            // Define the desired format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            placeholders.addProperty("currentDate", currentDate.format(formatter));
            for (int i = 0; i < filteredResponse.size(); i++) {
                JsonObject article = filteredResponse.get(i).getAsJsonObject();
                if (i<=5) {
                    placeholders.addProperty("image" + i, article.get("urlToImage").getAsString());
                    placeholders.addProperty("headlineTitle" + i, article.get("heading").getAsString());
                    placeholders.addProperty("headlineLink" + i, article.get("url").getAsString());
                    if (article.get("article").getAsString().length() > 50) {
                        placeholders.addProperty("headlineDescription" + i, article.get("article").getAsString().substring(0, 50) + "...");
                    }
                } else {
                    placeholders.addProperty("image" + i, article.get("urlToImage").getAsString());
                    placeholders.addProperty("viewTitle" + i, article.get("heading").getAsString());
                    placeholders.addProperty("viewLink" + i, article.get("url").getAsString());
                    if (article.get("article").getAsString().length() > 50) {
                        placeholders.addProperty("viewDescription" + i, article.get("article").getAsString().substring(0, 50) + "...");
                    }
                }
            }
            placeholders.addProperty("name", name);
            //emailService.sendEmail(emailID, "Market Watch: Momentum missing ahead of expiry", placeholders, CONTENT_TEMPLATE_PATH);
            JsonObject articleRequest = ajoUtilService.createAJOTransactionalMessageRequest(emailID, articleCampaignID, placeholders);
            ajoUtilService.sendTransactionEmail(articleRequest);
            response.addProperty("success", "Successfully sent email");
        } else if ("approval".equalsIgnoreCase(type)) {
            try (ResourceResolver resourceResolver = resourceResolverService.getResourceResolver()) {
                String url = getCurrentDomain(request) + "/content/kbank/us/en/email-approval-page."+kbankEncryptData.encryptData(emailID)+".html";
                JsonObject context = new JsonObject();
                context.addProperty("url", url);
                String approverEmail = KbankUserServiceUtil.getUserProfileDataById(emailID,"approver",resourceResolver);
                KbankUserServiceUtil.setDataOnUserProfile(emailID, "aidata", new Gson().toJson(filteredResponse), resourceResolver);
                //emailService.sendEmail(approverEmail, "Approval Required", placeholders, APPROVAL_TEMPLATE_PATH);
                JsonObject approverRequest = ajoUtilService.createAJOTransactionalMessageRequest(approverEmail, approverCampaignID, context);
                ajoUtilService.sendTransactionEmail(approverRequest);
            }
            response.add("data", filteredResponse);
            response.addProperty("success", "Successfully fetched data");
        }  else {
            response.add("data", filteredResponse);
        }
        return response;
    }

    private JsonArray getFilteredResponse(JsonArray aiResponse, String token) {
        JsonArray filteredResponse = new JsonArray();
        List<String> responseIds = new ArrayList<>();
        JsonArray repeatedArticles = new JsonArray();
        for (int i = 0; i < aiResponse.size(); i++) {
            JsonObject article = aiResponse.get(i).getAsJsonObject();
            if (responseIds.contains(article.get("heading").getAsString())) {
                continue;
            } else if (filteredResponse.size() == 9) {
                break;
            } else {
                filteredResponse.add(article);
                responseIds.add(article.get("heading").getAsString());
                try {
                    String imagePath = fireFlyService.getFireFlyImage(token, article.get("heading").getAsString(), 1);
                    if (!StringUtils.isEmpty(imagePath)) {
                        article.addProperty("urlToImage", imagePath);
                    }
                    repeatedArticles.add(article);
                } catch (IOException | URISyntaxException | InterruptedException e) {
                    log.error("Error in getting image from FireFly", e);
                }
                repeatedArticles.add(article);
            }
        }
        if (filteredResponse.size() < 9) {
            for (int i = 0; i < repeatedArticles.size(); i++) {
                JsonObject article = repeatedArticles.get(i).getAsJsonObject();
                if (filteredResponse.size() == 9) {
                    break;
                } else {
                    filteredResponse.add(article);
                }
            }
        }
        return filteredResponse;
    }

    private String getCurrentDomain(SlingHttpServletRequest request) {
        String scheme = request.getScheme(); // http or https
        String serverName = request.getServerName(); // example.com
        int serverPort = request.getServerPort(); // 80, 443, etc.

        // Construct the full domain URL
        StringBuilder domain = new StringBuilder();
        domain.append(scheme).append("://").append(serverName);

        // Append the port number if it's not the default port for the scheme
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            domain.append(":").append(serverPort);
        }

        return domain.toString();
    }
}
