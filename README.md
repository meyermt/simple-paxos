# simple-paxos
A simple server network using an incredibly basic Paxos algorithm to gain consensus on one field.

## How to Build and Run this Project

Must have Java 8 installed to run this project.

1. From the root directory, run `./gradlew shadowJar`
2. From the same directory, run `java -jar build/libs/simple-paxos-all.jar --serverPort <port> --networkPorts <comma delimited ports>`

**--serverPort** The port you plan to run a particular node on. All nodes are hardcoded to run on local 127.0.0.1.

**--networkPorts** The ports that are part of your port and you will gain consensus with.

## Fun Scenarios to Test

* The easiest scenario is bringing up all nodes, then going to one and proposing a price with `propose-price`. This should result in the price being propogated throughout.

* Update the agreed price and sequence of one node (`set-agreed-price`, `set-agreed-sequence`) and then propose the price of another node (proposed should have lower sequence). Watch the logs go nuts then check that the correct agreed price propogated instead of the proposed.

* Update a number of nodes' agreed prices and sequences and then choose a node to propose a price. Eventually the right sequence/price should prevail.

