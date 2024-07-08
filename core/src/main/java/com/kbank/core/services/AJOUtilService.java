package com.kbank.core.services;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URISyntaxException;

public interface AJOUtilService {
    public JsonObject sendTransactionEmail(JsonObject request) throws IOException, URISyntaxException, InterruptedException;
    public JsonObject createAJOTransactionalMessageRequest(String emailID, String campaignID, JsonObject contextData);
}
