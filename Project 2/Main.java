public class Main {
    public static void main(String[] args) {
        String configFilePath;
        String domain;

        configFilePath = args[0]; // Containing the config.txt path as the first argument
        domain = ".utdallas.edu"; // This is added after the dc machine ids to form a complete host name

        MutualExclusion mutualExclusion = new MutualExclusion(configFilePath, domain);
        System.out.println("System up? " + MutualExclusion.getNodeStatus());
        
        mutualExclusion.startApplication();
    }
}
