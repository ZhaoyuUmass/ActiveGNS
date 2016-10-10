package edu.umass.cs.gnsserver.utils;

import edu.umass.cs.gnscommon.GNSResponseCode;

public class AclResult {
  private final String publicKey;
  private final boolean aclCheckPassed;
  private final GNSResponseCode responseCode;

  public AclResult(String publicKey, boolean aclCheckPassed, GNSResponseCode responseCode) {
    this.publicKey = publicKey;
    this.aclCheckPassed = aclCheckPassed;
    this.responseCode = responseCode;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public boolean isAclCheckPassed() {
    return aclCheckPassed;
  }

  public GNSResponseCode getResponseCode() {
    return responseCode;
  }
}
