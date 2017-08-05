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

  public final int fId;

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

  // this method is only for serialization purposes
  private static UserType fromId(int id) {
    UserType ut = null;
    try {
      ut = values()[id];
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.print("Error: invalid UserType int value");
      throw new IllegalArgumentException();
    }
    return ut;
  }

  public static UserType fromString(String ut) {
    switch (ut) {
      case "CREATOR":
        return UserType.CREATOR;
      case "OWNER":
        return UserType.OWNER;
      case "MEMBER":
        return UserType.MEMBER;
      case "NOTSET":
        return UserType.NOTSET;
      default:
        System.out.println("ERROR: invalid string");
        return null;
    }
  }

  // returns true if the user has manager access for the target UserType
  public static boolean hasManagerAccess(UserType user, UserType target) {
    return user.fId < target.fId;
  }
}
