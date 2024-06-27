package com.kbank.core.services.impl;

import com.google.gson.*;
import com.kbank.core.services.AEPUtilService;
import com.kbank.core.services.AIGeneratedPersonalizedDataService;
import com.kbank.core.services.EmailService;
import com.kbank.core.services.GenericRestClient;
import com.kbank.core.servlets.RegisterUserServlet;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component(service = AEPUtilService.class, immediate = true)
public class AEPUtilServiceImpl implements AEPUtilService {

    private static final Logger log = LoggerFactory.getLogger(AEPUtilService.class);

    @Reference
    private GenericRestClient genericRestClient;

    private static final String CLIENT_ID = "scoe-hackathon-app";
    private static final String CLIENT_SECRET = "s8e-0KET_sozfAH-x3qp7Mn1agaJzvyi2IOg";
    private static final String TOKEN_CODE = "eyJhbGciOiJSUzI1NiIsIng1dSI6Imltc19uYTEtc3RnMS1rZXktcGFjLTEuY2VyIiwia2lkIjoiaW1zX25hMS1zdGcxLWtleS1wYWMtMSIsIml0dCI6InBhYyJ9.eyJpZCI6InNjb2UtaGFja2F0aG9uLWFwcF9zdGciLCJ0eXBlIjoiYXV0aG9yaXphdGlvbl9jb2RlIiwiY2xpZW50X2lkIjoic2NvZS1oYWNrYXRob24tYXBwIiwidXNlcl9pZCI6InNjb2UtaGFja2F0aG9uLWFwcEBBZG9iZUlEIiwiYXMiOiJpbXMtbmExLXN0ZzEiLCJvdG8iOmZhbHNlLCJjcmVhdGVkX2F0IjoiMTY5MTA0MTcyMDYwNCIsInNjb3BlIjoic3lzdGVtIn0.ZRUy3wSFFOdimL_G5TXPLfPtfnVA06kn2RPEl86Vo0fhEZuSzV7YskMhrZueGnwIA_1mC2V_67g4szWALiIoqVhw-ezNNHDu6O-MlG6fzIKchpdud01jXM5rpu13pjk1R1eFa2jwxiglW_Kx0MpRQfpHhRjQb3gk4uOWoynt8kJjC9fDyNES51khuE91AbkBtyRwva8wE_aaRFyFNcghNkT0T0ptQT0HyMjpfXWlO01cjJRdFPUp6e6trPJpImIEjqJZ-258wBW8229yHWHbWNrLTrnYo-iwY9Fs-M9yZFvrQoFc_kiS8529muYoIL1igkx4DKx9dsUbqKe7d7aW8g";

    private static final String AEP_TOKEN_BASE_URL = "https://ims-na1-stg1.adobelogin.com/ims/token/v1?grant_type=authorization_code";
    private static final String EMERALD_BASE_URL = "https://emerald-stage.adobe.io/collection/Segment_Topic_Prediction_Data/asset";
    private static final String AEP_SEGMENT_BASE_URL = "https://edge.adobedc.net/ee/v2/interact?dataStreamId=7241cbb6-d82e-4bb3-b196-eb4204e79f8d";

    @Override
    public String getLoginToken(String clientID, String clientSecret, String tokenCode) throws IOException, URISyntaxException, InterruptedException {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> data = new HashMap<>();
        data.put("client_id",clientID);
        data.put("client_secret",clientSecret);
        data.put("code",tokenCode);

        JsonObject response = genericRestClient.post(AEP_TOKEN_BASE_URL, createFormData(data), JsonObject.class, headers);
        return response.get("access_token").getAsString();
    }

    @Override
    public String getTopicDataFromSegment(String segmentID, Map<String, String> header) throws IOException, URISyntaxException, InterruptedException {
        final String url = EMERALD_BASE_URL + "/" + segmentID;
        JsonObject response = genericRestClient.get(url, StringUtils.EMPTY, JsonObject.class, header);
        return response.get("data").toString();
    }

