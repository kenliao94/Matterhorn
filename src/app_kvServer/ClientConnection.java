package app_kvServer;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.*;

import common.messages.KVMessage.StatusType;
import common.messages.TextMessage;


/**
 * Represents a connection end point for a particular client that is 
 * connected to the server. This class is responsible for message reception 
 * and sending. 
 * The class also implements the echo functionality. Thus whenever a message 
 * is received it is going to be echoed back to the client.
 */
public class ClientConnection implements Runnable {

	private static Logger logger = Logger.getRootLogger();
	
	private boolean isOpen;
	private static final int BUFFER_SIZE = 1024;
	private static final int DROP_SIZE = 128 * BUFFER_SIZE;
	
	private Socket clientSocket;
	private InputStream input;
	private OutputStream output;
	
	private KVServer kvServer;
	
	/**
	 * Constructs a new CientConnection object for a given TCP socket.
	 * @param clientSocket the Socket object for the client connection.
	 */
	public ClientConnection(Socket clientSocket, KVServer kvServer) {
		this.clientSocket = clientSocket;
		this.kvServer = kvServer;
		this.isOpen = true;
	}
	
	/**
	 * Initializes and starts the client connection. 
	 * Loops until the connection is closed or aborted by the client.
	 */
	public void run() {
		try {
			output = clientSocket.getOutputStream();
			input = clientSocket.getInputStream();
		
//			sendMessage(new TextMessage(
//					"Connection to MSRG Echo server established: " 
//					+ clientSocket.getLocalAddress() + " / "
//					+ clientSocket.getLocalPort()));
			
			while(isOpen) {
				try {
					TextMessage latestMsg = receiveMessage();
					if (latestMsg.getMsg().trim().length() == 0) {
						throw new IOException();
					}
					StatusType operation = latestMsg.getStatus();
					
					String key = null;
					String value = null;
					StatusType status = StatusType.PUT_SUCCESS;
					switch (operation) {
						case PUT:
							key = latestMsg.getKey();
							value = latestMsg.getValue();
							if (key.isEmpty() || key.contains(" ") || key.length() > 20 || value.length() > 120000) {
								logger.error("Error! Unable to PUT due to invalid key or value!");
								status = StatusType.PUT_ERROR;
								break;
							}
							try {
								if (kvServer.inStorage(key))
									status = StatusType.PUT_UPDATE;
								kvServer.putKV(key, value);
							} catch (Exception e) {
								logger.error("Error! Unable to PUT key-value pair!", e);
								status = StatusType.PUT_ERROR;
							}
							break;
						case GET:
							key = latestMsg.getKey();
							if (key.isEmpty() || key.contains(" ") || key.length() > 20) {
								logger.error("Error! Unable to GET due to invalid key");
								status = StatusType.GET_ERROR;
								break;
							}
							try {
								value = kvServer.getKV(latestMsg.getKey());
								if (value == null)
									status = StatusType.GET_ERROR;
								else
									status = StatusType.GET_SUCCESS;
							} catch (Exception e) {
								logger.error("Error! Unable to GET key-value pair!", e);
								status = StatusType.GET_ERROR;
							}
							break;
						case DELETE:
							key = latestMsg.getKey();
							value = "";
							try {
								kvServer.deleteKV(key);
								status = StatusType.DELETE_SUCCESS;
							} catch (Exception e) {
								logger.error("Error! Unable to DELETE key-value pair!", e);
								status = StatusType.DELETE_ERROR;
							}
							break;
						default:
							break;
					}
					TextMessage resultMsg = new TextMessage(status, key, value);
					
					sendMessage(resultMsg);
					
				/* connection either terminated by the client or lost due to 
				 * network problems*/	
				} catch (IOException ioe) {
					logger.error("Error! Connection lost!");
					isOpen = false;
				}				
			}
			
		} catch (IOException ioe) {
			logger.error("Error! Connection could not be established!", ioe);
			
		} finally {
			
			try {
				if (clientSocket != null) {
					input.close();
					output.close();
					clientSocket.close();
				}
			} catch (IOException ioe) {
				logger.error("Error! Unable to tear down connection!", ioe);
			}
		}
	}
	
	/**
	 * Method sends a TextMessage using this socket.
	 * @param msg the message that is to be sent.
	 * @throws IOException some I/O error regarding the output stream 
	 */
	public void sendMessage(TextMessage msg) throws IOException {
		byte[] msgBytes = msg.getMsgBytes();
		output.write(msgBytes, 0, msgBytes.length);
		output.flush();
		logger.info("SEND \t<" 
				+ clientSocket.getInetAddress().getHostAddress() + ":" 
				+ clientSocket.getPort() + ">: '" 
				+ msg.getMsg() +"'");
    }
	
	
	private TextMessage receiveMessage() throws IOException {
		
		int index = 0;
		byte[] msgBytes = null, tmp = null;
		byte[] bufferBytes = new byte[BUFFER_SIZE];
		
		/* read first char from stream */
		byte read = (byte) input.read();	
		boolean reading = true;
		
//		logger.info("First Char: " + read);
//		Check if stream is closed (read returns -1)
//		if (read == -1){
//			TextMessage msg = new TextMessage("");
//			return msg;
//		}

		while(/*read != 13  && */ read != 10 && read !=-1 && reading) {/* CR, LF, error */
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
			
			/* only read valid characters, i.e. letters and constants */
			bufferBytes[index] = read;
			index++;
			
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
		TextMessage msg = new TextMessage(msgBytes);
		logger.info("RECEIVE \t<" 
				+ clientSocket.getInetAddress().getHostAddress() + ":" 
				+ clientSocket.getPort() + ">: '" 
				+ msg.getMsg().trim() + "'");
		return msg;
    }
	

	
}
