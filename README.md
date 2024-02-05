
# Overview
The Kafka GCP Secret Manager Based Config Provider is a custom configuration provider for Apache Kafka that enables you to securely manage and retrieve configuration secrets from Google Cloud Secret Manager. This provider simplifies the process of handling sensitive configuration data such as passwords, API keys, and other secrets used by Kafka applications.

In this repository, you will find an illustrative example that uses `docker-compose` to set up a Kafka Connect environment with connectors running within it. The secrets for both the Kafka Connect environment and connectors are managed by Google Cloud Secret Manager.
## Features
- Seamless integration with Google Cloud Secret Manager.
- Secure storage and management of sensitive Kafka configuration data.
- Easy-to-use syntax for accessing secrets within Kafka applications.

# 0. Prerequisites
Before using the Kafka GCP Secret Manager Based Config Provider, ensure that you meet the following prerequisites:

1. **Google Cloud Platform**:
    - GCP Secret Manager to hold the secrets
    - A Service Account with a JSON key to access the Secret Manager.
2. **Kafka Cluster**: This provider is used for Kafka components, a Kafka cluster is required. You can create your cluster with Confluent Cloud to run the demo.
3. **Java Environment**: You need to have Java 11 and Maven 3.8+ to build this project.

# 1. Usage

## 1.1 Configure the GCP provider

You need to configure the GCP provider in your Kafka components' configuration properties. The Kafka component can be client applications, Kafka brokers, Kafka Connect workers, or others.

For example, add the following properties to your configuration:
```shell
config.providers=gcp
config.providers.gcp.class=org.example.kafka.config.GcpSmConfigProvider
```
If you are using Confluent Kafka Connect docker image, set the environment variables like this:
```shell
CONNECT_CONFIG_PROVIDERS=gcp
CONNECT_CONFIG_PROVIDERS_GCP_CLASS=org.example.kafka.config.GcpSmConfigProvider
```
The Confluent docker image will automatically convert the environment variable into Connect worker properties.

## 1.2. Deploy the Jar file into Kafka runtime

Since the config provider is loaded at the component startup, ensure that the config provider Jar file is available under the Kafka runtime classpath. In the provided example, the config provider jar is copied to the docker image under the path `/etc/kafka-connect/jars/kafka-config-provider-gcp.jar`. 

The Connect runtime classpath is updated to include it:
```shell
ENV CUB_CLASSPATH="$CUB_CLASSPATH:/etc/kafka-connect/jars/*"
```

Refer to `example/Dockerfile` for more details on how this is done in the example.

## 1.3. Setup GCP service account

The config provider uses the default `GoogleCredentials`. You need to use the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to provide the location of the service account credential JSON. Otherwise, the provider won't have the permission to access the GCP Secret Manager. For more details, see [Google Cloud documentation](https://cloud.google.com/docs/authentication/application-default-credentials#GAC)

If you don't want to put a credential file in the docker image or on your VM (for non-docker Connect clusters), you can attach the service account to the underlying VMs.

If you can't attach a service account to your VM, you can modify the code to take the JSON key as a config provider parameter. See more in the section "How to use the configuration for a custom Kafka configure provider."

In the provided example, a docker-based environment is used, and the JSON key is built-in in the docker image (not recommended for production).

## 1.4. Configure secrets
Once you have your config provider Jar and Google Cloud Secret Manager set up, you can create your own secrets and use them. The config provider will automatically fetch the secret based on the following configuration structure:
```shell
${gcp:<gcp-project-id>:<secret-name>[:<secret-version>]}
```
- `gcp` indicates which config provider to use
- `<gcp-project-id>` is where the secret manager located
- `<secret-name>` is the actual secret name in the manager. 
- `<secret-version>` is an optional parameter, defaulting to `latest`.

For example, if you have the GCP project as `my-gcp-test-project` and the following secrets in the Secret Manager:

