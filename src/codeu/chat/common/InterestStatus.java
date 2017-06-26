package codeu.chat.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;
import codeu.chat.util.Uuid;

public class InterestStatus {

  public static final Serializer<InterestStatus> SERIALIZER = new Serializer<InterestStatus>() {
    public void write(OutputStream out, InterestStatus value) throws IOException {
      Uuid.SERIALIZER.write(out, value.id);
      Serializers.INTEGER.write(out, value.unreadMessages);
      Serializers.nullable(Serializers.collection(Serializers.STRING))
          .write(out, value.newConversations);
      Serializers.nullable(Serializers.collection(Serializers.STRING))
          .write(out, value.addedConversations);
      Type.SERIALIZER.write(out, value.type);
      Serializers.STRING.write(out, value.name);
    }

    public InterestStatus read(InputStream in) throws IOException {
      Uuid id = Uuid.SERIALIZER.read(in);
      int unreadMessages = Serializers.INTEGER.read(in);
      List<String> newConversations = (List<String>) Serializers
          .nullable(Serializers.collection(Serializers.STRING)).read(in);
      List<String> addedConversations = (List<String>) Serializers
          .nullable(Serializers.collection(Serializers.STRING)).read(in);
      Type type = Type.SERIALIZER.read(in);
      String name = Serializers.STRING.read(in);
      return new InterestStatus(id,
                                unreadMessages,
                                newConversations,
                                addedConversations,
                                type,
                                name);
    }
  };

  // The ID of the interest status and the interest itself
  public final Uuid id;
  // Number of unread messages. -1 if type isn't CONVERSATION
  public final int unreadMessages;
  // The conversations created by the user interest. Null if type is not USER
  public final List<String> newConversations;
  // The conversations the user interest sent messages to. Null if type is not
  // USER
  public final List<String> addedConversations;
  // The type of the interest.
  public final Type type;
  // The name of the conversation or the User
  public final String name;

  public InterestStatus(Uuid id, int unreadMessages, List<String> newConversations,
      List<String> addedConversations, Type type, String name) {
    this.id = id;
    this.unreadMessages = unreadMessages;
    this.newConversations = newConversations;
    this.addedConversations = addedConversations;
    this.type = type;
    this.name = name;
  }

  public InterestStatus(Uuid id, int unreadMessages, String name) {
    this(id, unreadMessages, null, null, Type.CONVERSATION, name);
  }

  public InterestStatus(Uuid id, List<String> newConversations, List<String> addedConversations,
      String name) {
    this(id, -1, newConversations, addedConversations, Type.USER, name);
  }
}
