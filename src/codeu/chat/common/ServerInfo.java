package codeu.chat.common;

import java.io.IOException;

import codeu.chat.util.Uuid;

public final class ServerInfo {
  private final static String SERVER_VERSION = "1.0.0";

  public final Uuid version;
  
  public ServerInfo() {
    Uuid version = null;
    
    try {
      version = Uuid.parse(SERVER_VERSION);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not parse version");
    }
    
    this.version = version;
  }

  public ServerInfo(Uuid version) {
    this.version = version;
  }
}