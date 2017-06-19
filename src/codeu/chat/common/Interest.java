package codeu.chat.common;

import codeu.chat.util.Serializer;
import codeu.chat.util.Uuid;
import codeu.chat.util.Time;
import codeu.chat.common.Type;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class Interest {

  public final Uuid id;
  public final Uuid interestId;
  public final Type type;
  public final Time lastUpdate;

  public static final Serializer<Interest> SERIALIZER = new Serializer<Interest>() {
    @Override
    public void write(OutputStream out, Interest value) throws IOException {
      Uuid.SERIALIZER.write(out, value.id);
      Uuid.SERIALIZER.write(out, value.interestId);
      Type.SERIALIZER.write(out, value.type);
      Time.SERIALIZER.write(out, value.lastUpdate);
    }
    @Override
    public Interest read(InputStream in) throws IOException {
      return new Interest(Uuid.SERIALIZER.read(in),
          Uuid.SERIALIZER.read(in),
          Type.SERIALIZER.read(in),
          Time.SERIALIZER.read(in));
    }
  };

  public Interest(Uuid id, Uuid interestId, Type type, Time now) {
    this.id = id;
    this.interestId = interestId;
    this.type = type;
    lastUpdate = now;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) return false;
    if (!(other instanceof Interest)) return false;
    Interest that = (Interest) other;
    if (!(this.id.equals(that.id))) return false;
    if (!(this.interestId.equals(that.interestId))) return false;
    if (!(this.type.equals(that.type))) return false;
    if (!(this.lastUpdate.equals(that.lastUpdate))) return false;
		return true;
  }

  @Override
  public int hashCode() {
    int total = 3 * id.hashCode() + 7 * interestId.hashCode() + 13 *
      type.hashCode() + 17 * lastUpdate.hashCode();
    return total;
  }
}

