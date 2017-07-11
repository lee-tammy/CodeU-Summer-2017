package codeu.chat.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;
import codeu.chat.common.UserType;
import codeu.chat.util.Uuid;

public class ConversationPermission {
  private Uuid cpID;
  private Uuid creator;
  private Map<Uuid, UserType> users;

  public static final Serializer<ConversationPermission> SERIALIZER =
  new Serializer<ConversationPermission>() {
    public void write(OutputStream out, ConversationPermission value) throws IOException {
      Uuid.SERIALIZER.write(out, value.cpID);
      Uuid.SERIALIZER.write(out, value.creator);
      Serializers.map(Uuid.SERIALIZER, UserType.SERIALIZER).write(out, value.users);
    }

    public ConversationPermission read(InputStream in) throws IOException {
      Uuid cpID = Uuid.SERIALIZER.read(in);
      Uuid creator = Uuid.SERIALIZER.read(in);
      Map<Uuid, UserType> users = Serializers.map(Uuid.SERIALIZER,
          UserType.SERIALIZER).read(in);
      return new ConversationPermission(cpID, creator);
    }
  };

  // creator is 0, owner is 1, member is 2
  public ConversationPermission(Uuid creator, Uuid cpID) {
    this.creator = creator;
    this.cpID = cpID;
    users = new HashMap<>();
    users.put(creator, UserType.CREATOR);
  }

  public ConversationPermission(Uuid creator, Uuid cpId, Map<Uuid, UserType> users) {
    this.creator = creator;
    this.cpID = cpID;
    this.users = users;
  }

  public UserType returnStatus(Uuid user) {
    return users.get(user);
  }

  public void addOwner(Uuid user) {
    users.put(user, UserType.OWNER);
  }

  public void addMember(Uuid user) {
    users.put(user, UserType.MEMBER);
  }

  public void remove(Uuid user) {
    users.remove(user);
  }

  public void removeOwnership(Uuid user) {
    users.put(user, UserType.MEMBER);
  }

  public Uuid CreatorUuid() {
    return creator;
  }

  // returns ID of the Conversation Permissions
  public Uuid getID() {
    return cpID;
  }
}
