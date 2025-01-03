FROM openjdk:8-jdk
RUN apt-get update && apt-get install -y ant
WORKDIR /workspace
COPY . /workspace
CMD ["ant"]
