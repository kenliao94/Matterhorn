package app_kvClient;

// Java Import
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

// 3rd party library import
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

// Internal import
import logger.LogSetup;

import client.KVCommInterface;
import client.KVStore;

import common.messages.KVMessage;


public class KVClient implements IKVClient {

    private static Logger logger = Logger.getRootLogger();
    private static final String PROMPT = "KVClient> ";
    private BufferedReader stdin;
    private KVStore kvstore = null;
    private boolean stop = false;

    public void run() {
        while(!stop) {
            stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(PROMPT);
            
            try {
                String cmdLine = stdin.readLine();
                this.handleCommand(cmdLine);
            } catch (IOException e) {
                stop = true;
                printError("CLI does not respond - Application terminated ");
            }
        }
    }
 
    @Override
    public void newConnection(String hostname, int port)
    		throws UnknownHostException, IOException {
    	kvstore = new KVStore(hostname, port);
    	kvstore.connect();
    	System.out.println("Connected to server successfully");
    	logger.info("Connection established");
    }

    @Override
    public void closeConnection() throws IOException {
    	kvstore.disconnect();
    	System.out.println("Connection closed successfully");
    	logger.info("Connection closed");
    }

    @Override
    public KVCommInterface getStore() {
    	return kvstore;
    }
 
    public void handleCommand(String cmdLine) {
        String[] tokens = cmdLine.split("\\s+");

        if(tokens[0].equals("quit")) {
            stop = true;
            disconnect();
            printInfo("Application exit!");        
        } else if (tokens[0].equals("connect")){
            if(tokens.length == 3) {
                try {
                    newConnection(tokens[1], Integer.parseInt(tokens[2]));
                } catch (UnknownHostException e) {
                    printError("Unknown Host!");
                } catch (IOException e) {
                	printError("Failed to connect to server");
                }
            } else {
                printError("Invalid number of parameters!");
            }
        } else if (tokens[0].equals("put")) {
        	try{
	            if(tokens.length == 3) {
	            	KVMessage res = kvstore.put(tokens[1], tokens[2]);
	            	System.out.println(PROMPT + res.getStatus());
	            } else if (tokens.length == 2){
	            	KVMessage res = kvstore.put(tokens[1], "");
	            	System.out.println(PROMPT + res.getStatus());
	            }else {
	                printError("Wrong number of parameters passed. Please Check Help Manual");
	            }
        	} catch(Exception e) {
        		printError("Failed to put kv pair");
        	}
        } else if (tokens[0].equals("get")) {
        	if(tokens.length == 2) {
        		try{
        			KVMessage res = kvstore.get(tokens[1]);
        			System.out.println(PROMPT + res.getValue());
        		} catch (Exception e) {
        			printError("Failed to get key pair");
        		}
            } else {
            	printError("Wrong number of parameters passed. Please Check Help Manual");
        	}
        } else if(tokens[0].equals("disconnect")) {
        	kvstore.disconnect();
        } else if(tokens[0].equals("logLevel")) {
            if(tokens.length == 2) {
                String level = setLevel(tokens[1]);
                if(level.equals(LogSetup.UNKNOWN_LEVEL)) {
                    printError("No valid log level!");
                    printPossibleLogLevels();
                } else {
                    System.out.println(PROMPT + 
                            "Log level changed to level " + level);
                }
            } else {
                printError("Invalid number of parameters!");
            }
        } else if(tokens[0].equals("help")) {
            printHelp();
        } else {
            printError("Unknown command");
            printHelp();
        }
    }

    private void disconnect() {
        if(kvstore != null) {
            kvstore.disconnect();
            kvstore = null;
        }
    }

    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT).append("KV CLIENT HELP (Usage):\n");
        sb.append(PROMPT);
        sb.append("::::::::::::::::::::::::::::::::");
        sb.append("::::::::::::::::::::::::::::::::\n");
        sb.append(PROMPT).append("connect <host> <port>");
        sb.append("\t\t establishes a connection to a server\n");
        sb.append(PROMPT).append("disconnect");
        sb.append("\t\t\t disconnects from the server \n");
        sb.append(PROMPT).append("put <key> <value>");
        sb.append("\t\t Store a KV pair on the server \n");
        sb.append(PROMPT).append("get <key>");
        sb.append("\t\t\t Retrieve a KV pair on the server \n");
        sb.append(PROMPT).append("closeConnection");
        sb.append("\t\t\t closes the current connection \n");
        sb.append(PROMPT).append("logLevel");
        sb.append("\t\t\t ");
        sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");
        sb.append(PROMPT).append("quit ");
        sb.append("\t\t\t\t exits the program");
        System.out.println(sb.toString());
    }

    private void printPossibleLogLevels() {
        System.out.println(PROMPT 
                + "Possible log levels are:");
        System.out.println(PROMPT 
                + "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
    }

    private void printError(String error){
        System.out.println(PROMPT + "Error! " +  error);
    }
    
    private void printInfo(String info){
    	System.out.println(PROMPT + " " + info);
    }

    private String setLevel(String levelString) {
        if(levelString.equals(Level.ALL.toString())) {
            logger.setLevel(Level.ALL);
            return Level.ALL.toString();
        } else if(levelString.equals(Level.DEBUG.toString())) {
            logger.setLevel(Level.DEBUG);
            return Level.DEBUG.toString();
        } else if(levelString.equals(Level.INFO.toString())) {
            logger.setLevel(Level.INFO);
            return Level.INFO.toString();
        } else if(levelString.equals(Level.WARN.toString())) {
            logger.setLevel(Level.WARN);
            return Level.WARN.toString();
        } else if(levelString.equals(Level.ERROR.toString())) {
            logger.setLevel(Level.ERROR);
            return Level.ERROR.toString();
        } else if(levelString.equals(Level.FATAL.toString())) {
            logger.setLevel(Level.FATAL);
            return Level.FATAL.toString();
        } else if(levelString.equals(Level.OFF.toString())) {
            logger.setLevel(Level.OFF);
            return Level.OFF.toString();
        } else {
            return LogSetup.UNKNOWN_LEVEL;
        }
    }
 
    public static void main(String[] args) {
        try {
            new LogSetup("logs/client.log", Level.OFF);
            KVClient ui = new KVClient();
            ui.run();
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
