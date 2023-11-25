package org.example.kafka.config;


import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.provider.ConfigProvider;
import org.example.kafka.config.gcp.GcpSecretManagerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of {@link ConfigProvider}
 */
public class GcpSmConfigProvider implements ConfigProvider {

    private static final Logger logger = LoggerFactory.getLogger(GcpSmConfigProvider.class);

    public void configure(Map<String, ?> configs) {
        logger.info("{}", configs);
        logger.info("{} is loaded with GOOGLE_APPLICATION_CREDENTIALS: {}",
                GcpSmConfigProvider.class,
                System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
    }

    public ConfigData get(String path) {
        throw new RuntimeException("GcpSmConfigProvider.get(String path) is not implemented! Please use get by key");
    }

    /**
     * Retrieves the data with the given keys at the given Properties file.
     *
     * @param projectId the GCP project that secrets stores
     * @param secrets the secrets and its version
     * @return the configuration data
     */
    public ConfigData get(String projectId, Set<String> secrets) {
        Map<String, String> data = new HashMap<>();
        try (GcpSecretManagerHandler handler = ServiceProvider.secretManagerHandler(projectId)) {
            for (String secretStr : secrets) {
                String[] parts = secretStr.split(":");
                String secretVersion = "latest";
                logger.info("Loading ProjectId({}) SecretVersionName({})", projectId, secretStr);
                if (parts.length >= 2) {
                    secretVersion = parts[1];
                }
                String secretId = parts[0];
                String secret = handler.getSecret(secretId, secretVersion);
                if (secret != null) {
                    data.put(secretStr, secret);
                }
            }
            return new ConfigData(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch secret from GCP Secret Manager", e);
        }
    }

    public void close() {
    }
}