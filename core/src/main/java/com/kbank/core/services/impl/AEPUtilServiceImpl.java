package com.kbank.core.services.impl;

import com.google.gson.*;
import com.kbank.core.services.AEPUtilService;
import com.kbank.core.services.GenericRestClient;
import com.kbank.core.utils.JSONUtil;
import com.kbank.core.utils.impl.JSONUtilImpl;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component(service = AEPUtilService.class, immediate = true)
@Designate(ocd = AEPUtilServiceImpl.Config.class)
public class AEPUtilServiceImpl implements AEPUtilService {

    @ObjectClassDefinition(name = "AEP Helper Service", description = "AEP Helper Service")
    public @interface Config {
        String clientIdEmerald() default "scoe-hackathon-app";
        String clientSecretEmerald() default "s8e-0KET_sozfAH-x3qp7Mn1agaJzvyi2IOg";
        String tokenCodeEmerald() default "eyJhbGciOiJSUzI1NiIsIng1dSI6Imltc19uYTEtc3RnMS1rZXktcGFjLTEuY2VyIiwia2lkIjoiaW1zX25hMS1zdGcxLWtleS1wYWMtMSIsIml0dCI6InBhYyJ9.eyJpZCI6InNjb2UtaGFja2F0aG9uLWFwcF9zdGciLCJ0eXBlIjoiYXV0aG9yaXphdGlvbl9jb2RlIiwiY2xpZW50X2lkIjoic2NvZS1oYWNrYXRob24tYXBwIiwidXNlcl9pZCI6InNjb2UtaGFja2F0aG9uLWFwcEBBZG9iZUlEIiwiYXMiOiJpbXMtbmExLXN0ZzEiLCJvdG8iOmZhbHNlLCJjcmVhdGVkX2F0IjoiMTY5MTA0MTcyMDYwNCIsInNjb3BlIjoic3lzdGVtIn0.ZRUy3wSFFOdimL_G5TXPLfPtfnVA06kn2RPEl86Vo0fhEZuSzV7YskMhrZueGnwIA_1mC2V_67g4szWALiIoqVhw-ezNNHDu6O-MlG6fzIKchpdud01jXM5rpu13pjk1R1eFa2jwxiglW_Kx0MpRQfpHhRjQb3gk4uOWoynt8kJjC9fDyNES51khuE91AbkBtyRwva8wE_aaRFyFNcghNkT0T0ptQT0HyMjpfXWlO01cjJRdFPUp6e6trPJpImIEjqJZ-258wBW8229yHWHbWNrLTrnYo-iwY9Fs-M9yZFvrQoFc_kiS8529muYoIL1igkx4DKx9dsUbqKe7d7aW8g";
        String emeraldBaseUrl() default "https://emerald-stage.adobe.io/collection/Segment_Topic_Prediction_Data/asset";
        String aepSandBoxName() default "usecase-demo";
        String xAdobeId() default "3c8ba9ca-72f3-496a-8d3a-19e3890c8ad4";
        String aepTokenBaseUrl() default "https://ims-na1-stg1.adobelogin.com/ims/token/v1?grant_type=authorization_code";
        String aepSegmentBaseUrl() default "https://edge.adobedc.net/ee/v2/interact?dataStreamId=7241cbb6-d82e-4bb3-b196-eb4204e79f8d";
        String aepLoginTokenBaseUrl() default "https://ims-na1.adobelogin.com/ims/token/v3";
        String aepOrganizationID() default "C683509F655B5C760A495E7E@AdobeOrg";
        String aepProfileClientId() default "ccfdaca258624f76975b5b088c97effe";
        String aepProfileClientSecret() default "p8e-MQTENIOVbzCK8IbvOIBaGIZqTBKDWEcg";
        String aepProfileScope() default "cjm.suppression_service.client.delete,cjm.suppression_service.client.all,openid,session,AdobeID,read_organizations,additional_info.projectedProductContext";
    }

