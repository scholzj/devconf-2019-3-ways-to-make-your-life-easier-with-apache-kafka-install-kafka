#!/usr/bin/env bash

kubectl exec -it $(kubectl get pod -l component=postgres -o=jsonpath='{.items[0].metadata.name}') -- psql --username "replyloader" --dbname "shares"