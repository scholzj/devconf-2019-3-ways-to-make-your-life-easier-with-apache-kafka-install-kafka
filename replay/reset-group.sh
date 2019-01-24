#!/usr/bin/env bash

kubectl run kafka-cmd -ti --image=strimzi/kafka:0.9.0 -l app=kafka --rm=true --restart=Never -- bin/kafka-consumer-groups.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --group replay-demo-loader --to-earliest --execute --reset-offsets --topic replay-demo