package org.example.kafka.config.gcp;

public abstract class GcpSecretManagerHandler implements AutoCloseable {

    abstract public String getSecret(String secretId, String versionId);

}
