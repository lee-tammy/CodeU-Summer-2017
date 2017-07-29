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
import codeu.chat.util.ServerLog;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import codeu.chat.util.store.Store;
import codeu.chat.util.store.StoreAccessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

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

  private final Store<Uuid, ConversationHeader> conversationById = new Store<>(UUID_COMPARE);
  private final Store<Time, ConversationHeader> conversationByTime = new Store<>(TIME_COMPARE);
  private final Store<String, ConversationHeader> conversationByText = new Store<>(STRING_COMPARE);

  private final Store<Uuid, ConversationPayload> conversationPayloadById =
      new Store<>(UUID_COMPARE);

  private final Store<Uuid, Message> messageById = new Store<>(UUID_COMPARE);
  private final Store<Time, Message> messageByTime = new Store<>(TIME_COMPARE);
  private final Store<String, Message> messageByText = new Store<>(STRING_COMPARE);

  private final Store<Uuid, Interest> interestById = new Store<>(UUID_COMPARE);

  private final Store<Uuid, ConversationPermission> permissionById = new Store<>(UUID_COMPARE);

  public final Map<Uuid, ArrayList<Uuid>> interests = new HashMap<>();
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
	  String users = gson.toJson(userById);
	  if (!users.isEmpty()) { 
		fw.write(users);
	    fw.write("/n");
	    fw.write("___");
	    fw.write("/n");
	  }
	  
	  String conversations = gson.toJson(conversationById);
	  if (!conversations.isEmpty()) {
		fw.write(conversations);
	    fw.write("/n");
	    fw.write("___");
	    fw.write("/n");
	  }  
	  
	  String permissions = gson.toJson(permissionById);
	  if (!permissions.isEmpty()) {
	    fw.write(permissions);
	    fw.write("/n");
	    fw.write("___");
	    fw.write("/n");
	  }
	  
	  String messages = gson.toJson(messageById);
	  if (!messages.isEmpty()) {
		fw.write(messages);
	    fw.write("/n");
	    fw.write("___");
	    fw.write("/n");
	  }
	  
      String interests = gson.toJson(interestById);
	  if (!interests.isEmpty()) {
		fw.write(interests);
	    fw.write("/n");
	    fw.write("___");
	    fw.write("/n");
	  }	
	  
	  fw.flush();
	  fw.close();
	  
	  } catch (IOException e2) {
		e2.printStackTrace();
		System.out.println("Failed to save data to log!");
	  }
  }
  
  public void restore(File file) {
	// check to see if there is a log to restore from 
	if(!file.exists())
	  return;	
	
	// create a JSON reader to parse the log
	try {
	  JsonReader reader = new JsonReader(new FileReader(file.getAbsolutePath()));
	} catch (FileNotFoundException e) {
	  e.printStackTrace();
	}
	
//	Store[] stores = new Gson().fromJson(json, Store.class);
	
  }
}
