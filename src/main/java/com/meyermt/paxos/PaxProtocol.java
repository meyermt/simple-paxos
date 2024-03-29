package com.meyermt.paxos;

/**
 * A POJO that represents the JSON structure within the Paxos Protocol for this program.
 * Created by michaelmeyer on 5/3/17.
 */
public class PaxProtocol {

    public static String PROPOSE = "propose";
    public static String AGREE = "agree";
    public static String REJECT = "reject";
    public static String COMMIT = "commit";
    public static String AGREE_COMMIT = "agreecommit";

    private int port;
    private String action;
    private int proposedPrice;
    private int sequence;
    private int sourcePort;

    public PaxProtocol() {
    }

    /**
     * Constructs a new Paxos Protocol bean.
     * @param port port object is destined for.
     * @param action type of action for object
     * @param proposedPrice current running proposed price for consensus
     * @param sequence current running highest sequence
     * @param sourcePort port from which request originated
     */
    public PaxProtocol(int port, String action, int proposedPrice, int sequence, int sourcePort) {
        this.port = port;
        this.action = action;
        this.proposedPrice = proposedPrice;
        this.sequence = sequence;
        this.sourcePort = sourcePort;
    }

    public int getPort() {
        return port;
    }

    public String getAction() {
        return action;
    }

    public int getProposedPrice() {
        return proposedPrice;
    }

    public int getSequence() {
        return sequence;
    }

    public int getSourcePort() {
        return sourcePort;
    }
}