    private static final Logger log = LoggerFactory.getLogger(AEPUtilService.class);

    @Reference
    private GenericRestClient genericRestClient;

    private JSONUtil jsonUtil;

    private String CLIENT_ID_EMERALD;
    private String CLIENT_SECRET_EMERALD;
    private String TOKEN_CODE_EMERALD;

    private String SANDBOX_NAME;
    private String X_ADOBE_ID;

    private String AEP_TOKEN_BASE_URL;
    private String EMERALD_BASE_URL;
    private String AEP_SEGMENT_BASE_URL;

    private String AEP_LOGIN_TOKEN_BASE_URL;
    private String AEP_PROFILE_CLIENT_ID;
    private String AEP_PROFILE_CLIENT_SECRET;
    private String AEP_PROFILE_SCOPE;
    private String AEP_ORG_ID;

    private Config config;

    @Activate
    protected void activate(Config config) {
        this.config = config;
        this.CLIENT_ID_EMERALD = config.clientIdEmerald();
        this.CLIENT_SECRET_EMERALD = config.clientSecretEmerald();
        this.TOKEN_CODE_EMERALD = config.tokenCodeEmerald();
        this.EMERALD_BASE_URL = config.emeraldBaseUrl();
        this.SANDBOX_NAME = config.aepSandBoxName();
        this.X_ADOBE_ID = config.xAdobeId();
        this.AEP_TOKEN_BASE_URL = config.aepTokenBaseUrl();
        this.AEP_SEGMENT_BASE_URL = config.aepSegmentBaseUrl();
        this.AEP_LOGIN_TOKEN_BASE_URL = config.aepLoginTokenBaseUrl();
        this.AEP_PROFILE_CLIENT_ID = config.aepProfileClientId();
        this.AEP_PROFILE_CLIENT_SECRET = config.aepProfileClientSecret();
        this.AEP_PROFILE_SCOPE = config.aepProfileScope();
        this.AEP_ORG_ID = config.aepOrganizationID();
    }

    @Override
    public String getLoginTokenEMERALD(String clientID, String clientSecret, String tokenCode) throws IOException, URISyntaxException, InterruptedException {

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

    /**@Override
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
    }**/

@Override
public String getInterestsFromAEP(String emailID) throws IOException, URISyntaxException, InterruptedException {
    String interests = StringUtils.EMPTY;
    JsonObject entity = getProfileData(emailID);
    if (entity != null) {
        JsonObject gdccsc = entity.getAsJsonObject("_gdccsc");
        if (gdccsc != null) {
            interests = gdccsc.get("Interest").getAsString();
        }
    }
    return interests;
}


    @Override
    public JsonObject createAEPRequestData(String username, String firstName, String lastName, String email, String country, String[] interests) {
        JsonObject requestData = new JsonObject();
        JsonObject schemaRefHeader = new JsonObject();
        schemaRefHeader.addProperty("id", "https://ns.adobe.com/gdccsc/schemas/4e76bbbf79770dfd0f03138ae7bc98f50e075fbe709a58ca");
        schemaRefHeader.addProperty("contentType", "application/vnd.adobe.xed-full+json;version=1.0");


        JsonObject header = new JsonObject();
        header.add("schemaRef", schemaRefHeader);
        header.addProperty("imsOrgId", AEP_ORG_ID);
        header.addProperty("datasetId", "6665ea8ed2e8fc2c683b17b4");

        JsonObject schemaRefBody = new JsonObject();
        schemaRefBody.addProperty("id", "https://ns.adobe.com/gdccsc/schemas/4e76bbbf79770dfd0f03138ae7bc98f50e075fbe709a58ca");
        schemaRefBody.addProperty("contentType", "application/vnd.adobe.xed-full+json;version=1.0");

        JsonObject xdmMeta = new JsonObject();
        xdmMeta.add("schemaRef", schemaRefBody);

        JsonObject systemIdentifier = new JsonObject();
        systemIdentifier.addProperty("crmId", username);

        JsonObject gdccsc = new JsonObject();
        gdccsc.addProperty("Interest", String.join(", ", interests));
        gdccsc.add("systemIdentifier", systemIdentifier);

        JsonObject name = new JsonObject();
        name.addProperty("courtesyTitle", "Mr");
        name.addProperty("firstName", firstName);
        name.addProperty("fullName", firstName + " " + lastName);
        name.addProperty("lastName", lastName);
        name.addProperty("middleName", "");
        name.addProperty("suffix", "");

        JsonObject person = new JsonObject();
        person.addProperty("birthDate", "2004-01-12");
        person.addProperty("birthDayAndMonth", "01-12");
        person.addProperty("birthYear", 2004);
        person.addProperty("gender", "male");
        person.addProperty("maritalStatus", "married");
        person.add("name", name);
        person.addProperty("nationality", country);

        JsonObject personalEmail = new JsonObject();
        personalEmail.addProperty("address", username);
        personalEmail.addProperty("primary", true);
        personalEmail.addProperty("status", "active");

        JsonObject xdmEntity = new JsonObject();
        xdmEntity.add("_gdccsc", gdccsc);
        xdmEntity.add("person", person);
        xdmEntity.add("personalEmail", personalEmail);

        JsonObject body = new JsonObject();
        body.add("xdmMeta", xdmMeta);
        body.add("xdmEntity", xdmEntity);

        JsonObject root = new JsonObject();
        root.add("header", header);
        root.add("body", body);

        Gson gson = new Gson();
        requestData = gson.toJsonTree(root).getAsJsonObject();

        return requestData;
    }

    @Override
    public Map<String, String> generateHeadersForAEP() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("sandbox-name", SANDBOX_NAME);
        headers.put("x-adobe-flow-id", X_ADOBE_ID);
        return headers;
    }

