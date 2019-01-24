FROM strimzi/kafka-connect:0.9.0

USER root:root

RUN export DEBEZIUM_VERSION=0.8.3.Final && \
    mkdir -p plugins && cd plugins && \
    for PLUGIN in {mongodb,mysql,postgres}; do \
       curl http://central.maven.org/maven2/io/debezium/debezium-connector-$PLUGIN/$DEBEZIUM_VERSION/debezium-connector-$PLUGIN-$DEBEZIUM_VERSION-plugin.tar.gz | tar xz; \
    done && \
    cd ..

USER 1001
