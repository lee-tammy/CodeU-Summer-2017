package codeu.chat.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import codeu.chat.util.Logger;
import codeu.chat.util.Serializer;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

public class ServerInfo {
  public final Time startTime;
  private final static String SERVER_VERSION = "1.0.0";
  public final Uuid version;
  private static final Logger.Log LOG = Logger.newLog(ServerInfo.class);

  public static final Serializer<ServerInfo> SERIALIZER = new Serializer<ServerInfo>() {

    @Override
    public void write(OutputStream out, ServerInfo value) throws IOException {
      Time.SERIALIZER.write(out, value.startTime);
      Uuid.SERIALIZER.write(out, value.version);
    }

    @Override
    public ServerInfo read(InputStream in) throws IOException {
      return new ServerInfo(Time.SERIALIZER.read(in), Uuid.SERIALIZER.read(in));
    }

  };

  public ServerInfo() {
    Uuid version = null;
    try {
      version = Uuid.parse(SERVER_VERSION);
    } catch (IOException ex) {
      LOG.error("Couldn't initialize ServerInfo correctly", ex);
    }
    this.version = version;
    this.startTime = Time.now();
  }

  public ServerInfo(Time now) {
    Uuid version = null;
    try {
      version = Uuid.parse(SERVER_VERSION);
    } catch (IOException ex) {
      LOG.error("Couldn't initialize ServerInfo correctly", ex);
    }
    this.version = version;
    this.startTime = now;
  }

  public ServerInfo(Uuid version) {
    this(Time.now(), version);
  }

  public ServerInfo(Time startTime, Uuid version) {
    this.startTime = startTime;
    this.version = version;
  }
}