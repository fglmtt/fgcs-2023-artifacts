#FROM jimador/docker-jdk-8-maven-node

FROM maven:3.8.1-jdk-11

ENV MAVEN_OPTS -Xms16m -Xmx64m

RUN mkdir -p /usr/local/src/mvnapp
WORKDIR /usr/local/src/mvnapp
ADD . /usr/local/src/mvnapp

RUN mvn -Dmaven.test.skip=true clean install dependency:copy-dependencies

WORKDIR /usr/local/src/mvnapp
RUN chmod +x run.sh


CMD ./run.sh it.unimore.dipi.iot.smartobject.process.DeviceProcess
