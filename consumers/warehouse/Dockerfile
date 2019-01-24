FROM scholzj/centos-java-base:latest

ARG version=latest
ENV VERSION ${version}

COPY ./scripts/ /bin

ADD target/consumer.jar /

CMD ["/bin/run.sh", "/consumer.jar"]