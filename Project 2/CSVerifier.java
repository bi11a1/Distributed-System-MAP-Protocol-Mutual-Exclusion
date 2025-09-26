import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CSVerifier {
    private static int totalNodes;
    private static int totalRequest;
    private static int meanInterReqDelay;
    private static int meanCSExecutionTime;
    private static ArrayList<Integer> totalMsgSent = new ArrayList<>();
    private static ArrayList<ArrayList<Long>> allReqTimestamps = new ArrayList<>();
    private static ArrayList<ArrayList<ArrayList<Long>>> allCSTimestamps = new ArrayList<>();
    private static ArrayList<ArrayList<ArrayList<ArrayList<Integer>>>> allCSVectorClocks = new ArrayList<>();

    public CSVerifier(ArrayList<Message> allNodeInfo, int totalRequest, int meanInterReqDelay, int meanCSExecutionTime) {
        CSVerifier.totalNodes = allNodeInfo.size();
        CSVerifier.totalRequest = totalRequest;
        CSVerifier.meanInterReqDelay = meanInterReqDelay;
        CSVerifier.meanCSExecutionTime = meanCSExecutionTime;

        Collections.sort(allNodeInfo, Comparator.comparingInt(Message::getSenderNodeId));
        for (int idx = 0; idx < totalNodes; ++idx) {
            allReqTimestamps.add(allNodeInfo.get(idx).getAllReqTimestampInfo());
            allCSTimestamps.add(allNodeInfo.get(idx).getAllCSTimestampInfo());
            allCSVectorClocks.add(allNodeInfo.get(idx).getAllCSVectorClocks());
        }
    }

    public void updateTotalMsgSent(ArrayList<Integer> totalMsgSent) {
        CSVerifier.totalMsgSent = totalMsgSent;
    }

    /* Checks if two vector clocks have happenned before relation or not */
    public boolean isHappennedBefore(ArrayList<ArrayList<Integer>> firstCS, ArrayList<ArrayList<Integer>> secondCS) {
        // firstCS -> secondCS
        int greater = 0;
        int greaterEqual = 0;

        for (int idx = 0; idx < totalNodes; ++idx) {
            if (firstCS.get(1).get(idx) > secondCS.get(0).get(idx)) {
                greater++;
            } 
            if ((firstCS.get(1).get(idx) >= secondCS.get(0).get(idx))) {
                greaterEqual++;
            }
        }
        if (greaterEqual == totalNodes && greater > 0) {
            return true;
        }

        // secondCS -> firstCS
        greater = 0;
        greaterEqual = 0;
        for (int idx = 0; idx < totalNodes; ++idx) {
            if (secondCS.get(1).get(idx) > firstCS.get(0).get(idx)) {
                greater++;
            }
            if (secondCS.get(1).get(idx) >= firstCS.get(0).get(idx)) {
                greaterEqual++;
            }
        }
        if (greaterEqual == totalNodes && greater > 0) {
            return true;
        }

        System.out.println("Found Overlap between: " + firstCS + " and " + secondCS);
        return false;
    }

    /* Display all vector clock range */
    public void showAllVectorClocks() {
        for (int i = 0; i < allCSVectorClocks.size(); ++i) {
            System.out.println("For node " + i + ":");
            for (int j = 0; j < allCSVectorClocks.get(i).size(); ++j) {
                System.out.println(allCSVectorClocks.get(i).get(j));
            }
            System.out.println();
        }
    }

    /* Verifies if there is any overlap in the CS */
    public boolean startVerifyingCS() {
        //showAllVectorClocks();
        for (int node1 = 0; node1 < totalNodes; ++node1) {
            for (int node2 = node1+1; node2 < totalNodes; ++node2) {
                for (int cs1 = 0; cs1 < allCSVectorClocks.get(node1).size(); ++cs1) {
                    for (int cs2 = 0; cs2 < allCSVectorClocks.get(node2).size(); ++cs2) {
                        ArrayList<ArrayList<Integer>> firstCS = allCSVectorClocks.get(node1).get(cs1);
                        ArrayList<ArrayList<Integer>> secondCS = allCSVectorClocks.get(node2).get(cs2);
                        if (isHappennedBefore(firstCS, secondCS) == false) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    /* Calculates message complexity, response time, and system throughput */
    public void logComplexity() {
        System.out.println("CS Execution Time:" + meanCSExecutionTime);

        double msgComplexity = 0;
        double responseTime = 0;
        double systemThroughput = 0;

        Long systemStartTime = -1L;
        Long systemEndTime = -1L;

        for (int curNodeId = 0; curNodeId < totalNodes; ++curNodeId) {
            msgComplexity += totalMsgSent.get(curNodeId);
            
            for (int cs = 0; cs < totalRequest; ++cs) {
                Long reqTime = allReqTimestamps.get(curNodeId).get(cs);
                Long csExecCompleteTime = allCSTimestamps.get(curNodeId).get(cs).get(1);
                Long curResponseTime = csExecCompleteTime - reqTime;
                responseTime += curResponseTime;
                
                if (systemStartTime == -1) {
                    systemStartTime = allReqTimestamps.get(curNodeId).get(cs);
                } else {
                    systemStartTime = Math.min(systemStartTime, allReqTimestamps.get(curNodeId).get(cs));
                }

                if (systemEndTime == -1) {
                    systemEndTime = allCSTimestamps.get(curNodeId).get(cs).get(1);
                } else {
                    systemEndTime = Math.max(systemEndTime, allCSTimestamps.get(curNodeId).get(cs).get(1));
                }
            }
        }
        msgComplexity /= (totalNodes*totalRequest);
        System.out.printf("Message Complexity: %.2f\n", msgComplexity);
        
        responseTime /= (totalNodes*totalRequest);
        System.out.printf("Response Time: %.2f (ms)\n", responseTime);

        systemThroughput = (double) 1000*(totalNodes*totalRequest) / (systemEndTime-systemStartTime);
        System.out.printf("System Throughput: %.2f (per second)\n", systemThroughput);

        System.out.println("");

        if (ProtocolConfig.storeResult != true) {
            return;
        }

        // Storing the value in a file
        String filePath = "public_html/Project2/Experiments.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            // Create the file if it doesn't exist
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
                System.out.println("File created at " + filePath);
                writer.println("MeanCSExTime MsgComplexity ResponseTime SysThroughput");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
            // Append values to the file
            writer.print(meanCSExecutionTime + " ");
            writer.println(msgComplexity + " " + responseTime + " " + systemThroughput);

            System.out.println("Added the values!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
