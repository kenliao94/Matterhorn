package app_kvClient;

import client.KVCommInterface;

public interface IKVClient {
    /**
     * Creates a new connection to hostname:port
     * @throws Exception
     *      when a connection to the server can not be established
     */
    public void newConnection(String hostname, int port) throws Exception;
    
    /**
     * Test the connection to the server
     * @throws Exception
     *      when the connection cannot be tested
     */
    public KVCommInterface getStore();
}