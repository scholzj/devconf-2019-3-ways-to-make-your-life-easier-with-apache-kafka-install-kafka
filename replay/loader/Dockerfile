FROM scholzj/centos-java-base:latest

ARG version=latest
ENV VERSION ${version}

COPY ./scripts/ /bin

ADD target/replay-demo.jar /

CMD ["/bin/run.sh", "/replay-demo.jar"]