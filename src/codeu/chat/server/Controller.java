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

import codeu.chat.common.BasicController;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.ConversationPermission;
import codeu.chat.common.Interest;
import codeu.chat.common.InterestStatus;
import codeu.chat.common.InterestType;
import codeu.chat.common.Message;
import codeu.chat.common.RandomUuidGenerator;
import codeu.chat.common.RawController;
import codeu.chat.common.User;
import codeu.chat.common.UserType;
import codeu.chat.util.Logger;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Controller implements RawController, BasicController {

  private static final Logger.Log LOG = Logger.newLog(Controller.class);

  private final Model model;
  private final Uuid.Generator uuidGenerator;

  private static boolean writeToLog;

  public Controller(Uuid serverId, Model model) {
    this.model = model;
    this.uuidGenerator = new RandomUuidGenerator(serverId, System.currentTimeMillis());
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
  public ConversationHeader newConversation(String title, Uuid owner, UserType defaultAccess) {
    return newConversation(createId(), title, owner, Time.now(), defaultAccess);
  }

  @Override
  public Message newMessage(
      Uuid id, Uuid author, Uuid conversation, String body, Time creationTime) {

    final User foundUser = model.userById().first(author);
    final ConversationPayload foundConversation =
        model.conversationPayloadById().first(conversation);

    Message message = null;

    if (foundUser != null && foundConversation != null && isIdFree(id)) {

      message =
          new Message(
              id,
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
          Uuid.equals(foundConversation.firstMessage, Uuid.NULL)
              ? message.id
              : foundConversation.firstMessage;

      // Update the conversation to point to the new last message as it has changed.

      foundConversation.lastMessage = message.id;
    }

    return message;
  }

  @Override
  public User newUser(Uuid id, String name, Time creationTime) {

    User user = null;

    if (isIdFree(id)) {

      user = new User(id, name, creationTime);
      model.add(user);

      LOG.info("newUser success (user.id=%s user.name=%s user.time=%s)", id, name, creationTime);

    } else {

      LOG.info(
          "newUser fail - id in use (user.id=%s user.name=%s user.time=%s)",
          id, name, creationTime);
    }

    return user;
  }

  @Override
  public ConversationHeader newConversation(
      Uuid id, String title, Uuid owner, Time creationTime, UserType defaultAccess) {

    final User foundOwner = model.userById().first(owner);

    ConversationHeader conversation = null;
    ConversationPermission permission = null;

    if (foundOwner != null && isIdFree(id)) {
      conversation = new ConversationHeader(id, owner, creationTime, title, defaultAccess);
      permission = new ConversationPermission(id, owner, defaultAccess);
      model.add(conversation, permission);
      LOG.info("Conversation added: " + id);
    }

    return conversation;
  }

  public void removeConversation(ConversationHeader conversation) {
    model.remove(conversation);
  }

  public Interest addInterest(Uuid userId, Uuid interestId, InterestType interestType) {
    return addInterest(userId, userId, interestId, interestType, Time.now());
  }

  public Interest addInterest(
      Uuid id, Uuid userId, Uuid interestId, InterestType interestType, Time creationTime) {
    return model.addInterest(id, userId, interestId, interestType, creationTime);
  }

  public void removeInterest(Uuid userId, Uuid interestId) {
    model.removeInterest(userId, interestId);
  }

  public List<InterestStatus> interestStatus(Uuid user) {
    List<Uuid> userInterests = model.interests.get(user);
    List<InterestStatus> result = new ArrayList<>();
    Time now = Time.now();
    if (userInterests == null) return result;
    for (Uuid interestId : userInterests) {
      InterestStatus report = processInterest(interestId, user, now);
      if (report != null) {
        result.add(report);
      }
    }
    return result;
  }

  private InterestStatus processInterest(Uuid id, Uuid userId, Time now) {
    Interest interest = model.interestById().first(id);
    if (interest == null) return null;
    Time lastUpdate = interest.lastUpdate;
    InterestStatus result = null;
    if (interest.type == InterestType.USER) {
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
    } else if (interest.type == InterestType.CONVERSATION) {
      ConversationPermission perm = model.permissionById().first(id);
      if (!perm.containsUser(userId)) {
        return null;
      }
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

    for (candidate = uuidGenerator.make(); isIdInUse(candidate); candidate = uuidGenerator.make()) {

      // Assuming that "randomUuid" is actually well implemented, this
      // loop should never be needed, but just in case make sure that the
      // Uuid is not actually in use before returning it.

    }

    return candidate;
  }

  private boolean isIdInUse(Uuid id) {
    return model.messageById().first(id) != null
        || model.conversationById().first(id) != null
        || model.userById().first(id) != null;
  }

  private boolean isIdFree(Uuid id) {
    return !isIdInUse(id);
  }

  /**
   * Getter method for writeToLog
   *
   * @return boolean if we should write to log or no
   */
  public static boolean getWriteToLog() {
    return writeToLog;
  }

  /**
   * Setter method for writeToLog
   *
   * @param write the new update for writeToLog
   */
  public static void setWriteToLog(boolean write) {
    writeToLog = write;
  }

  public ConversationHeader conversationHeaderById(Uuid id) {
    return model.conversationById().first(id);
  }

  // Changes the access of the target user to the access type.
  // The requester must have a higher level than the target user as well as the access type.
  // Returns true iff the operation was successful.
  @Override
  public boolean changeAccess(Uuid requester, Uuid target, Uuid conversation, UserType accessType) {
    ConversationPermission cp = model.permissionById().first(conversation);

    if (requester.equals(target)) {
      LOG.warning("Can't change self access.");
      return false;
    }

    // Requester must have a higher level than the target.
    if (!UserType.hasManagerAccess(cp.status(requester), cp.status(target))) {
      LOG.warning("Requester doesn't have permission to change access.");
      return false;
    }

    // Must be at least one level above to change someone else's access.
    if (!UserType.hasManagerAccess(cp.status(requester), accessType)) {
      LOG.warning("Requester doesn't have permission to change access.");
      return false;
    }

    cp.changeAccess(target, accessType);
    return true;
  }

  /*
   * Adds a user to the current conversation with the specified access type.
   */
  @Override
  public String addUser(Uuid requester, Uuid target, Uuid conversation, UserType memberBit) {
    ConversationPermission cp = model.permissionById().first(conversation);

    // Cannot add themself
    if (requester.equals(target)) {
      LOG.warning("Can't add yourself to current conversation");
      return "Can not add yourself.";
    }

    // Requester can not add user that is already in the current conversation
    if (cp.containsUser(target)) {
      LOG.warning("User has already been added to the conversation.");
      return "User had already been added.";
    }

    // Requester can not add users with  member access type
    if (cp.status(requester) == UserType.MEMBER) {
      LOG.warning("Requester's access type is member; can't add other users.");
      return "Can not add with member access type.";
    }

    // Requester must have a higher access type than access type that will be
    // assigned to the added user
    if (memberBit == null || !UserType.hasManagerAccess(cp.status(requester), memberBit)) {
      LOG.warning("Requester doesn't have permission to add user as that access" + " type.");

      return "You do not have permission to add the user.";
    }

    // If requester does not specify access type, add user with default access
    // type
    if (memberBit == UserType.NOTSET) {
      cp.changeAccess(target, cp.defaultAccess);
    } else {
      cp.changeAccess(target, memberBit);
    }
    return "User added successfully.";
  }

  /*
   * Removes a user from the current conversation.
   */
  @Override
  public String removeUser(Uuid requester, Uuid target, Uuid conversation) {
    ConversationPermission cp = model.permissionById().first(conversation);

    if (requester.equals(target)) {
      LOG.warning("Can't remove yourself from current conversation");
      return "Can not remove yourself. Use the leave command in the user panel.";
    }

    // Cannot remove a user if they do not exist in the current conversation
    if (!cp.containsUser(target)) {
      LOG.warning("User is not a member of the current conversation");
      return "User does not exist in the conversation.";
    }

    // Requester with member access type cannot remove other users
    if (cp.status(requester) == UserType.MEMBER) {
      LOG.warning("Requester doesn't have permission to remove user as that access" + " type.");
      return "Can not remove with member access type.";
    }

    // Requester must have a higher access type than target
    if (!UserType.hasManagerAccess(cp.status(requester), cp.status(target))) {
      LOG.warning("Requester doesn't have permission to remove user as that access" + " type.");
      return "You do not have permission to remove the user.";
    }

    cp.removeUser(target);
    return "User removed successfully.";
  }

  @Override
  public void leaveConversation(Uuid user, Uuid conversation) {
    ConversationPermission cp = model.permissionById().first(conversation);
    cp.removeUser(user);
  }

  @Override
  public Map<Uuid, UserType> getConversationPermission(Uuid id) {
    ConversationPermission cp = model.permissionById().first(id);
    return cp.getUsers();
  }

  public void refreshLog() {
    model.refresh(new File(model.createFilePath()));
  }

  @Override
  public boolean hasNewMessage(Uuid conversationId, Time lastUpdate) {
    if (conversationId == null || lastUpdate == null) {
      LOG.error("Conversation IDs and timestamps can't be null to retrieve new messages.");
      return false;
    }
    ConversationPayload payload = model.conversationPayloadById().first(conversationId);
    if (payload == null) {
      LOG.error("Payload retrieved does not exist");
      return false;
    }
    Uuid lastMessageId = payload.lastMessage;
    Message lastMessage = model.messageById().first(lastMessageId);
    if (lastMessage == null) {
      LOG.error("Message doesn't exist");
      return false;
    }
    return lastMessage.creation.compareTo(lastUpdate) >= 0;
  }
}
