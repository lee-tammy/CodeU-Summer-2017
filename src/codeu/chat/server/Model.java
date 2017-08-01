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
import codeu.chat.common.Type;
import codeu.chat.common.User;
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
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

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
  private Store<Uuid, User> userById = new Store<>(UUID_COMPARE);
  private final Store<Time, User> userByTime = new Store<>(TIME_COMPARE);
  private final Store<String, User> userByText = new Store<>(STRING_COMPARE);

  private Store<Uuid, ConversationHeader> conversationById = new Store<>(UUID_COMPARE);
  private final Store<Time, ConversationHeader> conversationByTime = new Store<>(TIME_COMPARE);
  private final Store<String, ConversationHeader> conversationByText = new Store<>(STRING_COMPARE);

  private Store<Uuid, ConversationPayload> conversationPayloadById =
      new Store<>(UUID_COMPARE);

  private Store<Uuid, Message> messageById = new Store<>(UUID_COMPARE);
  private final Store<Time, Message> messageByTime = new Store<>(TIME_COMPARE);
  private final Store<String, Message> messageByText = new Store<>(STRING_COMPARE);

  private Store<Uuid, Interest> interestById = new Store<>(UUID_COMPARE);

  private Store<Uuid, ConversationPermission> permissionById = new Store<>(UUID_COMPARE);

  public Map<Uuid, ArrayList<Uuid>> interests = new HashMap<>();
  private int logSize = 7; // number of elements stored in the log
  private final Gson gson = new GsonBuilder().create();
  
  public void add(User user) {
    userById.insert(user.id, user);
    userByTime.insert(user.creation, user);
    userByText.insert(user.name, user);
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
    conversationPayloadById.insert(conversation.id, new ConversationPayload(conversation.id));
    permissionById.insert(permission.id, permission);
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
      Uuid id, Uuid userId, Uuid interestId, Type interestType, Time creationTime) {
    Interest newInterest = new Interest(id, interestId, interestType, creationTime);
    if (interests.get(userId) == null) {
      interests.put(userId, new ArrayList<Uuid>());
    }
    interests.get(userId).add(interestId);
    interestById.insert(interestId, newInterest);
    return newInterest;
  }

  public void removeInterest(Uuid userId, Uuid interestId) {
    if (interests.get(userId) != null) {
      interests.get(userId).remove(interestId);
    }
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
	  
	try {
	  FileWriter fw = new FileWriter(file.getAbsolutePath(), true);
	  
	  // take a snapshot of the users, conversations, messages, etc.  
	  fw.write(gson.toJson(userById) + "\n");
	  
	  fw.write(gson.toJson(conversationById) + "\n");
	  
	  fw.write(gson.toJson(permissionById) + "\n");
	  
	  fw.write(gson.toJson(messageById) + "\n");
	  
	  fw.write(gson.toJson(interestById) + "\n");	
	  
	  fw.write(gson.toJson(conversationPayloadById) + "\n");
	  
	  fw.write(gson.toJson(interests));
	  
	  fw.flush();
	  fw.close();
	  
	  } catch (IOException e2) {
		e2.printStackTrace();
		System.out.println("Failed to save data to log!");
	  }
  }
  
  @SuppressWarnings("unchecked")
  public void restore(File file) {
	// check to see if there is a log to restore from 
	if(!file.exists())
	  return;	
	
	BufferedReader br = null;
	try {
	  br = new BufferedReader(new FileReader(file.getAbsolutePath()));
	} catch (FileNotFoundException e) {
	  e.printStackTrace();
	}
	
	String completeText = "";
	String line;
	
	// read the entirety of file into one String 
	try {
      while((line = br.readLine()) != null) {
    	  completeText += line + "\n";
      }
	} catch (IOException e) {
		e.printStackTrace();
	}
	
	String[] jsonObjs = completeText.split("\n");
	
	if (jsonObjs.length == logSize) {
	  // convert from json to respective Store objects
	  JsonParser parser = new JsonParser();
	  JsonArray array = parser.parse(jsonObjs[0]).getAsJsonArray();
	  userById = gson.fromJson(jsonObjs[0], Store.class);
	  conversationById = gson.fromJson(jsonObjs[1], Store.class);
	  permissionById = gson.fromJson(jsonObjs[2], Store.class);
	  messageById = gson.fromJson(jsonObjs[3], Store.class);
	  interestById = gson.fromJson(jsonObjs[4], Store.class);
	  conversationPayloadById = gson.fromJson(jsonObjs[5], Store.class);
	  interests = gson.fromJson(jsonObjs[6], HashMap.class);
	  
	  // restore users
	  ArrayList<Uuid> userId = userById.getKeys();
	  for(int i = 0; i < userId.size(); i++) {
	    User temp = userById.first(userId.get(i));
	    userByTime.insert(temp.creation, temp);
	    userByText.insert(temp.name, temp);
	  }
	  
	  // restore conversations
	  ArrayList<Uuid> conversationId = conversationById.getKeys();
	  for(int i = 0; i < conversationId.size(); i++) {
	    ConversationHeader temp = conversationById.first(conversationId.get(i));
	    conversationByTime.insert(temp.creation, temp);
	    conversationByText.insert(temp.title, temp);
	  }
	  
	  // restore messages
	  ArrayList<Uuid> messageId = messageById.getKeys();
	  for(int i = 0; i < messageId.size(); i++) {
	    Message temp = messageById.first(messageId.get(i));
	    messageByTime.insert(temp.creation, temp);
	    messageByText.insert(temp.content, temp);
	  }
	} else {
	  System.out.println("ERROR: incorrect number of elements in log");
	  System.out.println("Expected: " + logSize + "\t Actual: " + jsonObjs.length);
	}
  }
  public class ComparatorInstanceCreator implements InstanceCreator<Comparator<Object>> {
	@Override
	public Comparator<Object> createInstance(java.lang.reflect.Type arg0) {
	  // TODO Auto-generated method stub
	  return null;
	}
  }
}
