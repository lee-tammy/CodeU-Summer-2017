package codeu.chat.common;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public enum Type {
  USER(0), CONVERSATION(1);

  public final int fId;

  Type(int id) {
    fId = id;
  }

  public static final Serializer<Type> SERIALIZER = new Serializer<Type>() {
    @Override
    public void write(OutputStream out, Type value) throws IOException {
      Serializers.INTEGER.write(out, value.fId);
    }

    @Override
    public Type read(InputStream in) throws IOException {
      return Type.fromId(Serializers.INTEGER.read(in));
    }
  };

  public static Type fromId(int id) {
    return values()[id];
  }

  public static int fromType(Type t) {
    return t.fId;
  }
}
