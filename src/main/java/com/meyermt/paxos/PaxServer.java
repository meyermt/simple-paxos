package com.meyermt.paxos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Main server for simple paxos project. Users enter the port of the server, and comma delimited set of other ports in the
 * network. They then can control a number of settings for each node.
 * Created by michaelmeyer on 5/1/17.
 */
public class PaxServer {

    private static Logger logger = LoggerFactory.getLogger(PaxServer.class);

    // Just using localhost IP all-around for this assignment
    public static String SHARED_IP = "127.0.0.1";
    private static String SERVER_PORT_ARG = "--serverPort";
    private static String NET_PORT_ARG = "--networkPorts";

    private static int port;
    private static List<Integer> networkPorts;

    /**
     * Entry point for PaxServer that first runs the server thread to listen for messages, then allows user to set various
     * node settings before finally proposing a price, which sets off the Paxos algorithm.
     * @param args program args, should include flags and values for --serverPort and --networkPorts
     */
    public static void main(String[] args) {
        if (args.length == 4 && args[0].startsWith(SERVER_PORT_ARG) && args[2].startsWith(NET_PORT_ARG)) {
            port = Integer.parseInt(args[1]);
            networkPorts = Arrays.asList(args[3].split(",")).stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());
            PaxListener paxListener = runServerThread(port, networkPorts);
            sendClientMessages(paxListener);
        } else {
            System.out.println("Illegal arguments. Should be run with arguments: --serverPort <desired port number> --networkPorts <comma delimited ports>");
            System.exit(1);
        }
    }

    /*
        Runs server listener thread and passes back reference in order to be able to access it's fields.
     */
    private static PaxListener runServerThread(int serverPort, List<Integer> networkPorts) {
        try {
            ServerSocket server = new ServerSocket(serverPort);
            PaxListener paxListener = new PaxListener(server, networkPorts);
            new Thread(paxListener).start();
            return paxListener;
        } catch (IOException e) {
            throw new RuntimeException("Unrecoverable issue running server on port: " + serverPort, e);
        }
    }

    /*
        Looping logic to be able to update a node and create more interesting scenarios.
     */
    private static void sendClientMessages(PaxListener paxListener) {
        Scanner scanner = new Scanner(System.in);
        String message = "";
        System.out.println("Welcome to the Exchange Rate Changer. Type 'propose-price' to propose a new rate");
        System.out.println("Type 'get-sequence' to see your sequence number.");
        System.out.println("Type 'set-sequence' to change your sequence number.");
        System.out.println("Type 'get-price' to update your node's current exchange rate value.");
        System.out.println("Type 'set-price' to see your node's current exchange rate value.");
        System.out.println("Type 'set-agreed-price' to update your node's agreed rate.");
        System.out.println("Type 'set-agreed-sequence' to update your node's agreed sequence number.");
        System.out.println("When done, type 'exit' to exit.");
        while (!message.equals("exit")) {
            message = scanner.nextLine();
            if (message.equals("set-sequence")) {
                System.out.println("Enter a new integer value for your node's sequence:");
                message = scanner.nextLine();
                paxListener.setSequence(Integer.parseInt(message));
            } else if (message.equals("set-price")) {
                System.out.println("Enter a new integer value for your node's exchange rate:");
                message = scanner.nextLine();
                paxListener.setPrice(Integer.parseInt(message));
            } else if (message.equals("propose-price")) {
                System.out.println("Enter a new integer value to propose to the network:");
                message = scanner.nextLine();
                paxListener.proposePrice(Integer.parseInt(message), paxListener.getSequence());
            } else if (message.equals("get-sequence")) {
                System.out.println("Your sequence number is: " + paxListener.getSequence());
            } else if (message.equals("get-price")) {
                System.out.println("Your exchange rate is set to: " + paxListener.getPrice());
            } else if (message.equals("set-agreed-price")) {
                System.out.println("Enter a new integer value for agreed price:");
                message = scanner.nextLine();
                paxListener.setAgreedPrice(Integer.parseInt(message));
            } else if (message.equals("set-agreed-sequence")) {
                System.out.println("Enter a new integer value for agreed sequence:");
                message = scanner.nextLine();
                paxListener.setAgreedSequence(Integer.parseInt(message));
            }
        }
        paxListener.stopServer();
        System.exit(0);
    }
}
