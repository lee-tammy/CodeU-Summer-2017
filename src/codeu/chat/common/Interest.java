package codeu.chat.common;

import codeu.chat.util.Serializer;
import codeu.chat.util.Uuid;
import codeu.chat.util.Time;
import codeu.chat.common.InterestType;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public final class Interest {

  public final Uuid id;
  public final Uuid interestId;
  public final Uuid userId;
  public final InterestType type;
  public Time lastUpdate;

  public static final Serializer<Interest> SERIALIZER = new Serializer<Interest>() {
    @Override
    public void write(OutputStream out, Interest value) throws IOException {
      Uuid.SERIALIZER.write(out, value.id);
      Uuid.SERIALIZER.write(out, value.userId);
      Uuid.SERIALIZER.write(out, value.interestId);
      InterestType.SERIALIZER.write(out, value.type);
      Time.SERIALIZER.write(out, value.lastUpdate);
    }

    @Override
    public Interest read(InputStream in) throws IOException {
      return new Interest(Uuid.SERIALIZER.read(in),
                          Uuid.SERIALIZER.read(in),
                          Uuid.SERIALIZER.read(in),
                          InterestType.SERIALIZER.read(in),
                          Time.SERIALIZER.read(in));
    }
  };

  public Interest(Uuid id, Uuid userId, Uuid interestId, InterestType type, Time now) {
    this.id = id;
    this.userId = userId;
    this.interestId = interestId;
    this.type = type;
    lastUpdate = now;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null)
      return false;
    if (!(other instanceof Interest))
      return false;
    Interest that = (Interest) other;
    return this.id.equals(that.id) && this.interestId.equals(that.interestId)
        && this.type.equals(that.type) && this.lastUpdate.equals(that.lastUpdate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, interestId, type, lastUpdate);
  }
}
