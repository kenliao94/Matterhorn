package failure_detector;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import client.AddressPair;
import client.KVStore;

public class FailureDetector {
	private int intervalSeconds = 30;
	private String zkHostname;
	private int zkPort;
	private ZooKeeper zk;
	
	public FailureDetector(int intervalSeconds, String zkHostname, int zkPort) {
		this.intervalSeconds = intervalSeconds;
		this.zkHostname = zkHostname;
		this.zkPort = zkPort;
		String connection = this.zkHostname + ":" + Integer.toString(this.zkPort) + "/";
		try {
			this.zk = new ZooKeeper(connection, 3000, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void detect() throws InterruptedException, IOException, KeeperException, NoSuchAlgorithmException {
		while(true) {
			List<String> failedServers = checkServerConnections();
			if (!notifyZookeeper(failedServers)) {
				System.out.println("Failed to notify zookeeper about server crashes!");
			}
			Thread.sleep(intervalSeconds * 1000);
		}
	}
	
	private List<String> checkServerConnections() throws IOException, KeeperException, InterruptedException, NoSuchAlgorithmException {
		HashMap<String, AddressPair> serverAddresses = getServerAddresses();
		List<String> crashedServers = new ArrayList<String>();
		for (Map.Entry<String, AddressPair> entry : serverAddresses.entrySet()) {
			if (!checkConnection(entry.getValue())) {
				System.out.println(entry.getKey() + " is not responding!");
				crashedServers.add(entry.getKey());
			}
		}
		return crashedServers;
	}
	
	private boolean checkConnection(AddressPair addressPair) throws NoSuchAlgorithmException, IOException {
		KVStore client = new KVStore(addressPair.getHost(), addressPair.getPort());
    	try {
    		client.connect();
    		client.get("test", addressPair.getHost(), addressPair.getPort());
    	} catch (IOException e) {
    		return false;
    	}
    	return true;
	}
	
	private HashMap<String, AddressPair> getServerAddresses() throws IOException, KeeperException, InterruptedException {
		HashMap<String, AddressPair> serverAddresses = new HashMap<String, AddressPair>();
		String zkPath = "/";
		List<String> zNodes = zk.getChildren(zkPath, false);
    	for (String zNode: zNodes) {
    		if (!zNode.equals("zookeeper")) {
    			System.out.println("znode: " + zNode);
    			String data = new String(zk.getData(zkPath + zNode, false, null));
                JSONObject jsonMessage = decodeJsonStr(data);
                String serverName = (String)jsonMessage.get("NodeName");
                String serverHost = (String)jsonMessage.get("NodeHost");
                int serverPort = Integer.parseInt(jsonMessage.get("NodePort").toString());
                serverAddresses.put(serverName, new AddressPair(serverHost, serverPort));
    		}
    	}
    	return serverAddresses;
	}
	
	private boolean notifyZookeeper(List<String> failedServers) throws KeeperException, InterruptedException {
		String zkPath = "/fd";
		String data = new String(zk.getData(zkPath, false, null));
		System.out.println(data);  // TEMP
		//this.zk.setData(zkPath, zkData, -1);
		return true;
	}
	
	private JSONObject decodeJsonStr(String data) {
    	JSONObject jsonMessage = null;
        JSONParser parser = new JSONParser();
        try {
            jsonMessage = (JSONObject) parser.parse(data);
        } catch (ParseException e) {
            System.out.println("Error! Unable to parse incoming bytes to json.");
        }
        return jsonMessage;
    }
	
	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Wrong number of arguments passed to failure detector!");
		}
		int intervalSeconds = Integer.parseInt(args[0]);
		String zkHostname = args[1];
		int zkPort = Integer.parseInt(args[2]);
		FailureDetector failureDetector = new FailureDetector(intervalSeconds, zkHostname, zkPort);
		try {
			failureDetector.detect();
		} catch (Exception e) {
			System.out.println("Error! Failure Detector encountered exception!");
			e.printStackTrace();
            System.exit(1);
		}
	}
}
