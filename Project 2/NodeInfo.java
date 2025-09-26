import java.util.ArrayList;

class NodeInfo {
    private int nodeId;
    private String hostName;
    private int port;
    private ArrayList<Integer> neighbors;
    
    public NodeInfo(int nodeId, String hostName, int port) {
        this.nodeId = nodeId;
        this.hostName = hostName;
        this.port = port;
    }

    public void AddNeighbors(ArrayList<Integer> neighbors) {
        this.neighbors = neighbors;
    }

    public int getNodeid() {
        return nodeId;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public ArrayList<Integer> getAllNeighbors() {
        return neighbors;
    }

    public void showNodeInfo() {
        System.out.printf("NodeId: %d, Hostname: %s, Port: %d, ", nodeId, hostName, port);
        System.out.printf("Neighbors: [");
        int totalNeighbors = neighbors.size();
        for (int i = 0; i < totalNeighbors; ++i) {
            System.out.printf("%d", neighbors.get(i));
            if (i != totalNeighbors-1) System.out.printf(", ");
        }
        System.out.printf("]\n");
    }
}