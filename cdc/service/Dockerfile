FROM scholzj/centos-java-base:latest

ARG version=latest
ENV VERSION ${version}

COPY ./scripts/ /bin

ADD target/cdc-service.jar /

CMD ["/bin/run.sh", "/cdc-service.jar"]