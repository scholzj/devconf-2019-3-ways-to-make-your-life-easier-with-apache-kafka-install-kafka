= Message replay

This demo shows how to do message replay after a failed database.

* Install all components using `kubectl apply -f replay/`
* Check the log files of the loader to see that it loaded all the data into database
* Check the database to see that the data are in the table
* Delete the database pod using `kubectl delete pod $(kubectl get pod -l component=postgres -o=jsonpath='{.items[0].metadata.name}')`
* Check that the database is empty :-/
* Try to restart the loader pod to see that it will not load the messages again (`kubectl delete pod $(kubectl get pod -l component=loader -o=jsonpath='{.items[0].metadata.name}')`)
* Check the consumer offsets using `replay/describe-group.sh`. Notice the current offset column.
* Scale down the loader to 0 replicas. If it runs, we cannot reset the offset.
* Reset the offset to 0 using `replay/reset-group.sh`
* Check the offset again to see how it changed
* Scale the loader up again
* Check the database which now has the records again