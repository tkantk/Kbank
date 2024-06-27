package com.kbank.core.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.kbank.core.services.GenericRestClient;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component(service = GenericRestClient.class, immediate = true)
public class GenericRestClientImpl implements GenericRestClient {


    public <T> T post(String endpoint, Object requestData, Class<T> responseType, Map<String, String> headers) throws IOException, InterruptedException, URISyntaxException {
        final HttpClient httpClient = HttpClient.newHttpClient();
        String finalRequestData;
        if (requestData instanceof String) {
            finalRequestData = (String) requestData;
        } else  {
            finalRequestData = new Gson().toJson(requestData);
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(endpoint))
                    .POST(HttpRequest.BodyPublishers.ofString(finalRequestData, StandardCharsets.UTF_8));

            headers.forEach(requestBuilder::header);
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
            }

            return new Gson().fromJson(response.body(), responseType);
    }

    public <T> T get(String endpoint, Object requestData, Class<T> responseType, Map<String, String> headers) throws IOException, InterruptedException, URISyntaxException {
        final HttpClient httpClient = HttpClient.newHttpClient();
        String finalRequestData;
        if (requestData instanceof String) {
            finalRequestData = (String) requestData;
        } else  {
            finalRequestData = new Gson().toJson(requestData);
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(endpoint))
                    .GET();
        headers.forEach(requestBuilder::header);

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
            }

        return new Gson().fromJson(response.body(), responseType);
    }

    public <T> T put(String endpoint, Object requestData, Class<T> responseType) throws IOException, InterruptedException, URISyntaxException {
        final HttpClient httpClient = HttpClient.newHttpClient();
        final ObjectMapper objectMapper = new ObjectMapper();
        String jsonRequestData = objectMapper.writeValueAsString(requestData);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(endpoint))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonRequestData, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
            }

            return objectMapper.readValue(response.body(), responseType);
    }

    public <T> T delete(String endpoint, Class<T> responseType) throws IOException, InterruptedException, URISyntaxException {
        final HttpClient httpClient = HttpClient.newHttpClient();
        final ObjectMapper objectMapper = new ObjectMapper();
        HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(endpoint))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
            }

            return objectMapper.readValue(response.body(), responseType);
    }
}
