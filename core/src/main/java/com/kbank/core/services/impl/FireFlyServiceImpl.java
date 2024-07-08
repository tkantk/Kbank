package com.kbank.core.services.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kbank.core.services.FireFlyService;
import com.kbank.core.services.GenericRestClient;
import com.kbank.core.utils.JSONUtil;
import com.kbank.core.utils.impl.JSONUtilImpl;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

@Component(service = FireFlyService.class, immediate = true)
public class FireFlyServiceImpl implements FireFlyService {

    private static Logger log = LoggerFactory.getLogger(FireFlyServiceImpl.class);

    private static String CLIENT_ID = "c1618baee6eb4cbe888c0e8f01570f72";
    private static String CLIENT_VALUE = "p8e-5nadX6dgrbXWf-CxTcTPJxDdTXV7L0m3";
    private static String GRANT_TYPE = "client_credentials";
    private static String SCOPE = "openid%2CAdobeID,session,additional_info,read_organizations,firefly_api,ff_apis";
    private static String TOKEN_URL = "https://ims-na1.adobelogin.com/ims/token/v3";
    private static String IMAGE_GENERATE_URL = "https://firefly-api.adobe.io/v2/images/generate";

    @Reference
    private GenericRestClient genericRestClient;

    private JSONUtil jsonUtil;


    @Override
    public String getFireFlyToken() throws IOException, URISyntaxException, InterruptedException {
        Map<String, String> headers = new HashMap<>();

        Map<String, String> data = new HashMap<>();
        data.put("client_id",CLIENT_ID);
        data.put("client_secret",CLIENT_VALUE);
        data.put("scope",SCOPE);
        data.put("grant_type","GRANT_TYPE");
        String finalURL = TOKEN_URL + "?client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_VALUE + "&scope=" + SCOPE + "&grant_type=" + GRANT_TYPE;

        JsonObject response = genericRestClient.post(finalURL, StringUtils.EMPTY, JsonObject.class, headers);
        return response.get("access_token").getAsString();
    }

    @Override
    public String getFireFlyImage(String token, String prompt, int count) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + token);
            headers.put("X-Api-Key", CLIENT_ID);
            headers.put("Content-Type", "application/json");
            String jsonPayload = createJsonPayload(prompt, count);
            JsonObject response = genericRestClient.post(IMAGE_GENERATE_URL, jsonPayload, JsonObject.class, headers);
            JsonObject outputObject = response.getAsJsonArray("outputs").get(0).getAsJsonObject();
            if (outputObject != null) {
                JsonObject imageObject = outputObject.get("image").getAsJsonObject();
                if (imageObject != null) {
                    return imageObject.get("presignedUrl").getAsString();
                }
            }
        } catch (Exception e) {
            log.error("Error in getting image from FireFly", e);
        }
        return "";
    }

    private static String createJsonPayload(String prompt, int count) {
        // Create Gson instance
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Create the main object
        JsonObject mainObject = new JsonObject();
        mainObject.addProperty("prompt", prompt);
        mainObject.addProperty("negativePrompt", "Flowers.");
        mainObject.addProperty("contentClass", "photo");
        mainObject.addProperty("n", count);

        // Add seeds array
        mainObject.add("seeds", gson.toJsonTree(Collections.singletonList(23442)));

        // Create size object
        JsonObject sizeObject = new JsonObject();
        sizeObject.addProperty("width", 1028);
        sizeObject.addProperty("height", 512);
        mainObject.add("size", sizeObject);

        // Create photoSettings object
        JsonObject photoSettingsObject = new JsonObject();
        photoSettingsObject.addProperty("aperture", 1.2);
        photoSettingsObject.addProperty("shutterSpeed", 0.0005);
        photoSettingsObject.addProperty("fieldOfView", 14);
        mainObject.add("photoSettings", photoSettingsObject);

        // Create styles object
        JsonObject stylesObject = new JsonObject();
        stylesObject.add("presets", gson.toJsonTree(Collections.singletonList("string")));
        stylesObject.addProperty("strength", 60);
        mainObject.add("styles", stylesObject);

        mainObject.addProperty("visualIntensity", 6);
        mainObject.addProperty("locale", "en-US");

        // Convert to JSON string

        return gson.toJson(mainObject);
    }
}
