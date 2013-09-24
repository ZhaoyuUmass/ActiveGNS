/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.cs.gns.nameserver.recordmap;

import edu.umass.cs.gns.database.BasicRecordCursor;
import edu.umass.cs.gns.nameserver.NameRecord;
import edu.umass.cs.gns.database.Field;
import edu.umass.cs.gns.exceptions.RecordExistsException;
import edu.umass.cs.gns.exceptions.RecordNotFoundException;
import edu.umass.cs.gns.nameserver.replicacontroller.ReplicaControllerRecord;
import org.json.JSONObject;

import java.util.*;

/**
 *
 * @author westy
 */
public interface RecordMapInterface {
  
  public void addNameRecord(NameRecord recordEntry) throws RecordExistsException;

  public NameRecord getNameRecord(String name) throws RecordNotFoundException;

  public void updateNameRecord(NameRecord recordEntry);

  public void addNameRecord(JSONObject json) throws RecordExistsException;

  public void removeNameRecord(String name);

  public boolean containsName(String name);

  public Set<String> getAllColumnKeys(String key) throws RecordNotFoundException;

  public Set<String> getAllRowKeys();

  public void reset();

  public HashMap<Field,Object> lookup(String name, Field nameField, ArrayList<Field> fields1) throws RecordNotFoundException;

  public HashMap<Field,Object> lookup(String name, Field nameField, ArrayList<Field> fields1,
                           Field valuesMapField, ArrayList<Field> valuesMapKeys) throws RecordNotFoundException;

  public abstract void update(String name, Field nameField, ArrayList<Field> fields1, ArrayList<Object> values1);


  public abstract void update(String name, Field nameField, ArrayList<Field> fields1, ArrayList<Object> values1,
                              Field valuesMapField, ArrayList<Field> valuesMapKeys, ArrayList<Object> valuesMapValues);

  public abstract void increment(String name, ArrayList<Field> fields1, ArrayList<Object> values1);

  public abstract void increment(String name, ArrayList<Field> fields1, ArrayList<Object> values1,
                                 Field votesMapField, ArrayList<Field> votesMapKeys, ArrayList<Object> votesMapValues);

  /**
   * Returns an iterator for all the rows in the collection with only the columns in fields filled in except
   * the NAME (AKA the primary key) is always there.
   * 
   * @param nameField - the field in the row that contains the name field
   * @param fields
   * @return 
   */
  public abstract BasicRecordCursor getIterator(Field nameField, ArrayList<Field> fields);

  /**
   * Returns an iterator for all the rows in the collection with all fields filled in.
   * 
   * @return 
   */
  public abstract BasicRecordCursor getAllRowsIterator();
  
  /**
   * Given a key and a value return all the records as a BasicRecordCursor that have a *user* key with that value.
   * 
   * @param valuesMapField - the field in the row that contains the *user* fields
   * @param key
   * @param value
   * @return 
   */
  public abstract BasicRecordCursor queryUserField(Field valuesMapField, String key, Object value);

  // Replica Controller
  
  public ReplicaControllerRecord getNameRecordPrimary(String name) throws RecordNotFoundException;

  public void addNameRecordPrimary(ReplicaControllerRecord recordEntry) throws RecordExistsException;

  public void updateNameRecordPrimary(ReplicaControllerRecord recordEntry);
  
}
