// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package codeu.chat.server;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.ConversationPermission;
import codeu.chat.common.Interest;
import codeu.chat.common.Message;
import codeu.chat.common.InterestType;
import codeu.chat.common.User;
import codeu.chat.common.UserType;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import codeu.chat.util.store.Store;
import codeu.chat.util.store.StoreAccessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public final class Model {

  private static final Comparator<Uuid> UUID_COMPARE =
      new Comparator<Uuid>() {

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

  private static final Comparator<Time> TIME_COMPARE =
      new Comparator<Time>() {
        @Override
        public int compare(Time a, Time b) {
          return a.compareTo(b);
        }
      };

  private static final Comparator<String> STRING_COMPARE = String.CASE_INSENSITIVE_ORDER;
  private final Store<Uuid, User> userById = new Store<>(UUID_COMPARE);
  private final Store<Time, User> userByTime = new Store<>(TIME_COMPARE);
  private final Store<String, User> userByText = new Store<>(STRING_COMPARE);
  private final List<User> users = new ArrayList<User>();

  private final Store<Uuid, ConversationHeader> conversationById = new Store<>(UUID_COMPARE);
  private final Store<Time, ConversationHeader> conversationByTime = new Store<>(TIME_COMPARE);
  private final Store<String, ConversationHeader> conversationByText = new Store<>(STRING_COMPARE);
  private final List<ConversationHeader> conversations = new ArrayList<ConversationHeader>();

  private final Store<Uuid, ConversationPayload> conversationPayloadById =
      new Store<>(UUID_COMPARE);
  private final List<ConversationPayload> payloads = new ArrayList<ConversationPayload>();

  private final Store<Uuid, Message> messageById = new Store<>(UUID_COMPARE);
  private final Store<Time, Message> messageByTime = new Store<>(TIME_COMPARE);
  private final Store<String, Message> messageByText = new Store<>(STRING_COMPARE);
  private final List<Message> messages = new ArrayList<Message>();

  private final Store<Uuid, Interest> interestById = new Store<>(UUID_COMPARE);
  private final List<Interest> interestList = new ArrayList<Interest>();

  private final Store<Uuid, ConversationPermission> permissionById = new Store<>(UUID_COMPARE);
  private final List<ConversationPermission> permissions = new ArrayList<ConversationPermission>();

  public Map<Uuid, ArrayList<Uuid>> interests = new HashMap<>();
  private static final int LOG_SIZE = 6; // number of elements stored in the log
  private boolean restoredLog = false;
  
  private final Type userType = new TypeToken<ArrayList<User>>(){}.getType();
  private final Type conversationType = new TypeToken<ArrayList<ConversationHeader>>(){}.getType();
  private final Type permissionType = new TypeToken<ArrayList<ConversationPermission>>(){}.getType();
  private final Type payloadType = new TypeToken<ArrayList<ConversationPayload>>(){}.getType();
  private final Type messageType = new TypeToken<ArrayList<Message>>(){}.getType();
  private final Type interestType = new TypeToken<ArrayList<Interest>>(){}.getType();
  
  public void add(User user) {
    userById.insert(user.id, user);
    userByTime.insert(user.creation, user);
    userByText.insert(user.name, user);
    users.add(user);
  }

  public StoreAccessor<Uuid, User> userById() {
    return userById;
  }

  public StoreAccessor<Time, User> userByTime() {
    return userByTime;
  }

  public StoreAccessor<Uuid, Interest> interestById() {
    return interestById;
  }

  public StoreAccessor<String, User> userByText() {
    return userByText;
  }

  public void add(ConversationHeader conversation, ConversationPermission permission) {
    conversationById.insert(conversation.id, conversation);
    conversationByTime.insert(conversation.creation, conversation);
    conversationByText.insert(conversation.title, conversation);
    conversations.add(conversation);
    ConversationPayload payload = new ConversationPayload(conversation.id);
    conversationPayloadById.insert(conversation.id, payload);
    payloads.add(payload);
    permissionById.insert(permission.id, permission);
    permissions.add(permission);
  }
  
  public void add(ConversationHeader conversation) {
	conversationById.insert(conversation.id, conversation);
	conversationByTime.insert(conversation.creation, conversation);
	conversationByText.insert(conversation.title, conversation);
	conversations.add(conversation);
  }
  
  public void add(ConversationPermission permission) {
	permissionById.insert(permission.id, permission);
	permissions.add(permission);
  }
  
  public void add(ConversationPayload payload) {
	conversationPayloadById.insert(payload.id, payload);
	payloads.add(payload);
  }

  public StoreAccessor<Uuid, ConversationHeader> conversationById() {
    return conversationById;
  }

  public StoreAccessor<Time, ConversationHeader> conversationByTime() {
    return conversationByTime;
  }

  public StoreAccessor<String, ConversationHeader> conversationByText() {
    return conversationByText;
  }

  public StoreAccessor<Uuid, ConversationPayload> conversationPayloadById() {
    return conversationPayloadById;
  }

  public void add(Message message) {
    messageById.insert(message.id, message);
    messageByTime.insert(message.creation, message);
    messageByText.insert(message.content, message);
    messages.add(message);
  }

  public StoreAccessor<Uuid, Message> messageById() {
    return messageById;
  }

  public StoreAccessor<Time, Message> messageByTime() {
    return messageByTime;
  }

  public StoreAccessor<String, Message> messageByText() {
    return messageByText;
  }

  public StoreAccessor<Uuid, ConversationPermission> permissionById() {
    return permissionById;
  }

  public Interest addInterest(
      Uuid id, Uuid userId, Uuid interestId, InterestType interestType, Time creationTime) {
    Interest newInterest = new Interest(id, userId, interestId, interestType, creationTime);
    if (interests.get(userId) == null) {
      interests.put(userId, new ArrayList<Uuid>());
    }
    interests.get(userId).add(interestId);
    interestById.insert(interestId, newInterest);
    interestList.add(newInterest);
    return newInterest;
  }

  public void removeInterest(Uuid userId, Uuid interestId) {
    if (interests.get(userId) != null) {
      interests.get(userId).remove(interestId);
    }
  }
  
  public boolean getRestoredLog() {
	return restoredLog;
  }
  
  public void setRestoredLog(boolean restoredLog) {
	this.restoredLog = restoredLog;
  }
  
  public void refresh(File file) { 
	// clear the current contents of the log
    try {
      FileWriter fwOb = new FileWriter(file.getAbsolutePath(), false);
      PrintWriter pwOb = new PrintWriter(fwOb, false);
      pwOb.flush();
      pwOb.close();
      fwOb.close();
	} catch (IOException e1) {
      e1.printStackTrace();
      System.out.println("Failed to clear contents of log!");
	} 
	
	// create gson object for serializing
	GsonBuilder gb = new GsonBuilder();
	Type mapType = new TypeToken<Map<Uuid, UserType>>(){}.getType();
	gb.registerTypeAdapter(mapType, new PermissionAdapter());
	Gson gson = gb.create();
	  
	try {
	  FileWriter fw = new FileWriter(file.getAbsolutePath(), true);
	  
	  // take a snapshot of the users, conversations, messages, etc.  
	  fw.write(gson.toJson(users, userType) + "\n");
	  
	  fw.write(gson.toJson(conversations, conversationType) + "\n");
	  
	  fw.write(gson.toJson(permissions, permissionType) + "\n");
	  
	  fw.write(gson.toJson(messages, messageType) + "\n");
	  
	  fw.write(gson.toJson(interestList, interestType) + "\n");	
	  
	  fw.write(gson.toJson(payloads, payloadType));
	  
	  fw.flush();
	  fw.close();
	  
	  } catch (IOException e2) {
		e2.printStackTrace();
		System.out.println("Failed to save data to log!");
	  }
  }
  
  public boolean restore(File file) {
	// check to see if there is a log to restore from 
	if(!file.exists())
	  return true;	
	
	BufferedReader br = null;
	try {
	  br = new BufferedReader(new FileReader(file.getAbsolutePath()));
	} catch (FileNotFoundException e) {
	  e.printStackTrace();
	  return false;
	}
	
	String completeText = "";
	String line;
	
	// read the entirety of file into one String 
	try {
      while((line = br.readLine()) != null) {
    	  completeText += line + "\n";
      }
      br.close();
	} catch (IOException e) {
	  e.printStackTrace();
	  return false;
	}
	String[] jsonObjs = completeText.split("\n");
	
	if (jsonObjs.length != LOG_SIZE) {
      System.out.println("ERROR: incorrect number of elements in log");
      System.out.println("Expected: " + LOG_SIZE + "\t Actual: " + jsonObjs.length);
      return false;
	}
	
	// create gson object for deserializing
	GsonBuilder gb = new GsonBuilder();
	Type mapType = new TypeToken<Map<Uuid, UserType>>(){}.getType();
	gb.registerTypeAdapter(mapType, new PermissionAdapter());
	Gson gson = gb.create();
	  
	// convert from json to respective Store objects
	List<User> restoredUsers = gson.fromJson(jsonObjs[0].trim(), userType);
	List<ConversationHeader> restoredConvos = gson.fromJson(jsonObjs[1].trim(), conversationType);
	List<ConversationPermission> restoredPermissions = gson.fromJson(jsonObjs[2].trim(), permissionType);
	List<Message> restoredMessages = gson.fromJson(jsonObjs[3].trim(), messageType);
	List<Interest> restoredInterests = gson.fromJson(jsonObjs[4].trim(), interestType);
	List<ConversationPayload> restoredPayloads = gson.fromJson(jsonObjs[5].trim(), payloadType);
	  
	// restore users
	for(User user : restoredUsers) {
	  add(user);
	}
	  
	// restore conversations
	for(ConversationHeader ch : restoredConvos) {
	  add(ch);
	}
	  
	// restore permissions
	for(ConversationPermission cp : restoredPermissions) {
	  add(cp);
	}
	  
	// restore payloads
	for(ConversationPayload cp : restoredPayloads) {
	  add(cp);
	}
	  
	// restore messages
	for(Message message : restoredMessages) {
	  add(message);
	}
	  
	// restore interests
	for(Interest interest : restoredInterests) {
	  addInterest(interest.id, interest.userId, interest.interestId, 
                  interest.type, interest.lastUpdate);
	}
	  
	System.out.println("Sucessfully restored previous state of the Server");
	  
	return true;
  }
  
  public String createFilePath() {
	String workingDirectory = System.getProperty("user.dir");
	return workingDirectory + File.separator + "serverLog.txt";
  }
  
  public class PermissionAdapter extends TypeAdapter<Map<Uuid,UserType>> {
	
	@Override
	public Map<Uuid,UserType> read(JsonReader reader) throws IOException {
	  if (reader.peek() == JsonToken.NULL) {
	    reader.nextNull();
	    return null;
	  }
	  String hashMap = reader.nextString();
	  String[] mapArray = hashMap.split("_");
	  Map<Uuid, UserType> map = new HashMap<Uuid, UserType>();
	  for(int i = 0; i < mapArray.length; i++) {
	    String[] values = mapArray[i].split(",");
	    Uuid uuid = Uuid.parse(values[0]);
		UserType ut = UserType.fromString(values[1]);
		map.put(uuid, ut);
	  }
	  return map;
	  }
	
	@Override
	public void write(JsonWriter writer, Map<Uuid,UserType> map) throws IOException {
	  if (map == null) {
	    writer.nullValue();
	    return;
	  }
	  String hashMap = "";
	  Set<Uuid> keys = map.keySet();
	  
	  for(Uuid key : keys) {
		hashMap+= key + "," + map.get(key) + "_";
	  }
	  // want to trim off the extraneous "_" at the end
	  writer.value(hashMap.substring(0, hashMap.length() - 1));
	}
  }
}
