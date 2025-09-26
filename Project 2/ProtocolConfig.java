/* Contains all the global variable used */

public class ProtocolConfig {
    // How many times the client should try to connect with the server
    public static int maxRetryCount = 5000;
    // Time to wait before trying again to do something
    public static int retryDelayMS = 1;

    // Different tags for identifying the type of the message sent over the channel
    public static String sendKeyMsgType = "Request for a key";
    public static String receiveKeyMsgType = "Reply with a key";
    public static String terminationMsgType = "Done Processing";
    public static String sendCntMsgType = "Send Message Count";
    public static String closeConnectionMsgType = "Close All Connections";

    // Initiates the CS verification
    public static int coordinatorNodeId = 0;

    // if true then save current result in the file
    public static boolean storeResult = false;

    // Private constructor to prevent instantiation
    private ProtocolConfig() {}
}
