package com.kbank.core.services;

import java.io.IOException;
import java.net.URISyntaxException;

public interface FireFlyService {

    public String getFireFlyToken() throws IOException, URISyntaxException, InterruptedException;
    public String getFireFlyImage(String token, String prompt, int count) throws IOException, URISyntaxException, InterruptedException;
}
