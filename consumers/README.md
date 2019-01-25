# Adding consumers

This demo show that you don't need to do anything to add additional services to your application landscape

* Deploy the e-shop service using `kubect apply -f consumers/eshop.yaml`
* Check its logs to see how many orders you are getting. This will surely be the next Amazon ;-)
* Deploy orders service ... `kubectl apply -f consumers/orders.yaml`
* Deploy invoicing service ... `kubectl apply -f consumers/invoicing.yaml`
* Deploy the shipping service  ... `kubectl apply -f consumers/shipping.yaml`
* and so on ...
* ... and check the log to see how everyone of them processes all the same records without any reconfiguration.