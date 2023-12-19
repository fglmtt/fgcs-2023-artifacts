FROM maven:3.8.1-jdk-11

ENV MAVEN_OPTS -Xms16m -Xmx64m

ENV PYTHONUNBUFFERED=1
RUN apt-get update
RUN apt-get install -y python3

RUN mkdir -p /usr/local/src/mvnapp
WORKDIR /usr/local/src/mvnapp
ADD . /usr/local/src/mvnapp

RUN mvn -Dmaven.test.skip=true clean install dependency:copy-dependencies

WORKDIR /usr/local/src/mvnapp
RUN chmod +x run.sh


CMD ./run.sh it.dt.function.DigitalTwinProcess
