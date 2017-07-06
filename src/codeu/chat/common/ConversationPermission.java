package codeu.chat.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.List;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;
import codeu.chat.util.Uuid;

public class ConversationPermission {
  private Uuid cpID;
  private LinkedHashSet<Uuid> owners;
  private LinkedHashSet<Uuid> members;
  private final Uuid CREATOR;
	
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
  
  public ConversationPermission(Uuid creator, Uuid cpID) {
	this.cpID = cpID;
	this.CREATOR = creator;
	owners = new LinkedHashSet<Uuid>();
	members = new LinkedHashSet<Uuid>();
  }
  
  public boolean isOwner(Uuid user) {
	return owners.contains(user);
  }
  
  public boolean isMember(Uuid user) {
	return members.contains(user);
  }
  
  // returns false if user is already an Owner
  public boolean addOwner(Uuid user) {
    members.add(user);
	return owners.add(user); 
  }
  
  // returns false if user is already a Member
  public boolean addMember(Uuid user) {
	return members.add(user);
  }
  
  // returns false if user was not actually an Owner
  public boolean removeOwner(Uuid user) {
	members.remove(user);
	return owners.remove(user);
  }
  
  // returns false if user was not actually a Member
  public boolean removeMember(Uuid user) {
	return members.remove(user);
  }
  
  public Uuid getCreator() {
	return this.CREATOR;
  }
  
  // returns ID of the Conversation Permissions
  public Uuid getID() {
	return cpID;
  }

}
