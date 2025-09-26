import java.io.*;
import java.net.*;

public class Client extends Thread
{
	private boolean connectionStatus = false;
	private Socket clientSocket = null;
	private ObjectOutputStream outStream = null;
	private ObjectInputStream inStream = null;

	private String serverHostName;
	private int serverPort;

	public Client(String serverHostName, int serverPort) {
		this.serverHostName = serverHostName;
		this.serverPort = serverPort;
	}

	@Override
	public void run() {
		connectToServer(serverHostName, serverPort);
	}

	public boolean getConnectionStatus() {
		return connectionStatus;
	}

    public void connectToServer(String hostName, int port) {
		for (int tries = 0; tries < ProtocolConfig.maxRetryCount && connectionStatus == false; ++tries) {
			try
			{
				// Establish the connection with hostName at port
				clientSocket = new Socket(hostName, port);
				connectionStatus = true;

				// Obtaining input and output streams
				outStream = new ObjectOutputStream(clientSocket.getOutputStream());
				inStream = new ObjectInputStream(clientSocket.getInputStream());
			} catch (ConnectException e) {
                // Connection failed. Retry after delaying for retryDelayMS
                try {
                    Thread.sleep(ProtocolConfig.retryDelayMS+5);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
                e.printStackTrace();
                break;  // Stop retrying if an IOException occurs
            }
		}

		if (connectionStatus == false) {
			System.out.println("Failed to connect after " + ProtocolConfig.maxRetryCount + " tries with: " + 
								hostName + " at port: " + port);
		}
	}

	public synchronized void clientSend(Message sendMsg) {
		try {
			outStream.writeObject(sendMsg);
			outStream.flush();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void closeConnection() {
		// Closing connection
		try {
			// Closing resources
			connectionStatus = false;
			if (inStream != null) inStream.close();
			if (outStream != null) outStream.close();
			if (clientSocket != null) clientSocket.close();
		} catch (Exception e) {
			System.out.println("Couldn't close connection!");
		}
	}
}
