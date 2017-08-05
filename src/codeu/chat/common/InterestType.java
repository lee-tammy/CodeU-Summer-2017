package codeu.chat.common;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public enum InterestType {
  USER(0), CONVERSATION(1);

  public final int fId;

  InterestType(int id) {
    fId = id;
  }

  public static final Serializer<InterestType> SERIALIZER = new Serializer<InterestType>() {
    @Override
    public void write(OutputStream out, InterestType value) throws IOException {
      Serializers.INTEGER.write(out, value.fId);
    }

    @Override
    public InterestType read(InputStream in) throws IOException {
      return InterestType.fromId(Serializers.INTEGER.read(in));
    }
  };

  public static InterestType fromId(int id) {
    return values()[id];
  }

  public static int fromType(InterestType t) {
    return t.fId;
  }
}
