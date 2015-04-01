package edu.umass.cs.gns.newApp;

import edu.umass.cs.gns.database.MongoRecords;
import edu.umass.cs.gns.main.GNS;
import edu.umass.cs.gns.nsdesign.Config;
import edu.umass.cs.gns.nsdesign.nodeconfig.GNSNodeConfig;
import java.io.IOException;
import edu.umass.cs.gns.reconfiguration.AbstractReplicaCoordinator;
import edu.umass.cs.gns.reconfiguration.InterfaceReconfigurableNodeConfig;
import edu.umass.cs.gns.reconfiguration.ReconfigurableNode;
import edu.umass.cs.gns.reconfiguration.ReconfigurationConfig;
import edu.umass.cs.gns.reconfiguration.reconfigurationutils.LocationBasedDemandProfile;
import java.util.Set;

/**
 * @author Westy
 * @param <NodeIDType>
 */
public class AppReconfigurableNode<NodeIDType> extends ReconfigurableNode<NodeIDType> {

  private MongoRecords<NodeIDType> mongoRecords = null;

  public AppReconfigurableNode(NodeIDType nodeID, InterfaceReconfigurableNodeConfig<NodeIDType> nc)
          throws IOException {
    super(nodeID, nc);

  }

  @Override
  protected AbstractReplicaCoordinator<NodeIDType> createAppCoordinator() {
    // this is called by super so we need to get this field initialized now
    if (this.mongoRecords == null) {
      this.mongoRecords = new MongoRecords<>(this.myID, Config.mongoPort);
    }
    NewApp app = new NewApp(this.myID, this.nodeConfig, this.messenger, mongoRecords);

    NewAppCoordinator appCoordinator = new NewAppCoordinator(app, this.nodeConfig, this.messenger);

    // start the NSListenerAdmin thread
    new AppAdmin(app, (GNSNodeConfig) nodeConfig).start();

    GNS.getLogger().info(myID.toString() + " Admin thread initialized");

    return appCoordinator;
  }

  private static void startNodePair(String nodeID, String nodeConfigFilename) throws IOException {
    GNSNodeConfig nodeConfig = new GNSNodeConfig(nodeConfigFilename, nodeID);
    new AppReconfigurableNode(nodeConfig.getReplicaNodeIdForTopLevelNode(nodeID), nodeConfig);
    new AppReconfigurableNode(nodeConfig.getReconfiguratorNodeIdForTopLevelNode(nodeID), nodeConfig);
  }

  private static void startTestNodes() throws IOException {
    // Change this to whatever your path is...
    String filename = Config.WESTY_GNS_DIR_PATH + "/conf/name-server-info";

    GNSNodeConfig nodeConfig = new GNSNodeConfig(filename, true);
    try {
      for (String activeID : (Set<String>) nodeConfig.getActiveReplicas()) {
        System.out.println("#####################################################");
        System.out.println("############# Setting up active replica " + activeID);
        new AppReconfigurableNode(activeID, nodeConfig);
      }
      for (String rcID : (Set<String>) nodeConfig.getReconfigurators()) {
        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        System.out.println("$$$$$$$$$$$$$$$$ Setting up reconfigurator " + rcID);
        new AppReconfigurableNode(rcID, nodeConfig);
      }

    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  public static void main(String[] args) throws IOException {
    //ReconfigurationConfig.setDemandProfile(NullDemandProfile.class);
    //ReconfigurationConfig.setDemandProfile(DemandProfile.class);
    ReconfigurationConfig.setDemandProfile(LocationBasedDemandProfile.class);
    System.out.println("********* DEMAND PROFILE: " + ReconfigurationConfig.getDemandProfile());

    if (args.length == 0) {
      startTestNodes();
    } else if (args.length == 2) {
      startNodePair(args[0], args[1]);
    } else {
      System.out.println("Usage: java -cp GNS.jar edu.umass.cs.gns.newApp.edu.umass.cs.gns.newApp.AppReconfigurableNode <NodeId> <nodeConfigFile>");
    }
  }
  
}
