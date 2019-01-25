# Change Data Capture

This demo shows how  Change Data Capture works using the [Debezium project](http://debezium.io)

* Deploy all the services `kubectl apply -f cdc/`
* Show the code of the service that it uses only MySQL and not Kafka
* Show the UI
* Deploy the Debezium connector using `cdc/deploy-plugin.sh`
* Check the log of the watcher application while doing changes using the UI `kubectl logs -f $(oc get pod -l component=watcher -o=jsonpath='{.items[0].metadata.name}')` and see ho it inteprets the changes
* Finally, run `cdc/check-messages.sh` to see the _raw_ messages from Debezium
