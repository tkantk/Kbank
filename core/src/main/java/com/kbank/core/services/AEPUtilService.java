package com.kbank.core.services;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public interface AEPUtilService {
    public String getLoginTokenEMERALD(String clientID, String clientSecret, String tokenCode) throws IOException, URISyntaxException, InterruptedException;
    public String getTopicDataFromSegment(String segmentID, Map<String,String> header) throws IOException, URISyntaxException, InterruptedException;
    public String[] getSegmentData(String emailID, Map<String,String> header) throws IOException, URISyntaxException, InterruptedException;
    public String getInterestsFromAEP(String emailID) throws IOException, URISyntaxException, InterruptedException;
    public JsonObject createAEPRequestData(String username, String firstName, String lastName, String email, String country, String[] interests);
    public Map<String, String> generateHeadersForAEP();
    public String getLoginTokenForIO() throws IOException, URISyntaxException, InterruptedException;
    public JsonObject getProfileData(String emailID) throws IOException, URISyntaxException, InterruptedException;
    public String getClientIDEmerald();
    public String getClientSecretEmerald();
    public String getTokenCodeEmerald();
    public String getAEPORGID();
    public String getAEPAPIKey();
    public String getAEPSandboxName();
}
