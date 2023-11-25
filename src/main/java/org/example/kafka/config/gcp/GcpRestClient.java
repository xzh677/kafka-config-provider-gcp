package org.example.kafka.config.gcp;

import com.google.auth.oauth2.GoogleCredentials;
import org.example.kafka.config.ServiceProvider;
import org.example.kafka.config.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class GcpRestClient {

    private enum REQUEST_TYPE {
        POST,
        GET
    }

    private static final Logger logger = LoggerFactory.getLogger(GcpRestClient.class);

    private static final int MAX_RETRIES = 3;

    private static final long RETRY_INTERVAL_MS = 5000;

    private static HttpClient httpClient;

    public GcpRestClient() {
        httpClient = ServiceProvider.httpClient();
    }

    public String get(GoogleCredentials credentials, String url) {
        return request(credentials, url, null, REQUEST_TYPE.GET);
    }

    public String post(GoogleCredentials credentials, String url, String payload) {
        return request(credentials, url, payload, REQUEST_TYPE.POST);
    }

    private String request(GoogleCredentials credentials, String url, String payload, REQUEST_TYPE requestType) {
        return Utils.runWithRetry(MAX_RETRIES, RETRY_INTERVAL_MS, (counter, uniqueId) -> {
            String logMsg = "API call attempts (" + counter + "/" + MAX_RETRIES +
                    ") with callId (" + uniqueId + ") to " + url;
            try {
                credentials.refreshIfExpired();
                String token = credentials.getAccessToken().getTokenValue();
                HttpRequest request;
                if (requestType == REQUEST_TYPE.POST) {
                    request = HttpRequest.newBuilder()
                            .uri(new URI(url))
                            .header("Authorization", "Bearer " + token)
                            .header("Accept", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .build();
                } else {
                    request = HttpRequest.newBuilder()
                            .uri(new URI(url))
                            .header("Authorization", "Bearer " + token)
                            .header("Accept", "application/json")
                            .build();
                }

                logger.info(logMsg);
                // Send the HTTP request
                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int responseCode = httpResponse.statusCode();
                if (200 <= responseCode && responseCode < 300) {
                    logger.info("{} - completed", logMsg);
                    return Optional.of(httpResponse.body());
                } else if (responseCode < 500) {
                    throw new RuntimeException("{} - unable to make API calls. Cause: " + httpResponse.body());
                }
                // retry
                logger.info("{} - failed with http code {}", logMsg, responseCode);
                return Optional.empty();
            } catch (Exception e) {
                logger.error("{} - unable to make API calls", logMsg, e);
                throw new RuntimeException(e);
            }
        });
    }
}
