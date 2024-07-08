package com.kbank.core.services.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kbank.core.services.AEPUtilService;
import com.kbank.core.services.AJOUtilService;
import com.kbank.core.services.GenericRestClient;
import com.kbank.core.utils.JSONUtil;
import com.kbank.core.utils.impl.JSONUtilImpl;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component(service = AJOUtilService.class, immediate = true)
@Designate(ocd = AJOUtilServiceImpl.Config.class)
public class AJOUtilServiceImpl implements AJOUtilService {

    @Reference
    private GenericRestClient genericRestClient;

    private JSONUtil jsonUtil;

    private String ajoTransactionCampaignBaseURL;

    @ObjectClassDefinition(name = "AJO Transaction Campaign Service", description = "AJO Transaction Campaign Service")
    public @interface Config {
        String ajoTransactionCampaignBaseURL() default "https://platform.adobe.io/ajo/im/executions/unitary";
    }

    @Activate
    protected void activate(final Config config) {
        this.ajoTransactionCampaignBaseURL = config.ajoTransactionCampaignBaseURL();
    }

    @Reference
    private AEPUtilService aepUtilService;

    @Override
    public JsonObject sendTransactionEmail(JsonObject request) throws IOException, URISyntaxException, InterruptedException {
        final String token = aepUtilService.getLoginTokenForIO();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("x-api-key", aepUtilService.getAEPAPIKey());
        headers.put("x-gw-ims-org-id", aepUtilService.getAEPORGID());
        headers.put("x-sandbox-name", aepUtilService.getAEPSandboxName());
        headers.put("Content-Type", "application/json");

        return genericRestClient.post(ajoTransactionCampaignBaseURL, request, JsonObject.class, headers);
    }

    @Override
    public JsonObject createAJOTransactionalMessageRequest(String emailID, String campaignID, JsonObject contextData) {
        JsonObject request = new JsonObject();
        request.addProperty("requestId", UUID.randomUUID().toString());
        request.addProperty("campaignId", campaignID);
        JsonArray recipients = new JsonArray();
        JsonObject recipient = new JsonObject();
        recipient.addProperty("type", "aep");
        recipient.addProperty("userId", emailID);
        recipient.addProperty("namespace", "Email");
        JsonObject channelData = new JsonObject();
        channelData.addProperty("emailAddress", emailID);
        recipient.add("channelData", channelData);
        recipient.add("context", contextData);
        recipients.add(recipient);
        request.add("recipients", recipients);
        return request;
    }
}