    private String createFormData(Map<String, String> data) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            try {
                String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString());
                String value = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
                joiner.add(key + "=" + value);
            } catch (Exception e) {
               log.info("Error in encoding the data {}", e.getMessage());
            }
        }
        return joiner.toString();
    }

    @Override
    public String[] getSegmentData(String emailID, Map<String,String> header) throws IOException, URISyntaxException, InterruptedException {

        JsonObject requestObject = new JsonObject();
        // Create the  JsonObject
        JsonObject request = new JsonObject();

        // Create the xdm JsonObject
        JsonObject xdm = new JsonObject();

        // Create the identityMap JsonObject
        JsonObject identityMap = new JsonObject();
        JsonArray ulCrmIdArray = new JsonArray();
        JsonObject ulCrmIdObject = new JsonObject();
        ulCrmIdObject.addProperty("id", emailID);
        ulCrmIdObject.addProperty("primary", true);
        ulCrmIdArray.add(ulCrmIdObject);
        identityMap.add("ULCrmId", ulCrmIdArray);

        xdm.add("identityMap", identityMap);
        xdm.addProperty("eventType", "web.webpagedetails.pageViews");

        // Create the web JsonObject
        JsonObject web = new JsonObject();
        JsonObject webPageDetails = new JsonObject();
        webPageDetails.addProperty("URL", "https://alloystore.dev/");
        webPageDetails.addProperty("name", "home-demo-Home Page");
        web.add("webPageDetails", webPageDetails);

        xdm.add("web", web);
        // Get the current instant
        Instant now = Instant.now();

        // Format the instant to the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        String formattedTimestamp = formatter.format(now.atZone(ZoneOffset.UTC));
        xdm.addProperty("timestamp", formattedTimestamp);

        request.add("xdm", xdm);

        // Create the data JsonObject
        JsonObject data = new JsonObject();
        data.addProperty("prop1", "custom value");

        request.add("data", data);

        requestObject.add("event", request);

        // Convert to JSON string
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(requestObject);
        log.info("Request: {}", json);
        header.put("Content-Type", "application/json");
        JsonObject response = genericRestClient.post(AEP_SEGMENT_BASE_URL, requestObject, JsonObject.class, header);
        return getSegmentIds(response).toArray(new String[0]);
    }

    @Override
    public String getInterestsFromAEP(String emailID) throws IOException, URISyntaxException, InterruptedException {
        final String token = getLoginToken(CLIENT_ID, CLIENT_SECRET, TOKEN_CODE);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("x-api-key", "scoe-hackathon-app");
        headers.put("x-gw-ims-org-id", "154340995B76EEF60A494007@AdobeOrg");
        String[] segments = getSegmentData(emailID, headers);
        Set<String> topicSet = new HashSet<>();
        if (segments != null) {
            for (String segment : segments) {
                log.info("User: {}, Segment: {}", emailID, segment);
                // Get topics from AEP
                final String topicData = getTopicDataFromSegment(segment, headers);
                log.info("Topic Data: {}", topicData);
                // Parse the topic data
                Set<String> newTopics = Arrays.stream(topicData.split(",\\s*"))
                        .collect(Collectors.toSet());
                topicSet.addAll(newTopics);
            }
        }

        return String.join(", ", topicSet);
    }

    public static List<String> getSegmentIds(JsonObject jsonObject) {
        List<String> segmentIds = new ArrayList<>();

        JsonArray handleArray = jsonObject.getAsJsonArray("handle");

        if (handleArray != null) {
            for (JsonElement handleElement : handleArray) {
                JsonObject handleObject = handleElement.getAsJsonObject();
                JsonArray payloadArray = handleObject.getAsJsonArray("payload");

                if (payloadArray != null) {
                    for (JsonElement payloadElement : payloadArray) {
                        JsonObject payloadObject = payloadElement.getAsJsonObject();
                        if (payloadObject.get("type") != null) {
                            String type = payloadObject.get("type").getAsString();

                            if ("profileLookup".equals(type)) {
                                JsonArray segmentsArray = payloadObject.getAsJsonArray("segments");

                                if (segmentsArray != null) {
                                    for (JsonElement segmentElement : segmentsArray) {
                                        JsonObject segmentObject = segmentElement.getAsJsonObject();
                                        String segmentId = segmentObject.get("id").getAsString();
                                        segmentIds.add(segmentId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return segmentIds;
    }
}
