FROM scholzj/centos-java-base:latest

ARG version=latest
ENV VERSION ${version}

COPY ./scripts/ /bin

ADD target/replay-init.jar /
ADD init.json /

CMD ["/bin/run.sh", "/replay-init.jar"]