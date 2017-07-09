package codeu.chat.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;
import codeu.chat.util.Uuid;
import codeu.chat.util.store.Store;

public class ConversationPermission {
  private static final Comparator<Uuid> UUID_COMPARE = new Comparator<Uuid>() {
	
	@Override
	public int compare(Uuid a, Uuid b) {

	  if (a == b) {
	    return 0;
	  }

	  if (a == null && b != null) {
	    return -1;
	  }

	  if (a != null && b == null) {
	    return 1;
	  }

	  final int order = Integer.compare(a.id(), b.id());
		return order == 0 ? compare(a.root(), b.root()) : order;
	  }
  };
  private Uuid cpID;
  private Store users = new Store(UUID_COMPARE);
	
  // TODO: properly implement Serializer for ConversationPermission
  public static final Serializer<ConversationPermission> SERIALIZER = new Serializer<ConversationPermission>() {
    public void write(OutputStream out, ConversationPermission value) throws IOException {
	  Uuid.SERIALIZER.write(out, value.cpID);
	}

	public ConversationPermission read(InputStream in) throws IOException {
	  Uuid id = Uuid.SERIALIZER.read(in);
	  return new ConversationPermission(id, id);
	}
  };
  
  // creator is 3, owner is 2, member is 1, default/undefined is 0
  public ConversationPermission(Uuid creator, Uuid cpID) {
	this.cpID = cpID;
	users.insert(creator, 3);
  }
  
  public int returnStatus(Uuid user) {
	return (int)users.first(user);
  }
  
  public void addOwner(Uuid user) {
    if(users.first(user) != null) {
      users.insert(user, null);
    }
    users.insert(user, 2);
  }
  
  public void addMember(Uuid user) {
	if(users.first(user) != null) {
	  users.insert(user, null);
	}
	  users.insert(user, 1);
  }
  
  public void removeOwner(Uuid user) {
	users.insert(user, null);
  }
  
  public void removeMember(Uuid user) {
	users.insert(user, null);
  }
  
  // returns ID of the Conversation Permissions
  public Uuid getID() {
	return cpID;
  }

}
