package com.kbank.core.services;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public interface AEPUtilService {
    public String getLoginToken(String clientID, String clientSecret, String tokenCode) throws IOException, URISyntaxException, InterruptedException;
    public String getTopicDataFromSegment(String segmentID, Map<String,String> header) throws IOException, URISyntaxException, InterruptedException;
    public String[] getSegmentData(String emailID, Map<String,String> header) throws IOException, URISyntaxException, InterruptedException;
    public String getInterestsFromAEP(String emailID) throws IOException, URISyntaxException, InterruptedException;
}
