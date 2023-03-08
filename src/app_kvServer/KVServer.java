package app_kvServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.*;


import logger.LogSetup;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import cache.KVCache;
import cache.KVFIFOCache;
import cache.KVLFUCache;
import cache.KVLRUCache;

import app_kvServer.IKVServer;

/**
 * KVServer implements the IKVServer interface and provides the functionality
 * of a key-value store server. It is responsible for listening for client
 * connections, creating new ClientConnections for each connection, and
 * providing access to the KVCache.
 */
public class KVServer implements IKVServer {

	/**
	 * Start KV Server at given port
	 * @param port given port for storage server to operate
	 * @param cacheSize specifies how many key-value pairs the server is allowed
	 *           to keep in-memory
	 * @param strategy specifies the cache replacement strategy in case the cache
	 *           is full and there is a GET- or PUT-request on a key that is
	 *           currently not contained in the cache. Options are "FIFO", "LRU",
	 *           and "LFU".
	 */
	private static Logger logger = Logger.getRootLogger();
	private boolean running;
	private ServerSocket serverSocket;
	
	private int port;
	private int cacheSize;
	private CacheStrategy strategy;
	private KVCache cache;
	
	private String dbPath = "./db/";
	
	public KVServer(int port, int cacheSize, String strategy) {
		this.port = port;
		this.cacheSize = cacheSize;
		this.strategy = CacheStrategy.valueOf(strategy);
		this.cache = createCache(this.strategy);
		File dbDir = new File(dbPath);
		try{
		 	dbDir.mkdir();
	    } 
	    catch(SecurityException se){
	    	logger.error("Error! Can't create database folder");
	    }        
	}
	
	public static void main(String[] args) {
		try {
			if (args.length != 4) {
				System.out.println("Wrong number of arguments passed to server");
			}
			new LogSetup("logs/server.log", Level.ALL);
			int port = Integer.parseInt(args[0]);
			int cacheSize = Integer.parseInt(args[1]);
			String cacheStrategy = args[2];
			KVServer server = new KVServer(port, cacheSize, cacheStrategy);
			server.run();
		} catch(Exception e) {
			logger.error("Error! Can't start server");
		}
	}

	@Override
	public int getPort(){
		return port;
	}

	@Override
    public String getHostname(){
		try {
			return InetAddress.getLocalHost().toString();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
    public CacheStrategy getCacheStrategy(){
		return strategy;
	}

	@Override	
    public int getCacheSize(){
		return cacheSize;
	}
		
    public synchronized void deleteKV(String key) throws Exception{
    	if (inCache(key))
    		cache.delete(key);
    	key += ".kv";
    	File kvFile = new File(dbPath + key);
    	if (kvFile.exists()) {
    		kvFile.delete();
    	}
	}

	@Override
    public synchronized boolean inStorage(String key){
		boolean result = false;
		key += ".kv";
		File kvFile = new File(dbPath + key);     
        if (kvFile.exists()) {
        	result = true;
        }
		return result;
	}

	@Override
    public synchronized boolean inCache(String key){
		if (cache != null && cache.get(key) != null) {
			return true;
		}
		return false;
	}

	@Override
    public synchronized String getKV(String key) throws Exception{
		String value = null;
		if (cache != null) {
			value = cache.get(key);
			if (value != null) {
				return value;
			}
		}
		key += ".kv";
		try {
            File kvFile = new File(dbPath + key);
            if (kvFile.exists()) {
            	FileReader fileReader = new FileReader(kvFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                value = bufferedReader.readLine();
            	bufferedReader.close();
            }
            return value;
        }
        catch(FileNotFoundException ex) {
        	logger.error("Error! " +
                "Unable to open kv file '" + 
                key + "'" + ex);                
        }
        catch(IOException ex) {
        	logger.error(
                "Error! reading file '" 
                + key + "'" + ex);                  
        }
		return value;
	}

	@Override
    public synchronized void putKV(String key, String value) throws Exception{
		if (cache != null) {
			if (value == cache.get(key)) {
				// Update recency in case of LRU or count in case of LFU.
				cache.set(key, value);
				// Avoid writing to DB.
				return;
			}
			cache.set(key, value);
		}
        key += ".kv";
        File kvFile = new File(dbPath + key);
        if (!kvFile.exists()) {
            try {
                kvFile.createNewFile();
            } catch (IOException e1) {
                logger.error("Error! " +
                        "Unable to create key-value file '" + 
                        key + "'");  
            }
        } 
        try {
            FileWriter fileWriter = new FileWriter(kvFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(value);
            bufferedWriter.close();
        }
        catch(FileNotFoundException ex) {
            logger.error("Error! " +
                "Unable to open kv file '" + 
                key + "'" + ex);                
        }
        catch(IOException ex) {
            logger.error(
                "Error! reading file '" 
                + key + "'" + ex);                  
        }
	}

	/**
	 * Clears the KVCache.
	 */
	@Override
    public synchronized void clearCache(){
		cache = createCache(strategy);
	}

	/**
	 * Clears the KVStorage.
	 */
	@Override
    public synchronized void clearStorage(){
		File[] files = new File(dbPath).listFiles();
		for (File file: files) {
			if (file.toString().endsWith(".kv")) {
				file.delete();
			}
		}
	}

	@Override
    public void run(){
		running = initializeServer();
        
        if(serverSocket != null) {
	        while(running){
	            try {
	            	port = serverSocket.getLocalPort();
	                Socket client = serverSocket.accept();                
	                ClientConnection connection = new ClientConnection(client, this);
	                new Thread(connection).start();
	                
	                logger.info("Connected to " 
	                		+ client.getInetAddress().getHostName() 
	                		+  " on port " + client.getPort());
	            } catch (IOException e) {
	            	logger.error("Error! " +
	            			"Unable to establish connection. \n", e);
	            }
	        }
        }
        logger.info("Server stopped.");
	}

	@Override
    public void kill(){
		stopServer();
	}

	@Override
    public void close(){
		// Cache is write-through, so saving is not necessary.
		stopServer();
	}
	
	private void stopServer() {
		running = false;
        try {
			serverSocket.close();
		} catch (IOException e) {
			logger.error("Error! " +
					"Unable to close socket on port: " + port, e);
		}
	}

	/**
	 * Creates a KVCache based on the given strategy.
	 * 
	 * @param strategy the CacheStrategy to use.
	 * @return the created KVCache.
	 */
	private KVCache createCache(CacheStrategy strategy){
		KVCache cache = null;
		switch (strategy) {
			case LRU:
				cache = new KVLRUCache(this.cacheSize);
				break;
			case FIFO:
				cache = new KVFIFOCache(this.cacheSize);
				break;
			case LFU:
				cache = new KVLFUCache(this.cacheSize);
			default:
				break;
		}
		return cache;
	}

	/**
	 * Initializes the server, opening a ServerSocket on the specified port.
	 * 
	 * @return true if the server was initialized successfully, false otherwise.
	 */
	private boolean initializeServer() {
    	logger.info("Initialize server ...");
    	try {
            serverSocket = new ServerSocket(port);
            logger.info("Server listening on port: " 
            		+ serverSocket.getLocalPort());    
            return true;
        
        } catch (IOException e) {
        	logger.error("Error! Cannot open server socket:");
            if(e instanceof BindException){
            	logger.error("Port " + port + " is already bound!");
            }
            return false;
        }
    }
}