| Secret Name                                    | Value                                                         | Version |
|-------------------------------------------|---------------------------------------------------------------------| ------- |
| kafka-jass-config                          |    org.apache.kafka.common.security.plain.PlainLoginModule required username='admin' password='admin';                  | 1
| kafka-api-key | ABCTESTKEY | 1 |
| kafka-api-secret | ABCTESTSECRET | 1 |

Then, you need to have the following configuration in `kafka-connection.env.template` for docker based Kafka Connect environment variable:
```shell
CONNECT_SASL_JAAS_CONFIG=$${gcp:my-gcp-test-project:kafka-jass-config:1}
```
or in a Datagen connector configuration:
```json
{
  "connector.class": "io.confluent.kafka.connect.datagen.DatagenConnector",
  "kafka.topic": "test-datagen-topic",
  "quickstart" : "PAGEVIEWS",
  "kafka.auth.mode": "KAFKA_API_KEY",
  "kafka.api.key": "${gcp:my-gcp-test-project:kafka-api-key}",
  "kafka.api.secret": "${gcp:my-gcp-test-project:kafka-api-secret}",
  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.converter": "org.apache.kafka.connect.json.JsonConverter",
  "value.converter.schemas.enable": "false",
  "max.interval": 1000,
  "tasks.max": "1"
}
```
Note: Kafka config provider is expecting one `$` sign as shown in Datagen connector configuration. The double `$` sign is required for docker environment, the extra `$` is for docker compose.

# 2. How to run the example

## 2.1 Build 
```shell
# build the config provider jar
mvn clean package
```

## 2.2 Usage
Create your own `example/kafka-connection.env` based on the template file under the `example` folder.

## 2.3 Run and Test locally
Use the following command to:

- build runtime Jar file
- build docker image that includes runtime Jar and the GCP service account JSON key
- run the connect cluster with docker compose

```shell
export GCP_JSON_KEY_FILEPATH=~/local/creds/sa-01-ee1fab000d0.json
example/run.sh
```

## 2.4 Deploy connector
```shell
curl --location --request PUT 'localhost:8083/connectors/test-connector/config' \
--header 'Content-Type: application/json' \
--data '{
  "connector.class": "io.confluent.kafka.connect.datagen.DatagenConnector",
  "kafka.topic": "test-datagen",
  "quickstart" : "PAGEVIEWS",
  "kafka.auth.mode": "KAFKA_API_KEY",
  "kafka.api.key": "${gcp:my-gcp-test-project:kafka-api-key}",
  "kafka.api.secret": "${gcp:my-gcp-test-project:kafka-api-secret}",
  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.converter": "org.apache.kafka.connect.json.JsonConverter",
  "value.converter.schemas.enable": "false",
  "max.interval": 1000,
  "tasks.max": "1"
}'
```
Make sure the api key and api secret are referenced to the correct GCP Secrets.

# 3. HowTo

## 3.1 How to use the configuration for custom Kafka configure provider

Kafka Properties Configuration for broker, connect cluster, kafka clients and other similar components.

| Config                                    | Description                                                         |
|-------------------------------------------|---------------------------------------------------------------------|
| config.providers                          | A comma-separated list of names for providers.                      |
| config.providers.{name}.class             | The Java class name for a provider.                                 |
| config.providers.{name}.param.{param-name} | A parameter to be passed to the above Java class on initialization. |


Environment Vairable Configuration for Confluent docker images

| Config                                      | Description                                                         |
|---------------------------------------------|---------------------------------------------------------------------|
| CONFIG_PROVIDERS                            | A comma-separated list of names for providers.                      |
| CONFIG_PROVIDERS_{name}_CLASS               | The Java class name for a provider.                                 |
| CONFIG_PROVIDERS_{name}\_PARAM_{param-name} | A parameter to be passed to the above Java class on initialization. |

The param configuration allows you to create your customizations for the provider. For example, loading the GCP service account JSON key from configuration instead of using the `GOOGLE_APPLICATION_CREDENTIALS` environment variable.


