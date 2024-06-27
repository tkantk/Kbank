package com.kbank.core.services;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public interface GenericRestClient {
    <T> T post(String endpoint, Object requestData, Class<T> responseType, Map<String, String> headers) throws IOException, InterruptedException, URISyntaxException;
    <T> T get(String endpoint, Object requestData, Class<T> responseType, Map<String, String> headers) throws IOException, InterruptedException, URISyntaxException;
    <T> T put(String endpoint, Object requestData, Class<T> responseType) throws IOException, InterruptedException, URISyntaxException;
    <T> T delete(String endpoint, Class<T> responseType) throws IOException, InterruptedException, URISyntaxException;
}
