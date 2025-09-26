import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MAP {
    private static String domain;
    private static String configFileName;

    private static int totalNodes;
    private static int minPerActive;
    private static int maxPerActive;
    private static int minSendDelay;
    private static int snapshotDelay;
    private static int maxNumber;

    private static boolean activeStatus;
    private static int terminationDetected;

    private static int totalMsgSent = 0; // To keep track of how many message this node has sent
    private static int totalMsgReceived = 0; // To keep track of how many message this node has received

    private static String localHostName;
    private static int localNodeId;
    private static NodeInfo localNodeInfo;
    private static Server localServerThread;
    private static ArrayList<Integer> localNodeNeighbors;
    private static ArrayList<Client> localNeighborSockets = new ArrayList<>();
    private static ArrayList<Integer> localVectorClock = new ArrayList<>();

    ArrayList<NodeInfo> allNodes = new ArrayList<>(); // Info about the neighbors
    ArrayList<ArrayList<Integer>> allNeighborInfo = new ArrayList<>(); // Use if needed in the future

    // ------ Needed for snapshot -------
    private static int snapShotNumber;
    private static int nodeColor; // 0 = Blue, 1 = Red
    private static String snapshotFileName;
    private static ArrayList<Message> inTransitMsg = new ArrayList<>(); // Stores messages that are in transit
    private static ArrayList<Integer> parentOfNode = new ArrayList<>(); // Stores parent node id of i'th node
    private static ArrayList<Integer> doneResetting = new ArrayList<>(); // List of nodes who have resetted
    private static ArrayList<Integer> localSnapshotState = new ArrayList<>(); // Local state of i'th node
    private static ArrayList<Integer> activeNodes = new ArrayList<>(); // List of nodes who are active
    private static ArrayList<ArrayList<Integer>> childOfNode = new ArrayList<>(); // List of childs of i'th node
    private static ArrayList<ArrayList<Integer>> localStateOfAllNodes = new ArrayList<>(); // Local state of node-i after snapshot


    public MAP(String configFileName, String domain) {
        MAP.domain = domain;
        MAP.configFileName = configFileName;
        localHostName = getLocalHostName();

        nodeColor = -1; // Indicates node color is invalid (i.e. not ready for snapshot)
        terminationDetected = 0; // At the start the protocol has not detected termination

        parseConfiguration(configFileName); // Read the config file
        initVectorClk(); // Sets up the initial vector clock for this node
        initSpanningTree(ProtocolConfig.coordinatorNodeId); // Forms the spanning tree of the network 

        setupConnections(); // Turns on server and connect with neighbors
        initActiveStatus(); // Sets whether the node will be active or not at the beginning
    }

    /* At the start of the protocol each node is active with 50% probability (coordinator node is active at the start) */
    private void initActiveStatus() {
        activeStatus = false;
        Random random = new Random();
        double rand = random.nextDouble();  // Generate a random value between 0 and 1

        // For all other nodes except for coordinator has 50% probability of becoming active
        if (rand >= 0.5 || localNodeId == ProtocolConfig.coordinatorNodeId) {
            activeStatus = true;
        }
    }

    public static void setActiveStatus(boolean status) {
        activeStatus = status;
    }

    public static boolean getActiveStatus() {
        return activeStatus;
    }

    public static synchronized void makeNodeActive() {
        if (activeStatus == false && totalMsgSent < maxNumber) {
            activeStatus = true;
        }
    }

    /* Initializing local vector clock with zero */
    private void initVectorClk() {
        for (int i = 0; i < totalNodes; ++i) {
            localVectorClock.add(0);
        }
    }
    
    public static ArrayList<Integer> getLocalVectorClock() {
        return localVectorClock;
    }

    /* Updates local clock value when receiving a message */
    public static synchronized void setClockWhenReceive(Message receivedMsg) {
        ArrayList<Integer> msgVectorClock = receivedMsg.getVectorClkMsg();

        // Take maximum of both message
        for (int i = 0; i < msgVectorClock.size(); ++i) {
            localVectorClock.set(i, Math.max(localVectorClock.get(i), msgVectorClock.get(i)));
        }
        // Increment the clock of current node by 1
        localVectorClock.set(localNodeId, localVectorClock.get(localNodeId)+1);

        // If a Red colored node receives a Blue application message record it as in-transit message
        if (receivedMsg.getMsgColor() == 0 && nodeColor == 1) {
            receivedMsg.setMsgType(ProtocolConfig.inTransitMsgType);
            forwardMsgToRoot(receivedMsg);
        }

        totalMsgReceived++;
    }

    public static synchronized void setClockWhenSend() {
        localVectorClock.set(localNodeId, localVectorClock.get(localNodeId)+1);

        // Increment the totalMsgSent to indicate that one more message is sent
        totalMsgSent++;
    }

    private String getLocalHostName() {
        try {
            // Get the InetAddress object representing the local machine
            InetAddress localhost = InetAddress.getLocalHost();

            // Get the hostname
            String hostname = localhost.getHostName();
            System.out.println("Server hostname: " + hostname); // dc02.utdallas.edu
            return hostname;
        } catch (UnknownHostException e) {
            System.err.println("Unable to determine the hostname: " + e.getMessage());
            return "Unable to determine the hostname!"; // if the host name is not found
        }
    }

    /* Halt for waitTime mili seconds */
    public static void waitFor(int waitTime) {
        try{
            Thread.sleep(waitTime);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /* Reads the config file and sets up the node */
    private void parseConfiguration(String configFileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(configFileName))) {
            String line;
            ArrayList<String> processedLine = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                line = removeComments(line); // Removing the contents after '#' character
                if (line.isEmpty()) continue;
                processedLine.add(line);
            }

            System.out.println("Total lines after processing: " + processedLine.size());

            // Printing the parsed conents after removing comments and extra spaces
            System.out.println("----------Configuration-----------");
            for (int line_no = 0; line_no < processedLine.size(); ++line_no) {
                System.out.println(processedLine.get(line_no));
            }
            
            // Integrating the config file with the MAP class

            // Assigning constant values
            String[] first_line = processedLine.get(0).split(" ");
            totalNodes = Integer.parseInt(first_line[0]);
            minPerActive = Integer.parseInt(first_line[1]);
            maxPerActive = Integer.parseInt(first_line[2]);
            minSendDelay = Integer.parseInt(first_line[3]);
            snapshotDelay = Integer.parseInt(first_line[4]);
            maxNumber = Integer.parseInt(first_line[5]);

            // Getting ID, Hostname, and Ports of all the nodes
            for (int line_no = 1; line_no < 1 + totalNodes; ++line_no) {
                String[] curLine = processedLine.get(line_no).split(" ");
                int nodeId = Integer.parseInt(curLine[0]);
                String hostName = curLine[1];
                int port = Integer.parseInt(curLine[2]);
                NodeInfo curNode = new NodeInfo(nodeId, hostName, port);
                allNodes.add(curNode);

                // config hostName + domain = dc02 + .utdallas.edu
                if (localHostName.equals(hostName + domain)) { 
                    localNodeId = nodeId; // This is the nodeId/serial of the current machine
                }
            }

            // Adding neighbors
            for (int line_no = 1 + totalNodes, curNodeID = 0; line_no < processedLine.size(); ++line_no, ++curNodeID) {
                String[] curLine = processedLine.get(line_no).split(" ");
                ArrayList<Integer> curNeighbors = new ArrayList<>();
                for (int neighborNo = 0; neighborNo < curLine.length; ++neighborNo) {
                    curNeighbors.add(Integer.parseInt(curLine[neighborNo]));
                }
                allNodes.get(curNodeID).AddNeighbors(curNeighbors);
                allNeighborInfo.add(curNeighbors);
            }

            // Display the current machine
            localNodeInfo = allNodes.get(localNodeId);
            System.out.print("Current Machine -> ");
            localNodeInfo.showNodeInfo();
            localNodeNeighbors = localNodeInfo.getAllNeighbors();

            // Set the snapshot file name and remove the previous file, if any
            cleanPreviousFile(); 
            System.out.println("----------End Configuration-----------\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Remove the contents after '#' characters and also removes extra white space */
    private String removeComments(String line) {
        int commentPos = line.indexOf('#');
        if (commentPos != -1) {
            line = line.substring(0, commentPos);
        }
        line.trim();
        return line;
    }

    private void setupConnections() {
        startServer();
        startNeighborhoodConnection();
    }

    /* Starts itself */
    public void startServer() {
        localServerThread = new Server(localNodeInfo);
        localServerThread.start();
        while(localServerThread.isServerUp() == false) {
            // Wait for some time before checking the status again
            waitFor(ProtocolConfig.retryDelayMS);
        }
    }

    /* Establish connections with neighbors */
    public void startNeighborhoodConnection() {
        ArrayList<Integer> neighbors = localNodeNeighbors;

        for (int i = 0; i < neighbors.size(); ++i) {
            int curNeighborId = neighbors.get(i);
            NodeInfo curNeighborNodeInfo = allNodes.get(curNeighborId);

            Client neighborClientThread = new Client(curNeighborNodeInfo.getHostName(), curNeighborNodeInfo.getPort());
            neighborClientThread.start();
            while(neighborClientThread.getConnectionStatus() == false) {
                // Wait for some time before checking the status again
                waitFor(ProtocolConfig.retryDelayMS);
            }
            localNeighborSockets.add(neighborClientThread); // Storing connected sockets
        }
    }

    /* Closes all neighboring connections and turns off local server */
    public static void closeAllConnections() {
        // Closing the neighboring connections
        for (int i = 0; i < localNodeNeighbors.size(); ++i) {
            System.out.println(" ! Closing Connection ! " + localNodeId + " ---> " + localNodeNeighbors.get(i));
            Client neighborClientThread = localNeighborSockets.get(i);
            neighborClientThread.closeConnection(); 
        }

        // Closing the server
        localServerThread.closeServer();
    }

    public void startMAPProtocol() {
        while(terminationDetected == 0) { // Run until termination is detected
            if (getActiveStatus() == false) {
                waitFor(ProtocolConfig.retryDelayMS);
                continue;
            }

            // Getting a random number in range of [minPerActive, maxPerActive] -> Number of messages to send
            int numOfMsgToSend = ThreadLocalRandom.current().nextInt(minPerActive, maxPerActive + 1);

            for (int cnt = 0; cnt < numOfMsgToSend; ++cnt) {
                // Pick a neighbor randomly and send the message
                int randNeighborIndx = ThreadLocalRandom.current().nextInt(0, localNodeNeighbors.size());
                int randNeighborId = localNodeNeighbors.get(randNeighborIndx);

                // Getting the channel through which the neighbor is connected
                Client curNeighborSocket = localNeighborSockets.get(randNeighborIndx);

                // The vector clock should be incremented before piggyback
                setClockWhenSend();
                // Prepare the message to be sent
                Message curMsg = new Message(localNodeId, randNeighborId);
                curMsg.setMsgType(ProtocolConfig.vectorClockMsgType);
                curMsg.setVectorClockMsg(localVectorClock);
                curMsg.setMsgColor(nodeColor);
                curNeighborSocket.clientSend(curMsg);

                // Wait for minSendDelay before sending the next message
                waitFor(minSendDelay);
            }

            setActiveStatus(false);
        }

        System.out.println("Send count: " + totalMsgSent + " Receive count: " + totalMsgReceived);

    }


    // ---------------------------------Methods for snapshot--------------------------------------------

    /* Builds the spanning tree for converge casting local states to the root */
    private void initSpanningTree(int rootNodeId) {
        // Initializing parent and child of all nodes as -1 indicating it has no parent as of now
        for (int i = 0; i < totalNodes; ++i) {
            parentOfNode.add(-1);
            childOfNode.add(new ArrayList<>());
        }

        // Will use BFS to set up the parents of each node
        boolean[] visited = new boolean[totalNodes];
        Queue<Integer> queue = new LinkedList<>();

        queue.offer(rootNodeId);
        visited[rootNodeId] = true;

        while (!queue.isEmpty()) {
            int currentNodeId = queue.poll();

            for (int neighborNodeId : allNeighborInfo.get(currentNodeId)) {
                if (!visited[neighborNodeId]) {
                    queue.offer(neighborNodeId);
                    visited[neighborNodeId] = true;
                    parentOfNode.set(neighborNodeId, currentNodeId); // Set the parent of the neighbor
                    childOfNode.get(currentNodeId).add(neighborNodeId);
                }
            }
        }
    }


    public static Client getNeighborSocket(int neighborNodeId) {
        for (int neighborIndx = 0; neighborIndx < localNodeNeighbors.size(); ++neighborIndx) {
            int curNeighborNodeId = localNodeNeighbors.get(neighborIndx);
            if (curNeighborNodeId != neighborNodeId) continue;
            return localNeighborSockets.get(neighborIndx);
        }
        return null;
    }

    /* Resets itself and tell all it's child nodes to reset */
    public static void resetNeighborsForSnapshot() {
        // Wait until all neighboring channels are active
        while(localNeighborSockets.size() < localNodeNeighbors.size()) {
            waitFor(ProtocolConfig.retryDelayMS);
        }

        nodeColor = 0; // At the start node's color is blue

        for (int childIndx = 0; childIndx < childOfNode.get(localNodeId).size(); ++childIndx) {
            int childNodeId = childOfNode.get(localNodeId).get(childIndx);

            Message resetMsg = new Message(localNodeId, childNodeId);
            resetMsg.setMsgType(ProtocolConfig.resetSnapshotType);

            Client childSocket = getNeighborSocket(childNodeId);
            childSocket.clientSend(resetMsg);
        }
        // System.out.println("Updated Node Color: " + nodeColor + " Reset Done!");

        Message senderInfo = new Message(localNodeId, parentOfNode.get(localNodeId));
        senderInfo.setMsgType(ProtocolConfig.doneResetType);
        forwardMsgToRoot(senderInfo);
    }

    /* Sets termination to 1 and informs all of it's child */
    public static void propagateTerminationStatus() {
        terminationDetected = 1;

        for (int childIndx = 0; childIndx < childOfNode.get(localNodeId).size(); ++childIndx) {
            int childNodeId = childOfNode.get(localNodeId).get(childIndx);

            Message terminationMsg = new Message(localNodeId, childNodeId);
            terminationMsg.setMsgType(ProtocolConfig.terminationMsgType);

            Client childSocket = getNeighborSocket(childNodeId);
            childSocket.clientSend(terminationMsg);
        }
        
        System.out.println("***Termination Detected!***");
        closeAllConnections();
    }

    /* Propagates the message to it's parent node if it is not the root node (i.e. coordinator) */
    public synchronized static void forwardMsgToRoot(Message senderInfo) {
        // If this is the root of the tree then stop here
        if (localNodeId == ProtocolConfig.coordinatorNodeId) {
            // Signal is about nodes being reseted for a new snapshot
            if (senderInfo.getMsgType().equals(ProtocolConfig.doneResetType)) {
                doneResetting.add(senderInfo.getSenderNodeId());
            }

            // Local state of other nodes
            if (senderInfo.getMsgType().equals(ProtocolConfig.localStateType)) {
                localStateOfAllNodes.set(senderInfo.getSenderNodeId(), senderInfo.getVectorClkMsg());
                if (senderInfo.getNodeActivityStatus() == 1) {
                    activeNodes.add(senderInfo.getSenderNodeId());
                }
            }

            // If in-transit message
            if (senderInfo.getMsgType().equals(ProtocolConfig.inTransitMsgType)) {
                inTransitMsg.add(senderInfo);
            }
            return;
        }

        int parofCurNode = parentOfNode.get(localNodeId);
        // Here, I do not modify the senderNodeId so that it can be known from where the message was generated
        senderInfo.setReceiverNodeId(parofCurNode);

        getNeighborSocket(parofCurNode).clientSend(senderInfo);
    }

    /* Sends marker message to all outgoing channels when changing color from Blue to Red */
    public synchronized static void sendMarkerToOutgoingChannel() {
        if (nodeColor == 1) {
            // The node color is already Red, no need to send marker message again
            return;
        }

        // Change color from Blue to Red
        nodeColor = 1;

        // Record local-state
        recordLocalState();

        // Forward this local state to the root
        Message localStateMsg = new Message(localNodeId, parentOfNode.get(localNodeId));
        localStateMsg.setMsgType(ProtocolConfig.localStateType);
        localStateMsg.setVectorClockMsg(localSnapshotState);
        localStateMsg.setNodeActivityStatus((activeStatus == true) ? 1 : 0);
        forwardMsgToRoot(localStateMsg);

        for (int neighborIndx = 0; neighborIndx < localNodeNeighbors.size(); ++neighborIndx) {
            int neighborNodeId = localNodeNeighbors.get(neighborIndx);

            // Send marker message to the neighbor after obtaining it's socket
            Message markerMsg = new Message(localNodeId, neighborNodeId);
            markerMsg.setMsgType(ProtocolConfig.markerMsgType);
            localNeighborSockets.get(neighborIndx).clientSend(markerMsg);
        }
    }

    private void cleanPreviousFile() {
        String fileName = MAP.configFileName;

        // Removing extension from the config filename
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            fileName = fileName.substring(0, lastDotIndex);
        }

        // Storing the local state in <config_name>-<node_id>.out
        fileName = fileName + "-" + localNodeId + ".out";

        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }

        MAP.snapshotFileName = fileName;
    }

    private static synchronized void recordLocalState() {
        localSnapshotState = new ArrayList<>(localVectorClock);
        System.out.println("Local state : " + localVectorClock);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(snapshotFileName, true));
            //writer.write(snapShotNumber + ": [");
            int afterStart = 0;
            for (Integer item : localSnapshotState) {
                if (afterStart == 1) {
                    //writer.write(", ");
                    writer.write(" ");
                }
                writer.write(item.toString());
                afterStart = 1;
            }
            //writer.write("]\n");
            writer.newLine();
            writer.close();
        } catch (Exception e) {
            System.out.println("Couldn't write into file");
            e.printStackTrace();
        }
    }

    /* Returns true if the recorded local state is consistent */
    private boolean checkConsistencyOfSnapshot(ArrayList<ArrayList<Integer>> localStates) {
        for (int i = 0; i < totalNodes; ++i) {
            Integer curMx = 0;
            for (int j = 0; j < totalNodes; ++j) {
                curMx = Math.max(curMx, localStates.get(j).get(i));
            }
            if (localStates.get(i).get(i) < curMx) {
                return false;
            }
        }

        return true;
    }

    public void startSnapshotProtocol() {
        // This node can not start the snapshot protocol if it's not the coordinator
        if (localNodeId != ProtocolConfig.coordinatorNodeId) {
            return;
        }

        snapShotNumber = 1; // Count's the number of snapshot
        while(true) {
            // Clear the recorded values of previous snapshot (if any)
            doneResetting.clear();
            localStateOfAllNodes.clear();
            activeNodes.clear();
            inTransitMsg.clear();

            System.out.println("Snapshot number: " + snapShotNumber);

            // Recursively reset all the nodes
            resetNeighborsForSnapshot();
            while (doneResetting.size() < totalNodes) { // Checks if all nodes done resetting
                waitFor(ProtocolConfig.retryDelayMS);
            }
            //System.out.println("Final reseted nodes: " + doneResetting);

            // Initialize localStateofAllNodes
            for (int i = 0; i < totalNodes; ++i) {
                localStateOfAllNodes.add(new ArrayList<>());
            }

            // Starts the protocol
            sendMarkerToOutgoingChannel();

            while (true) { // Monitor if received local state from all nodes
                int emptyRowIndx;
                for (emptyRowIndx = 0; emptyRowIndx < totalNodes; ++emptyRowIndx) {
                    if (localStateOfAllNodes.get(emptyRowIndx).isEmpty()) {
                        break;
                    }
                }
                if (emptyRowIndx == totalNodes) break; // None of the local state is empty
                waitFor(ProtocolConfig.retryDelayMS);
            }
            
            System.out.println("Local state of all nodes: " + localStateOfAllNodes);
        
            // Displaying the in-transit messages, if any
            // System.out.println("Total in-transit message: " + inTransitMsg.size());
            // for (int i = 0; i < inTransitMsg.size(); ++i) {
            //     Message curMsg = inTransitMsg.get(i);
            //     System.out.println(curMsg.getSenderNodeId() + " " + curMsg.getReceiverNodeId() + " : "
            //     + curMsg.getVectorClkMsg());; 
            // }

            boolean isConsistent = checkConsistencyOfSnapshot(localStateOfAllNodes);
            System.out.println("------- Consistency: " + isConsistent + " -------------- ");

            //System.out.println("Active nodes: " + activeNodes);

            // The protocol ends when all nodes are passive and there are no in-transit messages
            if (activeNodes.size() == 0 && inTransitMsg.size() == 0) {
                propagateTerminationStatus();
                System.out.println("<-------------- End of MAP protocol -------------->");
                break; // End of protocol
            }

            // Wait for snapshotDelay before taking the next snapshot
            waitFor(snapshotDelay);
            snapShotNumber++;
        }
    }
}