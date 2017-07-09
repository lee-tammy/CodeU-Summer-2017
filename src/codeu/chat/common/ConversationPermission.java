package codeu.chat.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import codeu.chat.util.Serializer;
import codeu.chat.util.Uuid;

public class ConversationPermission {
  private Uuid cpID;
  private Uuid creator;
  private HashMap<Uuid, Integer> users;
	
  // TODO: properly implement Serializer for ConversationPermission. Don't think this is correct
  public static final Serializer<ConversationPermission> SERIALIZER = new Serializer<ConversationPermission>() {
    public void write(OutputStream out, ConversationPermission value) throws IOException {
	  Uuid.SERIALIZER.write(out, value.cpID);
	  Uuid.SERIALIZER.write(out, value.creator);
	}

	public ConversationPermission read(InputStream in) throws IOException {
	  Uuid cpID = Uuid.SERIALIZER.read(in);
	  Uuid creator = Uuid.SERIALIZER.read(in);
	  return new ConversationPermission(cpID, creator);
	}
  };
  
  // creator is 3, owner is 2, member is 1
  public ConversationPermission(Uuid creator, Uuid cpID) {
	this.cpID = cpID;
	users.put(creator, 3);
	this.creator = creator;
  }
  
  public int returnStatus(Uuid user) {
	return users.get(user);
  }
  
  public void addOwner(Uuid user) {
    users.put(user, 2);
  }
  
  public void addMember(Uuid user) {
	users.put(user, 1);
  }
  
  public void removeOwner(Uuid user) {
	users.remove(user);
  }
  
  public void removeMember(Uuid user) {
	users.remove(user);
  }
  
  public Uuid CreatorUuid() {
	return creator;
  }
  // returns ID of the Conversation Permissions
  public Uuid getID() {
	return cpID;
  }

}
