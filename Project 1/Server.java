import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server extends Thread
{
	private NodeInfo localNodeInfo;
	private boolean connectionStatus = false;
	private ServerSocket ss = null;
	private ArrayList<ClientHandler> allClients = new ArrayList<>(); // All created clients by the server

	public Server(NodeInfo localNodeInfo) {
		this.localNodeInfo = localNodeInfo;
	}
	
	public boolean isServerUp() {
		return connectionStatus;
	}

	public ServerSocket getServerSocket() {
		return ss;
	}

	public void closeServer() {
		connectionStatus = false;

		for (int clientIndx = 0; clientIndx < allClients.size(); ++clientIndx) {
			allClients.get(clientIndx).closeClientHandler();
		}

		try {
			if (ss != null) ss.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("-------------- Closed the server --------------");
	}

	@Override
	public void run()
	{
		try {
			ss = new ServerSocket(localNodeInfo.getPort());
			connectionStatus = true;
			// Running infinite loop for getting client request
			while (connectionStatus == true)
			{
				Socket s = null;
				try
				{
					// Socket object to receive incoming client requests
					s = ss.accept();
										
					// Obtaining input and output streams
					ObjectOutputStream outSteam = new ObjectOutputStream(s.getOutputStream());
					ObjectInputStream inStream = new ObjectInputStream(s.getInputStream());
					
					// Calling new thread for this client
					ClientHandler newClient = new ClientHandler(s, inStream, outSteam);
					allClients.add(newClient);
					newClient.start();
				}
				catch (Exception e){
					if (connectionStatus == true) {
						e.printStackTrace();
					}
				}
			}

		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

class ClientHandler extends Thread
{
	final Socket s;
	final ObjectOutputStream outStream;
	final ObjectInputStream inStream;
	
	public ClientHandler(Socket s, ObjectInputStream inStream, ObjectOutputStream outStream)
	{
		this.s = s;
		this.outStream = outStream;
		this.inStream = inStream;
	}

	public Message readData() {
		Message receivedMsg = null;
		try {
			Object receivedObject = inStream.readObject();
			if (receivedObject != null) {
				receivedMsg = (Message) receivedObject;
			}
		} catch (Exception e) {
			// Client closed the connection
		}
		return receivedMsg;
	}

	public void closeClientHandler() {
		try
		{
			// Closing resources
			if (inStream != null) inStream.close();
			if (outStream != null) outStream.close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		while (true)
		{
			Message receivedMsg = readData();
			
			if (receivedMsg == null) {
				break;
			}

			// If received message tells to terminate
			if (receivedMsg.getMsgType().equals(ProtocolConfig.terminationMsgType)) {
				MAP.propagateTerminationStatus();
				break;
			}

			// If received vector clock message
			if (receivedMsg.getMsgType().equals(ProtocolConfig.vectorClockMsgType)) {
				MAP.setClockWhenReceive(receivedMsg);
				MAP.makeNodeActive();
			}

			// If received instruction to reset the snapshot values
			if (receivedMsg.getMsgType().equals(ProtocolConfig.resetSnapshotType)) {
				MAP.resetNeighborsForSnapshot();
			}

			// If need to pass done reset message to the coordinator/root
			if (receivedMsg.getMsgType().equals(ProtocolConfig.doneResetType)) {
				MAP.forwardMsgToRoot(receivedMsg);
			}

			// If need to pass local state message to the coordinator/root
			if (receivedMsg.getMsgType().equals(ProtocolConfig.localStateType)) {
				MAP.forwardMsgToRoot(receivedMsg);
			}

			// If received a marker message
			if (receivedMsg.getMsgType().equals(ProtocolConfig.markerMsgType)) {
				MAP.sendMarkerToOutgoingChannel();
			}
		}
	}
}
