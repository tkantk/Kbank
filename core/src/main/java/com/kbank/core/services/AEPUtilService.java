package com.kbank.core.services;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public interface AEPUtilService {
    public String getLoginToken(String clientID, String clientSecret, String tokenCode) throws IOException, URISyntaxException, InterruptedException;
    public String getTopicDataFromSegment(String segmentID, Map<String,String> header) throws IOException, URISyntaxException, InterruptedException;
    public String[] getSegmentData(String emailID, Map<String,String> header) throws IOException, URISyntaxException, InterruptedException;
    public String getInterestsFromAEP(String emailID) throws IOException, URISyntaxException, InterruptedException;
    public JsonObject createAEPRequestData(String username, String firstName, String lastName, String email, String country, String[] interests);
    public Map<String, String> generateHeadersForAEP();
    public String getLoginTokenForProfile() throws IOException, URISyntaxException, InterruptedException;
    public JsonObject getProfileData(String emailID) throws IOException, URISyntaxException, InterruptedException;
}
