import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class MutualExclusion {
    private static String domain;

    private static int totalNodes;
    private static int meanInterReqDelay;
    private static int meanCSExecutionTime;
    private static int totalRequest;

    private static String localHostName;
    private static int localNodeId;
    private static NodeInfo localNodeInfo;
    private static Server localServerThread;
    private static ArrayList<Integer> localNodeNeighbors;
    private static int localScalarClock;
    private static ArrayList<Integer> localVectorClock = new ArrayList<>(); // For verifying CS

    private static int totalMsgSent = 0;
    private static int totalMsgReceive = 0;
    private static Boolean nodeStatus = false;

    private static ArrayList<Client> localNeighborClients = new ArrayList<>(); // Info about neighboring sockets
    ArrayList<NodeInfo> allNodes = new ArrayList<>(); // Info about all nodes

    // -----------------Information for CS-----------------
    private static int reqNo = 0;
    private static int curReqLogicalTime;
    private static Long csEnteringTimestamp;
    private static Long csLeavingTimestamp;
    private static  ArrayList<Integer> csEnteringVectorClock;
    private static  ArrayList<Integer> csLeavingVectorClock;

    private static Boolean insideCS;
    private static ArrayList<Integer> localNodesKeys = new ArrayList<>(); // N sized matrix: 1-key, 0-missing key
    private static ArrayList<Message> deferedReplyMsg = new ArrayList<>(); // If can't give key immediately
    private static ArrayList<Integer> allSentMsgCnt = new ArrayList<>(); // Count's # of msg sent by each node
    private static ArrayList<Message> allReqDone = new ArrayList<>(); // Keeps track of nodes whose requests are fulfilled
    private static ArrayList<Long> allReqTimestamps = new ArrayList<>(); // The timestamp when the requests were generated
    private static ArrayList<ArrayList<Long>> allCSTimestamps = new ArrayList<>(); // Pair of (start, end) time of CS
    private static ArrayList<ArrayList<ArrayList<Integer>>> allCSVectorClocks = new ArrayList<>(); // Pair of (start, end) time of CS

    public MutualExclusion(String configFileName, String domain) {
        MutualExclusion.domain = domain;
        localHostName = getLocalHostName();

        parseConfiguration(configFileName); // Read the config file
        
        localScalarClock = 0; // Initial scalar clock value
        initVectorClk(); // Initial vector clock value

        // This node contains all the keys of all neighboring nodes that have node id greater than this node
        for (int curKey = 0; curKey < localNodeId; ++curKey) {
            localNodesKeys.add(0);
        }
        for (int curKey = localNodeId; curKey < totalNodes; ++curKey) {
            localNodesKeys.add(1);
        }

        insideCS = false;
        setCurReqLogicalTime(-1);

        setupConnections(); // Turns on server and connect with neighbors
        nodeStatus = true;
    }

    public static synchronized Boolean getNodeStatus() {
        return MutualExclusion.nodeStatus;
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

    /* Get Current Time in miliseconds */
    public static long getCurrentTimeInMS() {
        return System.currentTimeMillis();
    }

    /* Halt for waitTime - mili seconds */
    public static void waitFor(long waitTime) {
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

            // Printing the parsed contents after removing comments and extra spaces
            System.out.println("----------Configuration-----------");
            for (int line_no = 0; line_no < processedLine.size(); ++line_no) {
                System.out.println(processedLine.get(line_no));
            }
            
            // Integrating the config file with the MutualExclusion class

            // Assigning constant values
            String[] first_line = processedLine.get(0).split(" ");
            totalNodes = Integer.parseInt(first_line[0]);
            meanInterReqDelay = Integer.parseInt(first_line[1]);
            meanCSExecutionTime = Integer.parseInt(first_line[2]);
            totalRequest = Integer.parseInt(first_line[3]);

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
            for (int curNodeID = 0; curNodeID < totalNodes; ++curNodeID) {
                ArrayList<Integer> curNeighbors = new ArrayList<>();
                for (int neighborNodeId = 0; neighborNodeId < totalNodes; ++neighborNodeId) {
                    if (neighborNodeId != curNodeID) { // all nodes are neighbor except the node itself
                        curNeighbors.add(neighborNodeId);
                    }
                }
                allNodes.get(curNodeID).AddNeighbors(curNeighbors);
            }

            // Display the current machine
            localNodeInfo = allNodes.get(localNodeId);
            System.out.print("Current Machine -> ");
            localNodeInfo.showNodeInfo();
            localNodeNeighbors = localNodeInfo.getAllNeighbors();
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
            localNeighborClients.add(neighborClientThread); // Storing connected sockets
        }
    }

    /* Closes all neighboring connections and turns off local server */
    public static synchronized void closeAllConnections() {
        // Display some info
        System.out.println("Last Scalar Clock Value: " + localScalarClock);
        System.out.println("Total Sent: " + totalMsgSent + ", Total Received: " + totalMsgReceive);

        // Closing the neighboring connections
        for (int i = 0; i < localNodeNeighbors.size(); ++i) {
            // System.out.println(" ! Closing Connection ! " + localNodeId + " ---> " + localNodeNeighbors.get(i));
            Client neighborClientThread = localNeighborClients.get(i);
            neighborClientThread.closeConnection(); 
        }

        // Closing the server
        localServerThread.closeServer();
    }

    /* Initializing local vector clock with zero */
    private synchronized void initVectorClk() {
        for (int i = 0; i < totalNodes; ++i) {
            localVectorClock.add(0);
        }
    }
   
    public static synchronized ArrayList<Integer> getLocalVectorClock() {
        return localVectorClock;
    }

    private static synchronized void setCurReqLogicalTime(int curReqLogicalTime) {
        MutualExclusion.curReqLogicalTime = curReqLogicalTime;
    }

    private static synchronized int getCurReqLogicalTime() {
        return MutualExclusion.curReqLogicalTime;
    }

    private static synchronized void setLocalNodeKeys(int curNeighborId, int val) {
        localNodesKeys.set(curNeighborId, val);
    }

    private static synchronized int getLocalNodeKeys(int curNeighborId) {
        return localNodesKeys.get(curNeighborId);
    }

    /* Updates local clock value when receiving a message */
    public static synchronized void setClockWhenReceive(Message receivedMsg) {
        // If this message is only used for experimental purpose then ignore
        if (receivedMsg.getIgnoreThisMsg() == 1) {
            return;
        }

        // Taking maximum of local and received message's clock and then adding 1
        localScalarClock = Math.max(localScalarClock, receivedMsg.getScalarClockValue())+1;
        totalMsgReceive++;

        // --Updating vector clock when receive--
        ArrayList<Integer> msgVectorClock = receivedMsg.getVectorClkValue();
        // Take maximum of both message
        for (int i = 0; i < msgVectorClock.size(); ++i) {
            localVectorClock.set(i, Math.max(localVectorClock.get(i), msgVectorClock.get(i)));
        }
        // Increment the clock of current node by 1
        localVectorClock.set(localNodeId, localVectorClock.get(localNodeId)+1);
    }

    /* Updates scalar clock value when sending a message */
    public static synchronized void setClockWhenSend(Message sendMsg) {
        // If this message is only used for experimental purpose then ignore
        if (sendMsg.getIgnoreThisMsg() == 1) {
            return;
        }

        // Incrementing local clock value while sending a message
        localScalarClock++;
        totalMsgSent++;

        // --Updating vector clock when send--
        localVectorClock.set(localNodeId, localVectorClock.get(localNodeId)+1);
    }

    public static synchronized void sendMessage(Message sendMsg) {
        setClockWhenSend(sendMsg); // update clock
        sendMsg.setScalarClockValue(localScalarClock); // piggyback updated clock
        sendMsg.setVectorClockValue(getLocalVectorClock()); // piggyback updated vector clock
        getNeighborClientThread(sendMsg.getReceiverNodeId()).clientSend(sendMsg);
    }

    public static synchronized void HandleServerRequest(Message receivedMsg) {
        // Update clock upon receiving a message
        setClockWhenReceive(receivedMsg);

        // If another process requested for a key
        if (receivedMsg.getMsgType().equals(ProtocolConfig.sendKeyMsgType)) {
            boolean deferReply = false;

            if (insideCS == true) {
                deferReply = true;
            } else if (getCurReqLogicalTime() != -1) { // If current node already have a request
                if (getCurReqLogicalTime() <= receivedMsg.getReqTimestamp()) {
                    deferReply = true;
                }
            }

            if (deferReply == true) {
                deferedReplyMsg.add(receivedMsg);
            } else {
                GiveRequestedKey(receivedMsg.getSenderNodeId());

                // If this node already have an outstanding request need to ask for the key again
                if (getCurReqLogicalTime() != -1) {
                    RequestForKey(receivedMsg.getSenderNodeId());
                }
            }
        }

        // If received key from another node
        if (receivedMsg.getMsgType().equals(ProtocolConfig.receiveKeyMsgType)) {
            setLocalNodeKeys(receivedMsg.getSenderNodeId(), 1); // Received the missing key
        }

        // If received termination message
        if (receivedMsg.getMsgType().equals(ProtocolConfig.terminationMsgType)) {
            allReqDone.add(receivedMsg);
        }

        // If received message tells to close connections
        if (receivedMsg.getMsgType().equals(ProtocolConfig.closeConnectionMsgType)) {
            closeAllConnections();
        }

        // If received message is number of sent message count
        if (receivedMsg.getMsgType().equals(ProtocolConfig.sendCntMsgType)) {
            if (localNodeId == ProtocolConfig.coordinatorNodeId) {
                allSentMsgCnt.set(receivedMsg.getSenderNodeId(), receivedMsg.getReqTimestamp()); // Storing sent count
            } else {
                Message sendSentCntMSg = new Message(localNodeId, ProtocolConfig.coordinatorNodeId);
                sendSentCntMSg.setMsgType(ProtocolConfig.sendCntMsgType);
                sendSentCntMSg.setReqTimestamp(totalMsgSent); // Storing send count in reqtimestamp
                sendSentCntMSg.setIgnoreThisMsg(1);
                sendMessage(sendSentCntMSg);
            }
        }
    }

    /* Find the thread of neighboring client (for sending messages) */
    private static synchronized Client getNeighborClientThread(int neighborNodeId) {
        Client neighborClientThread = null;
        if (neighborNodeId < localNodeId) {
            neighborClientThread = localNeighborClients.get(neighborNodeId);
        } else {
            neighborClientThread = localNeighborClients.get(neighborNodeId-1);
        }
        return neighborClientThread;
    }

    /* The local node requests for a key to the keyHolderNodeId */
    private static synchronized void RequestForKey(int keyHolderNodeId) {
        // Sending a request message to keyHolderNodeId
        Message requestMsg = new Message(localNodeId, keyHolderNodeId);
        requestMsg.setMsgType(ProtocolConfig.sendKeyMsgType);
        requestMsg.setReqTimestamp(getCurReqLogicalTime());
        
        sendMessage(requestMsg);
    }

    /* Providing key from localNode to the requestedNode */
    private static synchronized void GiveRequestedKey(int requestedNodeId) {
        if (getLocalNodeKeys(requestedNodeId) == 0) {
            System.out.println(" !! !! !! No key " + localNodesKeys + " request-by: " + requestedNodeId);
            return;
        }
        setLocalNodeKeys(requestedNodeId, 0); // Now localNode won't have this key anymore
        Message sendKeyMsg = new Message(localNodeId, requestedNodeId);
        sendKeyMsg.setMsgType(ProtocolConfig.receiveKeyMsgType);

        sendMessage(sendKeyMsg);
    }

    /* Request received for entering CS */
    public synchronized void CSEnter() {
        reqNo++; // To keep track of how many requests has been made
        if (reqNo == 1) {
            System.out.print("Request fulfilled: 0");
        }
        if (reqNo%10 == 0) {
            System.out.print("..." + reqNo);
        }

        allReqTimestamps.add(getCurrentTimeInMS()); // Storing the time when this request was generated
        setCurReqLogicalTime(localScalarClock); // The logical timestamp when the request is generated

        int isMissingKey = 0; // Assume no missing keys at the beginning
        // Request for missing keys
        for (int curNeighborId = 0; curNeighborId < totalNodes; ++curNeighborId) {
            if (getLocalNodeKeys(curNeighborId) == 0) {
                RequestForKey(curNeighborId);
                isMissingKey = 1;
            }
        }

        // Wait till all missing keys are obtained
        while (isMissingKey == 1) {
            waitFor(ProtocolConfig.retryDelayMS);
            isMissingKey = 0;
            for (int curNeighborId = 0; curNeighborId < totalNodes; ++curNeighborId) {
                if (getLocalNodeKeys(curNeighborId) == 0) {
                    isMissingKey = 1;
                }
            }
        }
        insideCS = true;
        csEnteringTimestamp = getCurrentTimeInMS();
        csEnteringVectorClock = new ArrayList<>(getLocalVectorClock());
    }

    private synchronized void CheckIfAllDone() {
        while (allReqDone.size() != totalNodes) {
            waitFor(ProtocolConfig.retryDelayMS);
        }
        System.out.println("All nodes done processing!");
        for (int i = 0; i < totalNodes; ++i) allSentMsgCnt.add(-1);

        for (int curNeighborNodeId = 0; curNeighborNodeId < totalNodes; ++curNeighborNodeId) {
            if (curNeighborNodeId == ProtocolConfig.coordinatorNodeId) {
                allSentMsgCnt.set(curNeighborNodeId, totalMsgSent);
            } else {
                Message reqForSentCnt = new Message(localNodeId, curNeighborNodeId);
                reqForSentCnt.setMsgType(ProtocolConfig.sendCntMsgType);
                reqForSentCnt.setIgnoreThisMsg(1);
                sendMessage(reqForSentCnt);
            }
        }
        
        while (true) {
            int idx;
            for (idx = 0; idx < totalNodes; ++idx) {
                if (allSentMsgCnt.get(idx) == -1) break;
            }
            if (idx == totalNodes) break;
            waitFor(ProtocolConfig.retryDelayMS);
        }

        // Now starts CS Verification process
        CSVerifier csVerifier = new CSVerifier(allReqDone, totalRequest, meanInterReqDelay, meanCSExecutionTime);
        csVerifier.updateTotalMsgSent(allSentMsgCnt);

        System.out.println("\n***   CS Verification: " + csVerifier.startVerifyingCS() + "   ***\n");
        csVerifier.logComplexity();

        // Tell all nodes to close connections
        for (int nodeId = 0; nodeId < totalNodes; ++nodeId) {
            if (nodeId == ProtocolConfig.coordinatorNodeId) {
                continue;
            } else {
                Message closeConnectionMsg = new Message(localNodeId, nodeId);
                closeConnectionMsg.setMsgType(ProtocolConfig.closeConnectionMsgType);
                closeConnectionMsg.setIgnoreThisMsg(1);

                sendMessage(closeConnectionMsg);
            }
        }

        closeAllConnections();
    }

    private synchronized void InitiateTermination() {
        Message terminationMsg = new Message(localNodeId, ProtocolConfig.coordinatorNodeId);
        terminationMsg.setMsgType(ProtocolConfig.terminationMsgType);
        terminationMsg.setAllReqTimestampsInfo(allReqTimestamps);
        terminationMsg.setAllCSTimestampsInfo(allCSTimestamps);
        terminationMsg.setAllCSVectorClocks(allCSVectorClocks);
        terminationMsg.setIgnoreThisMsg(1);

        if (localNodeId == ProtocolConfig.coordinatorNodeId) {
            allReqDone.add(terminationMsg);
            CheckIfAllDone();
        } else {
            sendMessage(terminationMsg);
        }
    }

    /* Done processing CS */
    public synchronized void CSLeave() {
        csLeavingTimestamp = getCurrentTimeInMS();
        csLeavingVectorClock = new ArrayList<>(getLocalVectorClock());
        allCSTimestamps.add(new ArrayList<>(Arrays.asList(csEnteringTimestamp, csLeavingTimestamp)));

        // Adding vector timestamps into allCSVectorClock
        ArrayList<ArrayList<Integer>> curCSVectorClockRange = new ArrayList<>();
        curCSVectorClockRange.add(csEnteringVectorClock);
        curCSVectorClockRange.add(csLeavingVectorClock);
        allCSVectorClocks.add(curCSVectorClockRange);

        insideCS = false;
        setCurReqLogicalTime(-1); // No longer have any CS request
        
        // Sending the deferred reply messages
        for (int idx = 0; idx < deferedReplyMsg.size(); ++idx) {
            GiveRequestedKey(deferedReplyMsg.get(idx).getSenderNodeId());
        }
        deferedReplyMsg.clear();

        // If all requests have been processed
        if (reqNo == totalRequest) {
            System.out.println("");
            InitiateTermination(); // Comment this during experimental evaluation
        }
    }

    /* Generating a random value from exponential distribution from a given mean */
    public long ExponentialDist(int meanExpoVal) {
        double lambda = 1.0 / meanExpoVal;
        Random rand = new Random();
        double randomExpoVal = -Math.log(1 - rand.nextDouble()) / lambda;
        return Math.round(randomExpoVal);
    }

    public void startApplication() {
        for (int curReq = 0; curReq < totalRequest; ++curReq) {
            CSEnter(); // Blocking request by the application for CS

            long csExecutionTime = ExponentialDist(meanCSExecutionTime);
            waitFor(csExecutionTime); // Executing CS
            
            CSLeave(); // Informing service that CS execution is done
            
            long interRequestDelay = ExponentialDist(meanInterReqDelay);
            waitFor(interRequestDelay); // Waiting before requesting for next CS
        }
    }
}
