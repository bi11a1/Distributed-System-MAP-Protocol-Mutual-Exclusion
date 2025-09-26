
public class Main {
    public static void main(String[] args) {
        String configFilePath;
        String domain;

        configFilePath = args[0]; // Containing the config.txt path as the first argument
        domain = ".utdallas.edu"; // This is added after the dc machine ids to form a complete host name 

        MAP newMAP = new MAP(configFilePath, domain);

        // Run the MAP protocol in one thread and the snapshot protocol on another thread
        Thread protocolThread = new Thread(() -> newMAP.startMAPProtocol());
        Thread snapshotThread = new Thread(() -> newMAP.startSnapshotProtocol());

        protocolThread.start();
        snapshotThread.start();
    }
}
