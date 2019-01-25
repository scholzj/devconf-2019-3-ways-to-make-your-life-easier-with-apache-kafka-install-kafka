# DevConf.CZ 2019: 3 ways o make your life easier with Apache Kafka

This repository contains the demos from my talk about Apache Kafka at [DevConf.CZ](https://devconf.info/cz).

Before running the different demos, you need:

* Start your Minishift or `oc cluster up` OpenShfit cluster or connect to some other Kubernets or OpenShift environment
* Login as a cluster admin `oc login -u system:admin`
* Use the namespace `myproject`
* Deploy [Strimzi Kafka operator](http://strimzi.io) using `oc apply -f ./install-strimzi`
* Deploy Kafka and Kafka Connect cluster using `oc apply -f ./install-kafka`

Afterwards, go for one of the demos:

* [Message replay](./replay/)
* [Adding consumers](./consumers)
* [Change Data Capture](./cdc/)