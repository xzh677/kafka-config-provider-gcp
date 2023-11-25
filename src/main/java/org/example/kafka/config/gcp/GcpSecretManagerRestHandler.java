package org.example.kafka.config.gcp;

import com.google.auth.oauth2.GoogleCredentials;
import org.example.kafka.config.ServiceProvider;
import org.example.kafka.config.Utils;

import java.io.IOException;

public class GcpSecretManagerRestHandler extends GcpSecretManagerHandler {

    private final String baseUrl;

    private static final String SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    private final GcpRestClient client;

    private final GoogleCredentials credentials;

    public GcpSecretManagerRestHandler(String projectId) throws IOException {
        credentials = ServiceProvider.googleCredentials().createScoped(SCOPE);
        client = ServiceProvider.gcpRestClient();
        baseUrl = "https://secretmanager.googleapis.com/v1/projects/" + projectId;
    }

    // https://cloud.google.com/secret-manager/docs/reference/rest/v1/projects.secrets.versions/access
    @Override
    public String getSecret(String secretId, String versionId) {
        String url = createSecretUrl(secretId, versionId);
        String jsonBody = client.get(credentials, url);
        GcpSecretResponse response = Utils.fromJsonString(jsonBody, GcpSecretResponse.class);
        return new String(Utils.fromBase64(response.getPayload().getData()));
    }

    private String createSecretUrl(String secretId, String versionId) {
        return String.format(
                "%s/secrets/%s/versions/%s:access",
                baseUrl,
                secretId,
                versionId);
    }

    @Override
    public void close() {}
}
