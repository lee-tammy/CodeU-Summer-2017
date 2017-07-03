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

import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import codeu.chat.common.BasicController;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Interest;
import codeu.chat.common.InterestStatus;
import codeu.chat.common.Message;
import codeu.chat.common.RandomUuidGenerator;
import codeu.chat.common.RawController;
import codeu.chat.common.Type;
import codeu.chat.common.User;
import codeu.chat.common.UserType;
import codeu.chat.util.Logger;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import codeu.chat.util.ServerLog;

public final class Controller implements RawController, BasicController {

  private final static Logger.Log LOG = Logger.newLog(Controller.class);

  private final Model model;
  private final Uuid.Generator uuidGenerator;

  private PrintWriter output;
  private static boolean writeToLog;

  public Controller(Uuid serverId, Model model) {
    this.model = model;
    this.uuidGenerator = new RandomUuidGenerator(serverId, System.currentTimeMillis());
    try {
      output = new PrintWriter(new BufferedWriter(new FileWriter(ServerLog.createFilePath(), true)));
      output.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public User userById(Uuid id) {
    return model.userById().first(id);
  }

  @Override
  public Message newMessage(Uuid author, Uuid conversation, String body) {
    return newMessage(createId(), author, conversation, body, Time.now());
  }

  @Override
  public User newUser(String name) {
    return newUser(createId(), name, Time.now());
  }

  @Override
  public ConversationHeader newConversation(String title, Uuid owner) {
    return newConversation(createId(), title, owner, Time.now());
  }

  @Override
  public Message newMessage(Uuid id,
                            Uuid author,
                            Uuid conversation,
                            String body,
                            Time creationTime) {

    final User foundUser = model.userById().first(author);
    final ConversationPayload foundConversation = model.conversationPayloadById()
        .first(conversation);

    Message message = null;

    if (foundUser != null && foundConversation != null && isIdFree(id)) {

      message = new Message(id,
                            Uuid.NULL,
                            foundConversation.lastMessage,
                            creationTime,
                            author,
                            body,
                            conversation);
      model.add(message);
      LOG.info("Message added: %s", message.id);

      // Find and update the previous "last" message so that it's "next" value
      // will point to the new message.

      if (Uuid.equals(foundConversation.lastMessage, Uuid.NULL)) {

        // The conversation has no messages in it, that's why the last message
        // is NULL (the first
        // message should be NULL too. Since there is no last message, then it
        // is not possible
        // to update the last message's "next" value.
        foundConversation.firstMessage = message.id;
        foundConversation.lastMessage = message.id;

      } else {
        final Message lastMessage = model.messageById().first(foundConversation.lastMessage);
        lastMessage.next = message.id;
      }

      // If the first message points to NULL it means that the conversation was
      // empty and that
      // the first message should be set to the new message. Otherwise the
      // message should
      // not change.

      foundConversation.firstMessage =
          Uuid.equals(foundConversation.firstMessage, Uuid.NULL) ?
          message.id :
          foundConversation.firstMessage;

      // Update the conversation to point to the new last message as it has changed.

      foundConversation.lastMessage = message.id;
    }
    
    if (writeToLog) {
      output.println("M_" + author + "_" + id + "_" + conversation +  "_" +
            creationTime + "_" + body);
      output.flush();
    }

    return message;
  }

  @Override
  public User newUser(Uuid id, String name, Time creationTime) {

    User user = null;

    if (isIdFree(id)) {

      model.add(user);

      LOG.info(
          "newUser success (user.id=%s user.name=%s user.time=%s user.ut=%s)",
          id,
          name,
          creationTime);

    } else {

      LOG.info(
          "newUser fail - id in use (user.id=%s user.name=%s user.time=%s)",
          id,
          name,
          creationTime);
    }
    
    if (writeToLog) {
      output.println("U_" + name + "_" + user.id + "_" + creationTime);
      output.flush();
    }

    return user;
  }

  @Override
  public ConversationHeader newConversation(Uuid id, String title, Uuid owner, Time creationTime) {

    final User foundOwner = model.userById().first(owner);

    ConversationHeader conversation = null;

    if (foundOwner != null && isIdFree(id)) {
      conversation = new ConversationHeader(id, owner, creationTime, title);
      model.add(conversation);
      LOG.info("Conversation added: " + id);
    }

    return conversation;
  }

  public Interest addInterest(Uuid userId, Uuid interestId, Type interestType) {
    return addInterest(userId, userId, interestId, interestType, Time.now());
  }

  public Interest addInterest(Uuid id,
                              Uuid userId,
                              Uuid interestId,
                              Type interestType,
                              Time creationTime) {
    return model.addInterest(id, userId, interestId, interestType, creationTime);
  }

  public void removeInterest(Uuid userId, Uuid interestId) {
    model.removeInterest(userId, interestId);
  }

  // TODO(Adam): Create and send an ArrayList of InterestStatus objects instead
  // of an array
  // list of strings. Allow the client to figure out how to sort through the
  // data.
  public List<InterestStatus> interestStatus(Uuid user) {
    List<Uuid> userInterests = model.interests.get(user);
    List<InterestStatus> result = new ArrayList<>();
    Time now = Time.now();
    if (userInterests == null)
      return result;
    for (Uuid interestId : userInterests) {
      InterestStatus report = processInterest(interestId, now);
      if (report != null) {
        result.add(report);
      }
    }
    return result;
  }

  /*
   * Syntax guide:
   *
   * if type is User: U <name>: C <header title> --> for conversations created
   * by the user U <name>: A <header title> --> for messages added to
   * conversation by the user. if type is Conversation C <name>: <number of new
   * messages>
   */
  private InterestStatus processInterest(Uuid id, Time now) {
    Interest interest = model.interestById().first(id);
    if (interest == null)
      return null;
    Time lastUpdate = interest.lastUpdate;
    InterestStatus result = null;
    if (interest.type == Type.USER) {
      User user = model.userById().first(id);
      // Return all values with time higher than last Update
      Iterable<ConversationHeader> headers = model.conversationByTime().after(lastUpdate);
      List<String> createdConversations = new ArrayList<>();
      for (ConversationHeader header : headers) {
        if (header.creator.equals(user.id)) {
          createdConversations.add(header.title);
        }
      }

      // Return all values with time higher than last Update
      Iterable<Message> messages = model.messageByTime().after(lastUpdate);
      List<String> addedConversations = new ArrayList<>();
      for (Message message : messages) {
        if (message.author.equals(user.id)) {
          String conversation = model.conversationById().first(message.conversationHeader).title;
          if (!addedConversations.contains(conversation)) {
            addedConversations.add(conversation);
          }
        }
      }

      result = new InterestStatus(id, createdConversations, addedConversations, user.name);
    } else if (interest.type == Type.CONVERSATION) {
      ConversationPayload payload = model.conversationPayloadById().first(id);
      String title = model.conversationById().first(id).title;
      Message last = model.messageById().first(payload.lastMessage);
      int total = 0;
      while (last != null && last.creation.compareTo(lastUpdate) > 0) {
        total++;
        last = model.messageById().first(last.previous);
      }
      result = new InterestStatus(id, total, title);
    }
    interest.lastUpdate = now;
    return result;
  }

  private Uuid createId() {

    Uuid candidate;

    for (candidate = uuidGenerator.make();
         isIdInUse(candidate);
         candidate = uuidGenerator.make()) {

     // Assuming that "randomUuid" is actually well implemented, this
     // loop should never be needed, but just in case make sure that the
     // Uuid is not actually in use before returning it.

    }

    return candidate;
  }

  private boolean isIdInUse(Uuid id) {
    return model.messageById().first(id) != null ||
           model.conversationById().first(id) != null ||
           model.userById().first(id) != null;
  }

  private boolean isIdFree(Uuid id) { return !isIdInUse(id); }

  /**
   * Getter method for writeToLog
   * @return boolean if we should write to log or no
   */
  public static boolean getWriteToLog() {
    return writeToLog;
  }

  /**
   * Setter method for writeToLOg
   * @param write the new update for writeToLog
   */
  public static void setWriteToLog(boolean write) {
    writeToLog = write;
  }
  
    public ConversationHeader conversationHeaderById(Uuid id) {
    return model.conversationById().first(id);
  }

}
