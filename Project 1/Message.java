import java.io.Serializable;
import java.util.ArrayList;

/* All communications between nodes are done through Messages */
public class Message implements Serializable {
    private int senderNodeId;
    private int receiverNodeId;
    private String msgType;
    private int msgColor; // For chandy and lamport's snapshot protocol
    private int nodeActivityStatus; // To keep track of whether the node is active or passive
    private ArrayList<Integer> vectorClockMsg;

    public Message(int senderNodeId, int receiverNodeId) {
        this.senderNodeId = senderNodeId;
        this.receiverNodeId = receiverNodeId;
        this.msgColor = -1; // Invalid color - ignore
        this.nodeActivityStatus = -1; // Invalid active status - ignore
    }

    // Setters
    public void setSenderNodeId(int senderNodeId) {
        this.senderNodeId = senderNodeId;
    }

    public void setReceiverNodeId(int receiverNodeId) {
        this.receiverNodeId = receiverNodeId;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public void setVectorClockMsg(ArrayList<Integer> vectorClockMsg) {
        this.vectorClockMsg = new ArrayList<>(vectorClockMsg);
    }

    public void setMsgColor(int msgColor) {
        this.msgColor = msgColor;
    }

    public void setNodeActivityStatus(int nodeActivityStatus) {
        this.nodeActivityStatus = nodeActivityStatus;
    }

    // Getters
    public int getSenderNodeId() {
        return senderNodeId;
    }

    public int getReceiverNodeId() {
        return receiverNodeId;
    }

    public String getMsgType() {
        return msgType;
    }

    public ArrayList<Integer> getVectorClkMsg() {
        return vectorClockMsg;
    }

    public int getMsgColor() {
        return msgColor;
    }

    public int getNodeActivityStatus() {
        return nodeActivityStatus;
    }
}
