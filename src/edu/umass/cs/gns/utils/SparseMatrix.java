/*
 * Copyright (C) 2014
 * University of Massachusetts
 * All Rights Reserved 
 *
 * Initial developer(s): Westy.
 */
package edu.umass.cs.gns.utils;

import java.util.HashMap;

/**
 * Simple implementation of a Sparse Matrix using nested HashMaps.
 *
 * @author westy
 * @param <U>
 * @param <V>
 * @param <T>
 */
public class SparseMatrix<U, V, T> {

  HashMap<U, HashMap<V, T>> rows = new HashMap<>();

  T defaultValue;

  /**
   * * Creates a SparseMatrix with a default item value of null.
   */
  public SparseMatrix() {
    this(null);
  }

  /**
   * Creates a SparseMatrix with a default item value of defaultValue.
   *
   * @param defaultValue
   */
  public SparseMatrix(T defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   *
   * @param i
   * @param j
   * @param t
   */
  public void put(U i, V j, T t) {
    if (rows.get(i) == null) {
      rows.put(i, new HashMap<V, T>());
    }
    rows.get(i).put(j, t);
  }

  /**
   *
   * @param i
   * @param j
   * @return
   */
  public T get(U i, V j) {
    if (rows.get(i) == null) {
      return this.defaultValue;
    } else {
      T result = rows.get(i).get(j);
      if (result != null) {
        return result;
      } else {
        return this.defaultValue;
      }
    }
  }

  @Override
  public String toString() {
    return "SparseMatrix{" + "rows=" + rows + '}';
  }

}