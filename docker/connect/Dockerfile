FROM confluentinc/cp-server-connect:7.9.10 AS install-connectors

RUN confluent-hub install --no-prompt confluentinc/kafka-connect-jdbc:latest

# Stage 2 -- copy jars
FROM confluentinc/cp-server-connect:7.9.10

COPY --from=install-connectors /usr/share/confluent-hub-components/ /usr/share/confluent-hub-components/