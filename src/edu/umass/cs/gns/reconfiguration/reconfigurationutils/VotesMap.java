/*
 * Copyright (C) 2015
 * University of Massachusetts
 * All Rights Reserved 
 *
 * Initial developer(s): Westy.
 */
package edu.umass.cs.gns.reconfiguration.reconfigurationutils;

import edu.umass.cs.gns.main.GNS;
import edu.umass.cs.gns.util.Util;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A map between InetAddress and access counts.
 * 
 * @author westy
 */
public class VotesMap {

  JSONObject storage;

  /**
   * Creates a new empty VotesMap.
   */
  public VotesMap() {
    this.storage = new JSONObject();
  }

  /**
   * Creates a new VotesMap by copying a VotesMap.
   */
  public VotesMap(VotesMap votesMap) {
    this(votesMap.storage);
  }

  /**
   * Creates a new VotesMap from a JSON Object.
   */
  public VotesMap(JSONObject json) {
    this();
    Iterator<?> keyIter = json.keys();
    while (keyIter.hasNext()) {
      String key = (String) keyIter.next();
      try {
        storage.put(key, json.get(key));
      } catch (JSONException e) {
        GNS.getLogger().severe("Unable to parse JSON: " + e);
      }
    }
  }
  
  /**
   * Converts a VotesMap object into a JSONObject.
   * @return a JSONObject
   */
  public JSONObject toJSONObject() {
    return storage;
  }

  /**
   * Increments the value corresponding to the sender InetAddress by 1.
   * 
   * @param sender 
   */
  public void increment(InetAddress sender) {
    try {
      storage.increment(sender.getHostAddress());
    } catch (JSONException e) {
      GNS.getLogger().severe("Unable to parse JSON: " + e);
    }
  }

  /**
   * Returns the top N vote getting InetAddresses in the map.
   * Will return less if there are not N distinct entries.
   * @param n
   * @return an ArrayList of the top n
   */
  public ArrayList<InetAddress> getTopN(int n) {
    ArrayList<InetAddress> result = new ArrayList<>();
    // convert the JSONObject into a Map and sort it by value decreasing
    Map<String, Integer> map = Util.sortByValueDecreasing(toMap(storage));
    int cnt = 0;
    for (Map.Entry<String, Integer> entry : map.entrySet()) {
      if (cnt >= n) {
        break;
      }
      try {
        result.add(InetAddress.getByName(entry.getKey()));
      } catch (UnknownHostException e) {
        GNS.getLogger().severe("Unable to parse InetAddress: " + e);
      }
      cnt++;
    }
    return result;
  }

  /**
   * Adds the votes from update to the votes in this object.
   *
   * @param update
   */
  public void combine(VotesMap update) {
    Iterator<?> keyIter = update.storage.keys();
    while (keyIter.hasNext()) {
      String key = (String) keyIter.next();
      try {
        // optInt returns zero if the key doesn't exist
        storage.put(key, storage.optInt(key) + update.storage.getInt(key));
      } catch (JSONException e) {
        GNS.getLogger().severe("Unable to parse JSON: " + e);
      }
    }
  }
 
  @Override
  public String toString() {
    return toJSONObject().toString();
  }

  public static void main(String[] args) throws JSONException, UnknownHostException {
    VotesMap votesMap1 = new VotesMap();
    VotesMap votesMap2 = new VotesMap();

    votesMap1.increment(InetAddress.getByName("127.0.0.1"));
    votesMap1.increment(InetAddress.getByName("127.0.0.1"));
    votesMap1.increment(InetAddress.getByName("128.119.16.3"));

    votesMap2.increment(InetAddress.getByName("10.0.1.2"));
    votesMap2.increment(InetAddress.getByName("128.119.16.3"));
    votesMap2.increment(InetAddress.getByName("127.0.0.1"));
    
    VotesMap votesMap3 = new VotesMap(votesMap2);

    System.out.println(votesMap1);
    System.out.println(votesMap2);
    votesMap1.combine(votesMap2);
    System.out.println(votesMap1);
    votesMap1.combine(votesMap3);
    System.out.println(votesMap1);
    System.out.println(votesMap1.getTopN(10));
  }
  
  /**
   * Converts a JSONObject with integer values into a map.
   * 
   * @param json
   * @return a map
   */
  private static Map<String, Integer> toMap(JSONObject json) {
    Map<String, Integer> map = new HashMap<String, Integer>();
    try {
      Iterator<String> nameItr = json.keys();
      while (nameItr.hasNext()) {
        String name = nameItr.next();
        map.put(name, json.getInt(name));
      }
    } catch (JSONException e) {
      GNS.getLogger().severe("Unable to parse JSON: " + e);
    }
    return new HashMap<String, Integer>(map);
  }
}