    @Override
    public String getLoginTokenForIO() throws IOException, URISyntaxException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> data = new HashMap<>();
        data.put("client_id",AEP_PROFILE_CLIENT_ID);
        data.put("client_secret",AEP_PROFILE_CLIENT_SECRET);
        data.put("scope",AEP_PROFILE_SCOPE);
        data.put("grant_type","client_credentials");

        JsonObject response = genericRestClient.post(AEP_LOGIN_TOKEN_BASE_URL, createFormData(data), JsonObject.class, headers);
        return response.get("access_token").getAsString();
    }

    @Override
    public JsonObject getProfileData(String emailID) throws IOException, URISyntaxException, InterruptedException {
        final String token = getLoginTokenForIO();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("x-api-key", AEP_PROFILE_CLIENT_ID);
        headers.put("x-gw-ims-org-id", AEP_ORG_ID);
        headers.put("x-sandbox-name", SANDBOX_NAME);
        final String url = "https://platform.adobe.io/data/core/ups/access/entities?schema.name=_xdm.context.profile&entityId="+emailID+"&entityIdNS=Ulcrmid&fields=_gdccsc.Interest";
        JsonObject response = genericRestClient.get(url, StringUtils.EMPTY, JsonObject.class, headers);
        jsonUtil = new JSONUtilImpl();
        JsonElement firstElement = jsonUtil.getFirstElement(response);
        JsonObject nestedObject = firstElement.getAsJsonObject();
        return nestedObject.get("entity").getAsJsonObject();
    }

    @Override
    public String getClientIDEmerald() {
        return config.clientIdEmerald();
    }

    @Override
    public String getClientSecretEmerald() {
        return config.clientSecretEmerald();
    }

    @Override
    public String getTokenCodeEmerald() {
        return config.tokenCodeEmerald();
    }

    @Override
    public String getAEPORGID() {
        return config.aepOrganizationID();
    }

    @Override
    public String getAEPAPIKey() {
        return config.aepProfileClientId();
    }

    @Override
    public String getAEPSandboxName() {
        return config.aepSandBoxName();
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
