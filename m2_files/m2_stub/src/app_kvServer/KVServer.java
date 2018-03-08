package app_kvServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
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

import client.KVStore;

import cache.KVCache;
import cache.KVFIFOCache;
import cache.KVLFUCache;
import cache.KVLRUCache;

import app_kvServer.IKVServer;

import common.helper.MD5Hasher;
import common.message.MetaData;
import common.message.MetaDataEntry;

public class KVServer implements IKVServer {

    /**
     * Start KV Server with selected name
     * @param name          unique name of server
     * @param zkHostname    hostname where zookeeper is running
     * @param zkPort        port where zookeeper is running
     */

    private static Logger logger = Logger.getRootLogger();
    private boolean running;
    private ServerSocket serverSocket;
    
    private int cacheSize;
    private CacheStrategy strategy;
    private KVCache cache;
    
    private String dbPath = "./db/";
    
    private MetaDataEntry metaDataEntry;
    private String name;
    private String zkHostname;
    private int zkPort;
    private boolean writeLock;

    public KVServer(String name,
                    String zkHostname,
                    int zkPort) {
        /* KenNote: The func signature is different in M2 */
    	this.name = name;
    	this.zkHostname = zkHostname;
        this.zkPort = zkPort;
        this.dbPath += name + "/";
    }
    
    public void initKVServer(MetaDataEntry metaDataEntry, int cacheSize, String strategy) {
    	
    	this.metaDataEntry = metaDataEntry;
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
        running = initializeServer();
        if(serverSocket != null) {
            while(running){
                try {
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
    
    // invoker should provide zkHostname, zkPort, name
    public static void main(String[] args) {
		try {
			if (args.length != 4) {
				System.out.println("Wrong number of arguments passed to server");
			}
			new LogSetup("logs/server.log", Level.ALL);
			String zkHostname = args[0];
			int zkPort = Integer.parseInt(args[1]);
			String name = args[2];
			KVServer server = new KVServer(name, zkHostname, zkPort);
			server.run();
		} catch(Exception e) {
			logger.error("Error! Can't start server");
		}
	}



    @Override
    public int getPort(){
        /* KenNote: Just copied over from M1 */
        return metaDataEntry.serverPort;
    }


    @Override
    public String getHostname(){
        /* KenNote: Just copied over from M1 */
        try {
            return InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public CacheStrategy getCacheStrategy(){
        /* KenNote: Just copied over from M1 */
        return strategy;
    }


    @Override
    public int getCacheSize(){
        /* KenNote: Just copied over from M1 */
        return cacheSize;
    }


    @Override
    public boolean inStorage(String key){
        /* KenNote: Just copied over from M1 */
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
        /* KenNote: Just copied over from M1 */
        if (cache != null && cache.get(key) != null) {
            return true;
        }
        return false;
    }


    @Override
    public synchronized String getKV(String key) throws Exception{
        /* KenNote: Just copied over from M1 */
        String value = null;
        if (cache != null) {
            value = cache.get(key);
            if (value != null) {
                return value;
            }
        }
        key += ".kv";
        try {
        	value = getValueFromFile(dbPath + key);
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
        /* KenNote: Just copied over from M1 */
    	if (writeLock == true)
    		return;
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
                + key + "'");
        }
    }
    
    @Override
	public synchronized boolean deleteKV(String key) throws Exception{
    	if (writeLock == true)
    		return false;
		boolean result = false;
    	if (inCache(key))
    		cache.delete(key);
    	key += ".kv";
    	File kvFile = new File(dbPath + key);
    	if (kvFile.exists()) {
    		kvFile.delete();
    		result = true;
    	}
    	return result;
	}


    @Override
    public synchronized void clearCache(){
        /* KenNote: Just copied over from M1 */
        cache = createCache(strategy);
    }


    @Override
    public synchronized void clearStorage(){
        /* KenNote: Just copied over from M1 */
        File[] files = new File(dbPath).listFiles();
        for (File file: files) {
            if (file.toString().endsWith(".kv")) {
                file.delete();
            }
        }
    }


    @Override
    public void run(){
        /* KenNote: Just copied over from M1 */
        running = initializeServer();
        
        if(serverSocket != null) {
            while(running){
                try {
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
        /* KenNote: Just copied over from M1 */
        stopServer();
    }


    @Override
    public void close(){
        /* KenNote: Just copied over from M1 */
        // Cache is write-through, so saving is not necessary.
        stopServer();
    }


    @Override
    public void start() {
        // New: ECS related
    }


    @Override
    public void stop() {
        // New: ECS related
    }


    @Override
    public void lockWrite() {
    	writeLock = true;
    }


    @Override
    public void unlockWrite() {
    	writeLock = false;
    }
    
    @Override
    public void update(MetaDataEntry metaDataEntry) {
    	this.metaDataEntry = metaDataEntry;
    }


    @Override
    // assume hashRange is given in clockwise rotation
    public boolean moveData(BigInteger[] hashRange, String targetName) throws Exception {
    	// special case: hashRange across starting of the ring or end of the ring
    	if (hashRange[0].compareTo(hashRange[1]) == 1) {
    		hashRange[1] = hashRange[1].add(hashRange[0]);
    	}
    	// logic to retrive MetaData
    	MetaDataEntry targetMetaDataEntry = metaData.getMetaData().get(targetName);
    	KVStore migrationClient = new KVStore(targetMetaDataEntry.serverHost, targetMetaDataEntry.serverPort);
    	
    	migrationClient.connect();
    	MD5Hasher hasher = new MD5Hasher();
    	File[] files = new File(dbPath).listFiles();
        for (File file: files) {
        	String fileName = file.toString();
            if (fileName.endsWith(".kv")) {
                String key = fileName.substring(0, fileName.length() - 4);
                BigInteger hashValue = hasher.hashString(key);
                
                if (hashValue.compareTo(hashRange[0]) == 1 && hashValue.compareTo(hashRange[1]) == -1) {
                	String value = getValueFromFile(dbPath + fileName);
                	migrationClient.put(key, value);
                }
            }
        }
        migrationClient.disconnect();
        return true;
    }
    
    private String getValueFromFile(String path) throws IOException {
    	File kvFile = new File(path);
    	String value = null;
        if (kvFile.exists()) {
            FileReader fileReader = new FileReader(kvFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            value = bufferedReader.readLine();
            bufferedReader.close();
        }
        return value;
    }
    
    private void stopServer() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Error! " +
                    "Unable to close socket on port: " + getPort(), e);
        }
    }


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


    private boolean initializeServer() {
        logger.info("Initialize server ...");
        try {
            serverSocket = new ServerSocket(getPort());
            logger.info("Server listening on port: " 
                    + serverSocket.getLocalPort());    
            return true;
        
        } catch (IOException e) {
            logger.error("Error! Cannot open server socket:");
            if(e instanceof BindException){
                logger.error("Port " + getPort() + " is already bound!");
            }
            return false;
        }
    }
}
