# simple-paxos
A simple server network using an incredibly basic Paxos algorithm to gain consensus on one field.

## How to Build and Run this Project

Must have Java 8 installed to run this project.

1. From the root directory, run `./gradlew shadowJar`
2. From the same directory, run `java -jar build/libs/simple-paxos-all.jar --serverPort <port> --networkPorts <comma delimited ports>`

**--serverPort** The port you plan to run a particular node on. All nodes are hardcoded to run on local 127.0.0.1.

**--networkPorts** The ports that are part of your port and you will gain consensus with.

