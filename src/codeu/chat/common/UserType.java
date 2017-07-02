package codeu.chat.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;

public enum UserType {
  CREATOR(0), OWNER(1), MEMBER(2), NOTSET(3);

  public int fId;

  UserType(int id) {
	fId = id;
  }
  
  public static final Serializer<UserType> SERIALIZER = new Serializer<UserType>() {
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
}