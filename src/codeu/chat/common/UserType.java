package codeu.chat.common;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public enum UserType {
  CREATOR(0),
  OWNER(1),
  MEMBER(2),
  NOTSET(3);

  public int fId;

  UserType(int id) {
    fId = id;
  }

  public static final Serializer<UserType> SERIALIZER =
      new Serializer<UserType>() {
        @Override
        public void write(OutputStream out, UserType value) throws IOException {
          Serializers.INTEGER.write(out, value.fId);
        }

        @Override
        public UserType read(InputStream in) throws IOException {
          return UserType.fromId(Serializers.INTEGER.read(in));
        }
      };

  public static UserType fromId(int id) {
    return values()[id];
  }

  public static int fromType(UserType ut) {
    return ut.fId;
  }

  // Returns the level comparison of two UserType(s).
  // Zero means that the two levels are equal.
  // Positive means that ut1 has a higher level than ut2.
  // Negative means that ut2 has a higher level than ut1.
  public static int levelCompare(UserType ut1, UserType ut2) {
    return fromType(ut2) - fromType(ut1);
  }
}
