package com.meyermt.paxos;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

/**
 * A server/listener that does all the work for the algorithm. Responsible for listening for PaxProtocol items from other
 * nodes as well as broadcasting items from "outside" users (command line users).
 * Created by michaelmeyer on 5/3/17.
 */
public class PaxListener implements Runnable {
    private Logger logger = LoggerFactory.getLogger(PaxListener.class);
    private volatile boolean exit = false;
    private ServerSocket server;
    private List<Integer> networkPorts;
    private int price;
    private int sequence;
    private Optional<Integer> agreedPrice;
    private Optional<Integer> agreedSequence;
    private int agreeCount = 0;
    private int agreeCommitCount = 0;
    private boolean agreeCommitEnded = false;
    private boolean proposalEnded = false;

    /**
     * Instantiates a PaxListener.
     *
     * @param server       The server to use to listen for connections.
     * @param networkPorts The ports in the network
     */
    public PaxListener(ServerSocket server, List<Integer> networkPorts) {
        this.server = server;
        this.networkPorts = networkPorts;
        this.sequence = 1;
        this.price = 1;
        this.agreedPrice = Optional.empty();
        this.agreedSequence = Optional.empty();
    }

    /*
        Determines how to respond to various PaxProtocol items.
     */
    @Override
    public void run() {
        logger.info("Running server on port {}", server.getLocalPort());
        try {
            while (!exit) {
                Socket client = server.accept();
                new Thread(
                        new Runnable() {
                            public void run() {
                                PaxProtocol proto = readResultToProtocol(client);
                                if (proto.getAction().equals(PaxProtocol.PROPOSE)) {
                                    logger.info("Got request from {}", proto.getSourcePort());
                                    if (agreedSequence.isPresent()) {
                                        logger.info("Have an agreed sequence");
                                        if (agreedSequence.get() > proto.getSequence()) {
                                            logger.info("Agreed sequence higher");
                                            PaxProtocol returnProto = new PaxProtocol(server.getLocalPort(), PaxProtocol.REJECT, agreedPrice.get(), agreedSequence.get(), server.getLocalPort());
                                            sendProto(returnProto, proto.getSourcePort());
                                        } else {
                                            logger.info("Proposal higher than agreed");
                                            agreedPrice = Optional.of(proto.getProposedPrice());
                                            agreedSequence = Optional.of(proto.getSequence());
                                            PaxProtocol returnProto = new PaxProtocol(server.getLocalPort(), PaxProtocol.AGREE, proto.getProposedPrice(), proto.getSequence(), server.getLocalPort());
                                            sendProto(returnProto, proto.getSourcePort());
                                        }
                                    } else {
                                        logger.info("Do not have agreed sequence yet so agreeing");
                                        agreedPrice = Optional.of(proto.getProposedPrice());
                                        agreedSequence = Optional.of(proto.getSequence());
                                        PaxProtocol returnProto = new PaxProtocol(server.getLocalPort(), PaxProtocol.AGREE, proto.getProposedPrice(), proto.getSequence(), server.getLocalPort());
                                        sendProto(returnProto, proto.getSourcePort());
                                    }
                                } else if (proto.getAction().equals(PaxProtocol.AGREE)) {
                                    logger.info("Received agree");
                                    agreeCount++;
                                    if (agreeCount > (networkPorts.size() / 2) && !proposalEnded) {
                                        agreeCount = 0;
                                        proposalEnded = true;
                                        logger.info("everyone agreed on a price of {}", proto.getProposedPrice());
                                        sendCommits(proto.getProposedPrice());
                                    }
                                } else if (proto.getAction().equals(PaxProtocol.REJECT)) {
                                    logger.info("Received reject");
                                    agreeCommitEnded = false;
                                    agreeCount = 0;
                                    agreeCommitCount = 0;
                                    proposalEnded = false;
                                    sequence = proto.getSequence();
                                    price = proto.getProposedPrice();
                                    agreedPrice = Optional.of(proto.getProposedPrice());
                                    agreedSequence = Optional.of(proto.getSequence());
                                    proposePrice(proto.getProposedPrice(), proto.getSequence());
                                } else if (proto.getAction().equals(PaxProtocol.COMMIT)) {
                                    logger.info("Received commit agreement offer");
                                    if (agreedSequence.isPresent()) {
                                        logger.info("Have an agreed sequence");
                                        if (agreedSequence.get() > proto.getSequence()) {
                                            logger.info("Agreed sequence {} higher than commit value {}", agreedSequence.get(), proto.getSequence());
                                            PaxProtocol returnProto = new PaxProtocol(server.getLocalPort(), PaxProtocol.REJECT, agreedPrice.get(), agreedSequence.get(), server.getLocalPort());
                                            sendProto(returnProto, proto.getSourcePort());
                                        } else {
                                            PaxProtocol returnProto = new PaxProtocol(server.getLocalPort(), PaxProtocol.AGREE_COMMIT, proto.getProposedPrice(), proto.getSequence(), server.getLocalPort());
                                            sendProto(returnProto, proto.getSourcePort());
                                            logger.info("Committing since offer still higher");
                                            price = proto.getProposedPrice();
                                            sequence = proto.getSequence();
                                        }
                                    } else {
                                        logger.info("Committing from promise, still no other agreements"); // this is actually impossible but seems better to have complete logic
                                        PaxProtocol returnProto = new PaxProtocol(server.getLocalPort(), PaxProtocol.AGREE_COMMIT, proto.getProposedPrice(), proto.getSequence(), server.getLocalPort());
                                        sendProto(returnProto, proto.getSourcePort());
                                        price = proto.getProposedPrice();
                                        sequence = proto.getSequence();
                                    }
                                } else if (proto.getAction().equals(PaxProtocol.AGREE_COMMIT)) {
                                    logger.info("Received commit agreement");
                                    agreeCommitCount++;
                                    if (agreeCommitCount > (networkPorts.size() / 2) && !agreeCommitEnded) {
                                        agreeCommitEnded = true;
                                        agreeCommitCount = 0;
                                        logger.info("Yay you will change your price to {}", proto.getProposedPrice());
                                        agreedPrice = Optional.of(proto.getProposedPrice());
                                        agreedSequence = Optional.of(proto.getSequence());
                                        sequence = proto.getSequence();
                                        price = proto.getProposedPrice();
                                    }
                                }
                            }
                        }).start();
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to connect with client socket", e);
        }
    }

    /**
     *
     * @param proposedPrice
     * @param proposedSeq
     */
    public void proposePrice(int proposedPrice, int proposedSeq) {
        proposalEnded = false;
        networkPorts.stream().forEach(port -> {
            PaxProtocol proto = new PaxProtocol(port, PaxProtocol.PROPOSE, proposedPrice, proposedSeq, server.getLocalPort());
            sendProto(proto, port);
        });
        agreeCount = 0;
    }

    /**
     * Broadcasts commit messages to all nodes, asking them to commit to updating their values.
     * @param proposedPrice The price to broadcast
     */
    public void sendCommits(int proposedPrice) {
        agreeCommitEnded = false;
        networkPorts.stream().forEach(port -> {
            PaxProtocol proto = new PaxProtocol(port, PaxProtocol.COMMIT, proposedPrice, sequence, server.getLocalPort());
            sendProto(proto, port);
        });
    }

    /**
     * Sends a PaxProtocol bean to a given node.
     * @param proto The bean to send along in request
     * @param port The port to send request to
     */
    public void sendProto(PaxProtocol proto, int port) {
        try {
            Gson gson = new Gson();
            Socket client = new Socket(PaxServer.SHARED_IP, port);
            PrintWriter output = new PrintWriter(client.getOutputStream(), true);
            logger.info("sending proto: {}", gson.toJson(proto));
            output.println(gson.toJson(proto));
        } catch (IOException e) {
            throw new RuntimeException("Error sending proto to port " + port, e);
        }
    }

    /**
     * Custom method to stop the server.
     */
    public void stopServer() {
        exit = true;
    }

    public int getPrice() {
        return this.price;
    }

    public int getSequence() {
        return this.sequence;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public void setAgreedPrice(int price) {
        this.agreedPrice = Optional.of(price);
    }

    public void setAgreedSequence(int sequence) {
        this.agreedSequence = Optional.of(sequence);
    }

    /*
        Reusable method to read PaxProtocol beans in.
     */
    private PaxProtocol readResultToProtocol(Socket client) {
        logger.info("Entering readResultToProtocol");
        try (BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));)
        {
            String clientInput = input.readLine();
            Gson gson = new Gson();
            logger.info("received json: {}", clientInput);
            return gson.fromJson(clientInput, PaxProtocol.class);
        } catch (IOException e) {
            throw new RuntimeException("Encounteered error reading message from client.", e);
        }
    }
}
