package codeu.chat.common;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;
import codeu.chat.util.Uuid;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ConversationPermission {
  public final Uuid id;
  public final Uuid creator;
  public final UserType defaultAccess;
  private Map<Uuid, UserType> users;

  public static final Serializer<ConversationPermission> SERIALIZER =
      new Serializer<ConversationPermission>() {
        public void write(OutputStream out, ConversationPermission value) throws IOException {
          Uuid.SERIALIZER.write(out, value.id);
          Uuid.SERIALIZER.write(out, value.creator);
          Serializers.map(Uuid.SERIALIZER, UserType.SERIALIZER).write(out, value.users);
          UserType.SERIALIZER.write(out, value.defaultAccess);
        }

        public ConversationPermission read(InputStream in) throws IOException {
          Uuid id = Uuid.SERIALIZER.read(in);
          Uuid creator = Uuid.SERIALIZER.read(in);
          Map<Uuid, UserType> users =
              Serializers.map(Uuid.SERIALIZER, UserType.SERIALIZER).read(in);
          UserType defaultAccess = UserType.SERIALIZER.read(in);
          return new ConversationPermission(id, creator, defaultAccess);
        }
      };

  // creator is 0, owner is 1, member is 2
  public ConversationPermission(Uuid id, Uuid creator, UserType defaultAccess) {
    this.id = id;
    this.creator = creator;
    this.defaultAccess = defaultAccess;
    users = new HashMap<>();
    users.put(creator, UserType.CREATOR);
  }

  public ConversationPermission(Uuid id, Uuid creator, Map<Uuid, UserType> users, UserType defaultAccess) {
    this.creator = creator;
    this.id = id;
    this.users = users;
    this.defaultAccess = defaultAccess;
  }

  public UserType status(Uuid user) {
    if (!users.containsKey(user)) {
      return UserType.NOTSET;
    }
    return users.get(user);
  }

  public void changeAccess(Uuid user, UserType newType) {
    users.put(user, newType);
  }
}
