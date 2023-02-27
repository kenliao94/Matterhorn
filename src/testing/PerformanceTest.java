package testing;

import junit.framework.TestCase;
import logger.LogSetup;

import java.sql.Timestamp;

import org.apache.log4j.Level;
import org.junit.Test;

import app_kvServer.KVServer;
import client.KVStore;
import common.messages.KVMessage;

/**
 * PerformanceTest is a JUnit test class used to test the performance of the KVStore.
 *
 */
public class PerformanceTest extends TestCase {

	@Test
	public void testStoragePerformance() {
		// Variables to modify for different test benchmarks.
		int cacheCapacity = 10;
		String cacheStrategy = "FIFO";
		//KVServer server = null;
		try {
			new LogSetup("logs/testing/test.log", Level.ERROR);
			//server = new KVServer(50000, cacheCapacity, cacheStrategy);
			// Server run should start a separate thread.
			//server.run();
			//String serverAddress = server.getHostname();
			//int serverPort = server.getPort();
			String serverAddress = "localhost";  // TEMP
			int serverPort = 50000;  // TEMP
			KVStore testStore = new KVStore(serverAddress, serverPort);
			testStore.connect();
			System.out.println("Connected to " + serverAddress + " at port " + serverPort);
			int numGets = 0;
			int numPuts = 0;
			double putTime = 0;
			double getTime = 0;
			KVMessage message;
			System.out.println("Performing benchmark on 1000 operations");
			
			for (int i = 0; i < 1000; i++) {
				int randInt = (int)(Math.random() * 1000 % 10);
				boolean getOp = randInt >= 2 ? true : false;
				
				if (getOp) {
					numGets++;
					System.out.println("Performing get() operation.");
					Timestamp startTime = new Timestamp(System.currentTimeMillis());
					String testKey = String.valueOf((int)(Math.random() * 200));
					message = testStore.get(testKey);
					System.out.println("Server response: " + message.toString());
					Timestamp endTime = new Timestamp(System.currentTimeMillis());
					getTime += endTime.getTime() - startTime.getTime();
				} else {
					numPuts++;
					System.out.println("Performing put() operation.");
					Timestamp startTime = new Timestamp(System.currentTimeMillis());
					String testKey = String.valueOf((int)(Math.random() * 200));
					String testValue = String.valueOf((int)(Math.random() * 200));
					message = testStore.put(testKey, testValue);
					System.out.println("Server response: " + message.toString());
					Timestamp endTime = new Timestamp(System.currentTimeMillis());
					putTime += endTime.getTime() - startTime.getTime();
				}
			}
			
			double avgTimeGet = getTime / numGets;
			double avgTimePut = putTime / numPuts;
			double timeTotal = putTime + getTime;
			System.out.println("Total Time: " + String.valueOf(timeTotal));
			System.out.println("Average time for Get: " + String.valueOf(avgTimeGet));
			System.out.println("Average time for Put: " + String.valueOf(avgTimePut));
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			/*if (server != null) {
				server.close();
			}*/
		}
	}

}
