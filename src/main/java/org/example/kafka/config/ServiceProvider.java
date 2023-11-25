package org.example.kafka.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.example.kafka.config.gcp.GcpRestClient;
import org.example.kafka.config.gcp.GcpSecretManagerHandler;
import org.example.kafka.config.gcp.GcpSecretManagerRestHandler;

import java.io.IOException;
import java.net.http.HttpClient;

public final class ServiceProvider {

    private ServiceProvider() {}

    public static GoogleCredentials googleCredentials() throws IOException {
        return ServiceAccountCredentials.getApplicationDefault();
    }

    public static HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    public static GcpSecretManagerHandler secretManagerHandler(String projectId) throws IOException {
        return new GcpSecretManagerRestHandler(projectId);
    }

    public static GcpRestClient gcpRestClient() { return new GcpRestClient(); }
}
