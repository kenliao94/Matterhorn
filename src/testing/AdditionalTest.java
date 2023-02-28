package testing;

import org.junit.Test;

import app_kvServer.KVServer;
import app_kvClient.KVClient;

import client.KVStore;

import common.messages.KVMessage;
import common.messages.KVMessage.StatusType;

import junit.framework.TestCase;

/**
 * This class tests the KVServer and KVClient classes, specifically testing the clearStorage(), inStorage(), deleteKV(), and shellUI() methods, as well as putting keys with a space.
 */
public class AdditionalTest extends TestCase {
	
	private KVServer kvServer;
	private KVStore kvClient;

	public void setUp() {
		kvClient = new KVStore("localhost", 50000);
		try {
			kvClient.connect();
		} catch (Exception e) {
		}
	}

	public void tearDown() {
		kvClient.disconnect();
	}

	@Test
	public void testClearStorage() {
		kvServer = new KVServer(1234, 1234, "LRU");
		Exception ex = null;
		boolean response1 = true;
		boolean response2 = true;
		try {
			kvServer.putKV("test", "test");
			kvServer.putKV("test1", "test1");
			kvServer.clearStorage();
			response1 = kvServer.inStorage("test");
			response2 = kvServer.inStorage("test1");
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex == null && response1 == false && response2 == false);
	}
	
	@Test
	public void testInStorage() {
		kvServer = new KVServer(1234, 1234, "LRU");
		Exception ex = null;
		boolean response1 = false;
		boolean response2 = true;
		try {
			kvServer.putKV("test", "test");
			response1 = kvServer.inStorage("test");
			response2 = kvServer.inStorage("gg");
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex == null && response1 == true && response2 == false);
	}
	
	@Test
	public void testDeleteKV() {
		kvServer = new KVServer(1234, 1234, "LRU");
		Exception ex = null;
		boolean response = true;
		try {
			kvServer.putKV("test", "test");
			kvServer.deleteKV("test");
			response = kvServer.inStorage("test") || kvServer.inCache("test");
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex == null && response == false);
	}
	
	@Test
	public void testShellUI() {
		// Test connect command
		KVClient ui = new KVClient();
		kvServer = new KVServer(50000, 1024, "FIFO");
		ui.handleCommand("connect localhost 50000");
		ui.handleCommand("put testUI testUI"); // Insert
		ui.handleCommand("put testUI"); // Delete
		boolean response = kvServer.inStorage("testUI");
		assertTrue(response == false);
	}
	
	@Test
	public void testPutWithSpace() {
		String key = "space";
		String value = "my string";
		KVMessage response = null;
		Exception ex = null;

		try {
			response = kvClient.put(key, "");
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex == null && response.getStatus() == StatusType.DELETE_SUCCESS);

		try {
			response = kvClient.put(key, value);
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex == null && response.getStatus() == StatusType.PUT_SUCCESS);
		
		try {
			response = kvClient.put(key, "");
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex == null && response.getStatus() == StatusType.DELETE_SUCCESS);
	}
}
