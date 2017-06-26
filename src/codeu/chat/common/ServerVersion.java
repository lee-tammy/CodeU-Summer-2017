package codeu.chat.common;


import java.io.IOException;

import codeu.chat.util.Uuid;

/**
 * Checks the version of the server
 */
public final class ServerVersion {
  private final static String SERVER_VERSION = "1.0.0";

  public final Uuid version;
  
  public ServerVersion() {
    Uuid version = null;
    
    try {
      version = Uuid.parse(SERVER_VERSION);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not parse version");
    }
    
    this.version = version;
  }

  public ServerVersion(Uuid version) {
    this.version = version;
  }
}
