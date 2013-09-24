package edu.umass.cs.gns.nameserver.recordmap;

import edu.umass.cs.gns.database.BasicRecordCursor;
import edu.umass.cs.gns.database.CassandraRecords;
import edu.umass.cs.gns.database.Field;
import edu.umass.cs.gns.exceptions.FieldNotFoundException;
import edu.umass.cs.gns.exceptions.RecordNotFoundException;
import edu.umass.cs.gns.main.GNS;
import edu.umass.cs.gns.main.StartNameServer;
import edu.umass.cs.gns.nameserver.NameRecord;
import edu.umass.cs.gns.nameserver.replicacontroller.ReplicaControllerRecord;
import edu.umass.cs.gns.util.JSONUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

public class CassandraRecordMap extends BasicRecordMap {

  private String collectionName;

  public CassandraRecordMap(String collectionName) {
    this.collectionName = collectionName;
  }

  @Override
  public Set<String> getAllRowKeys() {
    CassandraRecords records = CassandraRecords.getInstance();
    return records.keySet(collectionName);
  }

  @Override
  public Set<String> getAllColumnKeys(String name) {
    if (!containsName(name)) {
      try {
        CassandraRecords records = CassandraRecords.getInstance();
        JSONObject json = records.lookup(collectionName, name);
        return JSONUtils.JSONArrayToSetString(json.names());
      } catch (JSONException e) {
        GNS.getLogger().severe("Error updating json record: " + e);
        return null;
      }
    } else {
      return null;
    }
  }

  @Override
  public HashMap<Field, Object> lookup(String name, Field nameField, ArrayList<Field> fields1) throws RecordNotFoundException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public HashMap<Field, Object> lookup(String name, Field nameField, ArrayList<Field> fields1,
          Field valuesMapField, ArrayList<Field> valuesMapKeys) throws RecordNotFoundException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void update(String name, Field nameField, ArrayList<Field> fields1, ArrayList<Object> values1) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void update(String name, Field nameField, ArrayList<Field> fields1, ArrayList<Object> values1,
          Field valuesMapField, ArrayList<Field> valuesMapKeys, ArrayList<Object> valuesMapValues) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void increment(String name, ArrayList<Field> fields1, ArrayList<Object> values1) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void increment(String name, ArrayList<Field> fields1, ArrayList<Object> values1, Field votesMapField, ArrayList<Field> votesMapKeys, ArrayList<Object> votesMapValues) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public BasicRecordCursor getIterator(Field nameField, ArrayList<Field> fields) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public BasicRecordCursor getAllRowsIterator() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  
  @Override
  public BasicRecordCursor queryUserField(Field valuesMapField, String key, Object value) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public NameRecord getNameRecord(String name) {
    try {
      JSONObject json = CassandraRecords.getInstance().lookup(collectionName, name);
      if (json == null) {
        return null;
      } else {
        return new NameRecord(json);
      }
    } catch (JSONException e) {
      GNS.getLogger().severe("Error getting name record " + name + ": " + e);
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void addNameRecord(NameRecord recordEntry) {
    if (StartNameServer.debugMode) {
      try {
        GNS.getLogger().fine("Start addNameRecord " + recordEntry.getName());
      } catch (FieldNotFoundException e) {
        GNS.getLogger().severe("Field not found exception. " + e.getMessage());
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        return;
      }
    }
    try {
      addNameRecord(recordEntry.toJSONObject());
      //CassandraRecords.getInstance().insert(collectionName, recordEntry.getName(), recordEntry.toJSONObject());
    } catch (JSONException e) {
      e.printStackTrace();
      GNS.getLogger().severe("Error adding name record: " + e);
      return;
    }
  }

  @Override
  public void addNameRecord(JSONObject json) {
    CassandraRecords records = CassandraRecords.getInstance();
    try {
      String name = json.getString(NameRecord.NAME.getName());
      records.insert(collectionName, name, json);
      GNS.getLogger().finer(records.toString() + ":: Added " + name);
    } catch (JSONException e) {
      GNS.getLogger().severe(records.toString() + ":: Error adding name record: " + e);
      e.printStackTrace();
    }
  }

  @Override
  public void updateNameRecord(NameRecord recordEntry) {
    try {
      CassandraRecords.getInstance().update(collectionName, recordEntry.getName(), recordEntry.toJSONObject());
    } catch (JSONException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (FieldNotFoundException e) {
      GNS.getLogger().severe("Field found found exception: " + e.getMessage());
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  @Override
  public void removeNameRecord(String name) {
    CassandraRecords.getInstance().remove(collectionName, name);
  }

  @Override
  public boolean containsName(String name) {
    return CassandraRecords.getInstance().contains(collectionName, name);
  }

  @Override
  public void reset() {
    CassandraRecords.getInstance().reset(collectionName);
  }

  @Override
  public ReplicaControllerRecord getNameRecordPrimary(String name) {
    try {
      JSONObject json = CassandraRecords.getInstance().lookup(collectionName, name);
      if (json == null) {
        return null;
      } else {
        return new ReplicaControllerRecord(json);
      }
    } catch (JSONException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    return null;
  }

  @Override
  public void addNameRecordPrimary(ReplicaControllerRecord recordEntry) {
    try {
      CassandraRecords.getInstance().insert(collectionName, recordEntry.getName(), recordEntry.toJSONObject());
    } catch (JSONException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      return;
    } catch (FieldNotFoundException e) {
      GNS.getLogger().severe("Field not found " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public void updateNameRecordPrimary(ReplicaControllerRecord recordEntry) {
    try {
      CassandraRecords.getInstance().update(collectionName, recordEntry.getName(), recordEntry.toJSONObject());
    } catch (JSONException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (FieldNotFoundException e) {
      GNS.getLogger().severe("Field not found " + e.getMessage());
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }
//  // test code
//  public static void main(String[] args) throws Exception {
//    NameServer.nodeID = 4;
//    retrieveFieldTest();
//    //System.exit(0);
//  }
//
//  private static void retrieveFieldTest() throws Exception {
//    ConfigFileInfo.readHostInfo("ns1", NameServer.nodeID);
//    HashFunction.initializeHashFunction();
//    BasicRecordMap recordMap = new CassandraRecordMap(CassandraRecords.DBNAMERECORD);
//    System.out.println(recordMap.getNameRecordFieldAsIntegerSet("1A434C0DAA0B17E48ABD4B59C632CF13501C7D24", NameRecord.PRIMARY_NAMESERVERS.getName()));
//    recordMap.updateNameRecordFieldAsIntegerSet("1A434C0DAA0B17E48ABD4B59C632CF13501C7D24", "FRED", new HashSet<Integer>(Arrays.asList(1, 2, 3)));
//    System.out.println(recordMap.getNameRecordFieldAsIntegerSet("1A434C0DAA0B17E48ABD4B59C632CF13501C7D24", "FRED"));
//  }

}
