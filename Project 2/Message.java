import java.io.Serializable;
import java.util.ArrayList;

/* All communications between nodes are done through Messages */
public class Message implements Serializable {
    private int senderNodeId;
    private int receiverNodeId;
    private int scalarClockValue;
    private int reqTimestamp;
    private int ignoreThisMsg;
    private String msgType;
    private ArrayList<Long> allReqTimestampsInfo;
    private ArrayList<ArrayList<Long>> allCSTimestampsInfo;
    private ArrayList<Integer> vectorClockValue;
    private ArrayList<ArrayList<ArrayList<Integer>>> allCSVectorClocks;

    public Message(int senderNodeId, int receiverNodeId) {
        this.senderNodeId = senderNodeId;
        this.receiverNodeId = receiverNodeId;
        this.ignoreThisMsg = 0;
        this.allCSTimestampsInfo = new ArrayList<>();
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

    public void setScalarClockValue(int scalarClockValue) {
        this.scalarClockValue = scalarClockValue;
    }

    public void setReqTimestamp(int reqTimestamp) {
        this.reqTimestamp = reqTimestamp;
    }

    public void setIgnoreThisMsg(int ignoreThisMsg) {
        this.ignoreThisMsg = ignoreThisMsg;
    }

    public void setAllReqTimestampsInfo(ArrayList<Long> allReqTimestampsInfo) {
        this.allReqTimestampsInfo = allReqTimestampsInfo;
    }

    public void setAllCSTimestampsInfo(ArrayList<ArrayList<Long>> allCSTimestampsInfo) {
        this.allCSTimestampsInfo = allCSTimestampsInfo;
    }

    public void setVectorClockValue(ArrayList<Integer> vectorClockValue) {
        this.vectorClockValue = new ArrayList<>(vectorClockValue);
    }

    public void setAllCSVectorClocks(ArrayList<ArrayList<ArrayList<Integer>>> allCSVectorClocks) {
        this.allCSVectorClocks = allCSVectorClocks;
    }

    // Getters
    public int getSenderNodeId() {
        return this.senderNodeId;
    }

    public int getReceiverNodeId() {
        return this.receiverNodeId;
    }

    public String getMsgType() {
        return this.msgType;
    }

    public int getScalarClockValue() {
        return this.scalarClockValue;
    }

    public int getReqTimestamp() {
        return this.reqTimestamp;
    }

    public int getIgnoreThisMsg() {
        return this.ignoreThisMsg;
    }
    
    public ArrayList<Long> getAllReqTimestampInfo() {
        return this.allReqTimestampsInfo;
    }

    public ArrayList<ArrayList<Long>> getAllCSTimestampInfo() {
        return this.allCSTimestampsInfo;
    }

    public ArrayList<Integer> getVectorClkValue() {
        return this.vectorClockValue;
    }

    public ArrayList<ArrayList<ArrayList<Integer>>> getAllCSVectorClocks() {
        return this.allCSVectorClocks;
    }
}
