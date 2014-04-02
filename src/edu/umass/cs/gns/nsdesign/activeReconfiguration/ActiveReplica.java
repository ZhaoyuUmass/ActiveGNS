package edu.umass.cs.gns.nsdesign.activeReconfiguration;

import edu.umass.cs.gns.main.GNS;
import edu.umass.cs.gns.nio.GNSNIOTransport;
import edu.umass.cs.gns.nsdesign.GNSMessagingTask;
import edu.umass.cs.gns.nsdesign.GNSNodeConfig;
import edu.umass.cs.gns.nsdesign.Reconfigurable;
import edu.umass.cs.gns.nsdesign.packet.NewActiveSetStartupPacket;
import edu.umass.cs.gns.nsdesign.packet.OldActiveSetStopPacket;
import edu.umass.cs.gns.nsdesign.packet.Packet;
import edu.umass.cs.gns.util.UniqueIDHashMap;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Implements group reconfiguration functionality.
 *
 * Created by abhigyan on 3/27/14.
 */
public class ActiveReplica {

  private Reconfigurable reconfigurableApp;

  /**ID of this node*/
  private int nodeID;

  /** nio server*/
  private GNSNIOTransport nioServer;

  /** executor service for handling tasks */
  private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

  /** Configuration for all nodes in GNS **/
  private GNSNodeConfig gnsNodeConfig;


  /** Ongoing stop requests proposed by this active replica. */
  private ConcurrentHashMap<String, OldActiveSetStopPacket> ongoingStops =
          new ConcurrentHashMap<String, OldActiveSetStopPacket>();

  private UniqueIDHashMap ongoingStateTransferRequests = new UniqueIDHashMap();

  private UniqueIDHashMap activeStartupInProgress = new UniqueIDHashMap();



  public ActiveReplica(int nodeID, HashMap<String, String> configParameters, GNSNodeConfig gnsNodeConfig,
                       GNSNIOTransport nioServer, ScheduledThreadPoolExecutor scheduledThreadPoolExecutor,
                       Reconfigurable reconfigurableApp) {
    this.nodeID = nodeID;
    this.gnsNodeConfig = gnsNodeConfig;
    this.nioServer = nioServer;
    this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
    this.reconfigurableApp = reconfigurableApp;
  }


  public void handleIncomingPacket(JSONObject json) {
    try {
      Packet.PacketType type = Packet.getPacketType(json);
      switch (type) {
        // replica controller to active replica
        case NEW_ACTIVE_START:
          GroupChange.handleNewActiveStart(new NewActiveSetStartupPacket(json), this);
          break;
        case NEW_ACTIVE_START_FORWARD:
          GroupChange.handleNewActiveStartForward(new NewActiveSetStartupPacket(json), this);
          break;
        case NEW_ACTIVE_START_RESPONSE:
          GroupChange.handleNewActiveStartResponse(new NewActiveSetStartupPacket(json), this);
          break;
        case NEW_ACTIVE_START_PREV_VALUE_REQUEST:
          GroupChange.handlePrevValueRequest(new NewActiveSetStartupPacket(json), this);
          break;
        case NEW_ACTIVE_START_PREV_VALUE_RESPONSE:
          GroupChange.handlePrevValueResponse(new NewActiveSetStartupPacket(json), this);
          break;
        case OLD_ACTIVE_STOP:
          GroupChange.handleOldActiveStopFromReplicaController(new OldActiveSetStopPacket(json), this);
          break;
        case DELETE_OLD_ACTIVE_STATE:
          GroupChange.deleteOldActiveState(new OldActiveSetStopPacket(json), this);
          break;
      }
    } catch (JSONException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * The app will call this method after it has executed stop decision
   * @param name name for which stop is executed
   * @param version stopped version number of replica
   * @param noError true if stop request was successfully executed, false otherwise.
   */
  public void stopProcessed(String name, int version, boolean noError) {
    try {
      String key = name + "-" + version;
      OldActiveSetStopPacket stopPacket = ongoingStops.remove(key);
      if (stopPacket != null && noError) {
        GNS.getLogger().severe("sent confirmation for name = " + name + " version = " + version + " node = " + nodeID);
        GNSMessagingTask msgTask = GroupChange.getReplicaControllerConfirmMsg(stopPacket, this);
        GNSMessagingTask.send(msgTask, nioServer);
      } else {
        // this should tell us why stop was not sent.
        GNS.getLogger().info("No confirmation to replica controller: name = " + name + " version = " + version +
        " noError = " + noError + " StopPacket = " + stopPacket + " Keys: " + ongoingStops.keySet() + "node = " + nodeID);

      }
    } catch (JSONException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  Reconfigurable getReconfigurableApp() {
    return reconfigurableApp;
  }

  int getNodeID() {
    return nodeID;
  }

  GNSNIOTransport getNioServer() {
    return nioServer;
  }

  ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
    return scheduledThreadPoolExecutor;
  }

  GNSNodeConfig getGnsNodeConfig() {
    return gnsNodeConfig;
  }

  UniqueIDHashMap getOngoingStateTransferRequests() {
    return ongoingStateTransferRequests;
  }

  UniqueIDHashMap getActiveStartupInProgress() {
    return activeStartupInProgress;
  }

  ConcurrentHashMap<String, OldActiveSetStopPacket> getOngoingStops() {
    return ongoingStops;
  }

}