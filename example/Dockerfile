FROM confluentinc/cp-kafka-connect:7.5.0
USER root

RUN confluent-hub install --no-prompt confluentinc/kafka-connect-datagen:0.6.3

COPY kafka-config-provider-gcp-1.0-SNAPSHOT.jar /etc/kafka-connect/jars/kafka-config-provider-gcp.jar

# change location of this file
COPY gcp-sa.json /etc/kafka-connect/secrets/gcp-sa.json

USER 1001
ENV CUB_CLASSPATH="$CUB_CLASSPATH:/etc/kafka-connect/jars/*"
ENV GOOGLE_APPLICATION_CREDENTIALS=/etc/kafka-connect/secrets/gcp-sa.json
