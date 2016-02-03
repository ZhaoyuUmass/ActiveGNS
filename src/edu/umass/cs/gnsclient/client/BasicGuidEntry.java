/*
 *
 *  Copyright (c) 2015 University of Massachusetts
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 *  Initial developer(s): Westy, Emmanuel Cecchet
 *
 */
package edu.umass.cs.gnsclient.client;

import edu.umass.cs.gnscommon.GnsProtocol;
import edu.umass.cs.gnscommon.utils.Base64;
import edu.umass.cs.gnscommon.exceptions.client.EncryptionException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class defines a BasicGuidEntry. This objects encapsulates just the information associated with a guid that 
 * can be read and written from a GNS server. See also <code>GuidEntry</code> which also contains the private key 
 * and cannot be read or written to the GNS.
 *
 * @author Westy
 */
public class BasicGuidEntry {

  protected String entityName;
  protected String guid;
  protected PublicKey publicKey;

  /**
   * Creates an empty new <code>BasicGuidEntry</code> object.
   */
  public BasicGuidEntry() {
  }

  /**
   * Creates a new <code>BasicGuidEntry</code> object.
   *
   * @param entityName entity name (usually an email)
   * @param guid Guid generated by the GNS
   * @param publicKey public key
   */
  public BasicGuidEntry(String entityName, String guid, PublicKey publicKey) {
    this.entityName = entityName;
    this.guid = guid;
    this.publicKey = publicKey;
  }
  
  /**
   * Creates a new <code>BasicGuidEntry</code> object from a JSONObject.
   * 
   * @param json 
   * @throws org.json.JSONException 
   * @throws edu.umass.cs.gnscommon.exceptions.client.EncryptionException 
   */
  public BasicGuidEntry (JSONObject json) throws JSONException, EncryptionException {
    this.entityName = json.getString(GnsProtocol.GUID_RECORD_NAME);
    this.guid = json.getString(GnsProtocol.GUID_RECORD_GUID);
    this.publicKey = generatePublicKey(json.getString(GnsProtocol.GUID_RECORD_PUBLICKEY));
  } 

  /**
   * Returns the entityName value.
   *
   * @return Returns the entityName.
   */
  public String getEntityName() {
    return entityName;
  }

  /**
   * Returns the guid value.
   *
   * @return Returns the guid.
   */
  public String getGuid() {
    return guid;
  }

  /**
   * Returns the publicKey value.
   *
   * @return Returns the publicKey.
   */
  public PublicKey getPublicKey() {
    return publicKey;
  }
  
  /**
   * Returns the public key as a string - which for us means as
   * a Base64 encoded string without line separators.
   * 
   * @return 
   */
  public String getPublicKeyString() {
    byte[] publicKeyBytes = publicKey.getEncoded();
    return Base64.encodeToString(publicKeyBytes, false);
  }

  /**
   * Converts this BasicGuidEntry to a string.
   * 
   * @return
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return entityName + " (" + guid + ")";
  }

  /**
   * Indicates whether some object is equal to this BasicGuidEntry object.
   * 
   * @param o
   * @return
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof BasicGuidEntry)) {
      return false;
    }
    BasicGuidEntry other = (BasicGuidEntry) o;
    if (entityName == null && other.getEntityName() != null) {
      return false;
    }
    if (entityName != null && !entityName.equals(other.getEntityName())) {
      return false;
    }
    return !publicKey.equals(other.getPublicKey());
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 97 * hash + (this.entityName != null ? this.entityName.hashCode() : 0);
    hash = 97 * hash + (this.guid != null ? this.guid.hashCode() : 0);
    hash = 97 * hash + (this.publicKey != null ? this.publicKey.hashCode() : 0);
    return hash;
  }
  
  private PublicKey generatePublicKey(String encodedPublic)
          throws EncryptionException {
    byte[] encodedPublicKey = Base64.decode(encodedPublic);

    try {
      KeyFactory keyFactory = KeyFactory.getInstance(GnsProtocol.RSA_ALGORITHM);
      X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
              encodedPublicKey);
      return keyFactory.generatePublic(publicKeySpec);

    } catch (NoSuchAlgorithmException e) {
      throw new EncryptionException("Failed to generate keypair", e);
    } catch (InvalidKeySpecException e) {
      throw new EncryptionException("Failed to generate keypair", e);
    }
  }

}
