/* Contains all the global variable used */

public class ProtocolConfig {
    // How many times the client should try to connect with the server
    public static int maxRetryCount = 1000;
    // Time to wait before trying again to do something
    public static int retryDelayMS = 100;

    // Different tags for identifying the type of the message sent over the channel
    public static String vectorClockMsgType = "Vector Clock";
    public static String terminationMsgType = "Terminate";
    public static String markerMsgType = "Marker";
    public static String resetSnapshotType = "Reset Snapshot";
    public static String doneResetType = "Done Resetting";
    public static String localStateType = "Local State During Snapshot";
    public static String inTransitMsgType = "In Transit Message";

    // This Node Id is considered as the coordinator and will be active at the beginning
    public static int coordinatorNodeId = 0;

    // Private constructor to prevent instantiation
    private ProtocolConfig() {}
}
