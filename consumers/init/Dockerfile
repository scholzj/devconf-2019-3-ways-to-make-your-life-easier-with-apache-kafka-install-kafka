FROM scholzj/centos-java-base:latest

ARG version=latest
ENV VERSION ${version}

COPY ./scripts/ /bin

ADD target/consumer-init.jar /
ADD init.json /

CMD ["/bin/run.sh", "/consumer-init.jar"]