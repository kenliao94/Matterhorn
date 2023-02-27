package client;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import common.messages.KVMessage;
import common.messages.TextMessage;
import common.Constants;

public class KVStore implements KVCommInterface {
	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	
	private Socket socket;
	private OutputStream output;
 	private InputStream input;
	private String address;
	private int port;
    private Logger logger = Logger.getRootLogger();

	private static final int BUFFER_SIZE = Constants.BUFFER_SIZE;
	private static final int DROP_SIZE = Constants.DROP_SIZE;
	
	public KVStore(String input_address, int input_port) {
		address = input_address;
		port = input_port;
    }

	@Override
	public void connect() throws UnknownHostException, IOException {
    	socket = new Socket(address, port);
		output = socket.getOutputStream();
		input = socket.getInputStream();
        logger.info("Connection established");
	}

	@Override
	public void disconnect() {
		logger.info("tearing down the connection ...");
		try {
			if (socket != null) {
				//input.close();
				//output.close();
				socket.close();
				socket = null;
				logger.info("connection closed!");
			}
		} catch(IOException ioe) {
			logger.error("Unable to close connection!");
		}
	}

	@Override
	public KVMessage put(String key, String value) throws Exception {
		// Create Request
		TextMessage req = null;
		if (value != "" && value != null) {
			req = new TextMessage("PUT", key, value);
		} else {
			req = new TextMessage("DELETE", key, value);
		}
		byte[] req_byte = req.getMsgBytes();
		output.write(req_byte, 0, req_byte.length);
		output.flush();
		// Wait to read response from server. TODO: add a timeout
		TextMessage res = receiveMessage();
		return res; //Need to use KVMessage not TextMessage
	}

	@Override
	public KVMessage get(String key) throws Exception {
		// Create Request
		TextMessage req = new TextMessage("GET", key, "");
		byte[] req_byte = req.getMsgBytes();
		output.write(req_byte, 0, req_byte.length);
		output.flush();
		// Wait to read response from server. TODO: add a timeout
		TextMessage res = receiveMessage();
		return res;
	}

	private TextMessage receiveMessage() throws IOException {
		int index = 0;
		byte[] msgBytes = null, tmp = null;
		byte[] bufferBytes = new byte[BUFFER_SIZE];
		
		/* read first char from stream */
		byte read = (byte) input.read();	
		boolean reading = true;
		
		while(read != 13 && reading) {/* carriage return */
			/* if buffer filled, copy to msg array */
			if(index == BUFFER_SIZE) {
				if(msgBytes == null){
					tmp = new byte[BUFFER_SIZE];
					System.arraycopy(bufferBytes, 0, tmp, 0, BUFFER_SIZE);
				} else {
					tmp = new byte[msgBytes.length + BUFFER_SIZE];
					System.arraycopy(msgBytes, 0, tmp, 0, msgBytes.length);
					System.arraycopy(bufferBytes, 0, tmp, msgBytes.length,
							BUFFER_SIZE);
				}

				msgBytes = tmp;
				bufferBytes = new byte[BUFFER_SIZE];
				index = 0;
			} 
			
			/* only read valid characters, i.e. letters and numbers */
			if((read > 31 && read < 127)) {
				bufferBytes[index] = read;
				index++;
			}
			
			/* stop reading is DROP_SIZE is reached */
			if(msgBytes != null && msgBytes.length + index >= DROP_SIZE) {
				reading = false;
			}
			
			/* read next char from stream */
			read = (byte) input.read();
		}
		
		if(msgBytes == null){
			tmp = new byte[index];
			System.arraycopy(bufferBytes, 0, tmp, 0, index);
		} else {
			tmp = new byte[msgBytes.length + index];
			System.arraycopy(msgBytes, 0, tmp, 0, msgBytes.length);
			System.arraycopy(bufferBytes, 0, tmp, msgBytes.length, index);
		}
		
		msgBytes = tmp;
		
		/* build final String */
		TextMessage msg = new TextMessage(msgBytes);
		logger.info("Receive message:\t '" + msg.getMsg() + "'");
		return msg;
    }
}
