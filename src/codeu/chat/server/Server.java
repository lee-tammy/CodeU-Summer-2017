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
import codeu.chat.common.InterestStatus;
import codeu.chat.common.Message;
import codeu.chat.common.NetworkCode;
import codeu.chat.common.Relay;
import codeu.chat.common.Secret;
import codeu.chat.common.ServerInfo;
import codeu.chat.common.InterestType;
import codeu.chat.common.User;
import codeu.chat.common.UserType;
import codeu.chat.util.Logger;
import codeu.chat.util.Serializers;
import codeu.chat.util.Timeline;
import codeu.chat.util.Uuid;
import codeu.chat.util.connections.Connection;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class Server {

  public interface Command {
    void onMessage(InputStream in, OutputStream out) throws IOException;
  }

  private static final Logger.Log LOG = Logger.newLog(Server.class);

  private static final int RELAY_REFRESH_MS = 5000; // 5 seconds
  private static final int LOG_REFRESH_MS = 10000; // 10 seconds

  private static ServerInfo info = new ServerInfo();

  private final Timeline timeline = new Timeline();

  private final Map<Integer, Command> commands = new HashMap<>();

  private final Uuid id;
  private final Secret secret;

  private final Model model = new Model();
  private final View view = new View(model);
  private final Controller controller;

  private final Relay relay;
  private Uuid lastSeen = Uuid.NULL;

  public Server(final Uuid id, final Secret secret, final Relay relay) {

    this.id = id;
    this.secret = secret;
    this.controller = new Controller(id, model);
    this.relay = relay;

    codeu.chat.server.Controller.setWriteToLog(false);

    info = new ServerInfo();

    this.commands.put(
        NetworkCode.SERVER_INFO_REQUEST,
        new Command() {
          public void onMessage(InputStream in, OutputStream out) throws IOException {
            Serializers.INTEGER.write(out, NetworkCode.SERVER_INFO_RESPONSE);
            ServerInfo.SERIALIZER.write(out, info);
          }
        });

    // New Message - A client wants to add a new message to the back end.
    this.commands.put(
        NetworkCode.NEW_MESSAGE_REQUEST,
        new Command() {
          @Override
          public void onMessage(InputStream in, OutputStream out) throws IOException {

            final Uuid author = Uuid.SERIALIZER.read(in);
            final Uuid conversation = Uuid.SERIALIZER.read(in);
            final String content = Serializers.STRING.read(in);

            final Message message = controller.newMessage(author, conversation, content);

            Serializers.INTEGER.write(out, NetworkCode.NEW_MESSAGE_RESPONSE);
            Serializers.NULLABLE(Message.SERIALIZER).write(out, message);

            timeline.scheduleNow(createSendToRelayEvent(author, conversation, message.id));
          }
        });

    // New User - A client wants to add a new user to the back end.
    this.commands.put(
        NetworkCode.NEW_USER_REQUEST,
        new Command() {
          @Override
          public void onMessage(InputStream in, OutputStream out) throws IOException {

            final String name = Serializers.STRING.read(in);
            final User user = controller.newUser(name);

            Serializers.INTEGER.write(out, NetworkCode.NEW_USER_RESPONSE);
            Serializers.NULLABLE(User.SERIALIZER).write(out, user);
          }
        });

    // New Conversation - A client wants to add a new conversation to the back
    // end.
    this.commands.put(
        NetworkCode.NEW_CONVERSATION_REQUEST,
        new Command() {
          @Override
          public void onMessage(InputStream in, OutputStream out) throws IOException {

            final String title = Serializers.STRING.read(in);
            final Uuid owner = Uuid.SERIALIZER.read(in);
            final UserType defaultAccess = UserType.SERIALIZER.read(in);
            final ConversationHeader conversation =
                controller.newConversation(title, owner, defaultAccess);

            Serializers.INTEGER.write(out, NetworkCode.NEW_CONVERSATION_RESPONSE);
            Serializers.NULLABLE(ConversationHeader.SERIALIZER).write(out, conversation);
          }
        });

    this.commands.put(NetworkCode.REMOVE_CONVERSATION_REQUEST, new Command(){
      @Override
      public void onMessage(InputStream in, OutputStream out)throws IOException{ 
        final ConversationHeader conversation = 
            Serializers.NULLABLE(ConversationHeader.SERIALIZER).read(in);
        controller.removeConversation(conversation);
        Serializers.INTEGER.write(out, NetworkCode.REMOVE_CONVERSATION_RESPONSE);
      }
    });

    // Get Users - A client wants to get all the users from the back end.
    this.commands.put(
        NetworkCode.GET_USERS_REQUEST,
        new Command() {
          @Override
          public void onMessage(InputStream in, OutputStream out) throws IOException {

            final Collection<User> users = view.getUsers();

            Serializers.INTEGER.write(out, NetworkCode.GET_USERS_RESPONSE);
            Serializers.COLLECTION(User.SERIALIZER).write(out, users);
          }
        });

    // Get Conversations - A client wants to get all the conversations from the
    // back end.
    this.commands.put(
        NetworkCode.GET_ALL_CONVERSATIONS_REQUEST,
        new Command() {
          @Override
          public void onMessage(InputStream in, OutputStream out) throws IOException {

            final Collection<ConversationHeader> conversations = view.getConversations();

            Serializers.INTEGER.write(out, NetworkCode.GET_ALL_CONVERSATIONS_RESPONSE);
            Serializers.COLLECTION(ConversationHeader.SERIALIZER).write(out, conversations);
          }
        });

    // Get Conversations By Id - A client wants to get a subset of the
    // conversations from the back end. Normally this will be done after calling
    // Get Conversations to get all the headers and now the client wants to get
    // a subset of the payloads.
    this.commands.put(
        NetworkCode.GET_CONVERSATIONS_BY_ID_REQUEST,
        new Command() {
          @Override
          public void onMessage(InputStream in, OutputStream out) throws IOException {

            final Collection<Uuid> ids = Serializers.COLLECTION(Uuid.SERIALIZER).read(in);
            final Collection<ConversationPayload> conversations = view.getConversationPayloads(ids);

            Serializers.INTEGER.write(out, NetworkCode.GET_CONVERSATIONS_BY_ID_RESPONSE);
            Serializers.COLLECTION(ConversationPayload.SERIALIZER).write(out, conversations);
          }
        });

    this.commands.put(
        NetworkCode.GET_USER_BY_ID_REQUEST,
        new Command() {
          @Override
          public void onMessage(InputStream in, OutputStream out) throws IOException {

            final Uuid userId = Uuid.SERIALIZER.read(in);

            Serializers.INTEGER.write(out, NetworkCode.GET_USER_BY_ID_RESPONSE);
            User.SERIALIZER.write(out, controller.userById(userId));
          }
        });

    this.commands.put(
        NetworkCode.GET_CONVERSATION_HEADER_BY_ID_REQUEST,
        new Command() {
          @Override
          public void onMessage(InputStream in, OutputStream out) throws IOException {

            final Uuid id = Uuid.SERIALIZER.read(in);

            Serializers.INTEGER.write(out, NetworkCode.GET_USER_BY_ID_RESPONSE);
            ConversationHeader.SERIALIZER.write(out, controller.conversationHeaderById(id));
          }
        });

    // Get Messages By Id - A client wants to get a subset of the messages from
    // the back end.
    this.commands.put(
        NetworkCode.GET_MESSAGES_BY_ID_REQUEST,
        new Command() {
          @Override
          public void onMessage(InputStream in, OutputStream out) throws IOException {

            final Collection<Uuid> ids = Serializers.COLLECTION(Uuid.SERIALIZER).read(in);
            final Collection<Message> messages = view.getMessages(ids);

            Serializers.INTEGER.write(out, NetworkCode.GET_MESSAGES_BY_ID_RESPONSE);
            Serializers.COLLECTION(Message.SERIALIZER).write(out, messages);
          }
        });

    this.commands.put(
        NetworkCode.NEW_INTEREST_REQUEST,
        new Command() {
          public void onMessage(InputStream in, OutputStream out) throws IOException {
            final Uuid userId = Uuid.SERIALIZER.read(in);
            final Uuid interestId = Uuid.SERIALIZER.read(in);
            final InterestType interestType = InterestType.SERIALIZER.read(in);

            controller.addInterest(userId, interestId, interestType);
            Serializers.INTEGER.write(out, NetworkCode.NEW_INTEREST_RESPONSE);
          }
        });

    this.commands.put(
        NetworkCode.REMOVE_INTEREST_REQUEST,
        new Command() {
          public void onMessage(InputStream in, OutputStream out) throws IOException {
            final Uuid userId = Uuid.SERIALIZER.read(in);
            final Uuid interestId = Uuid.SERIALIZER.read(in);
            controller.removeInterest(userId, interestId);
            Serializers.INTEGER.write(out, NetworkCode.REMOVE_INTEREST_RESPONSE);
          }
        });

    this.commands.put(
        NetworkCode.INTEREST_STATUS_REQUEST,
        new Command() {
          public void onMessage(InputStream in, OutputStream out) throws IOException {
            final Uuid userId = Uuid.SERIALIZER.read(in);
            Serializers.INTEGER.write(out, NetworkCode.INTEREST_STATUS_RESPONSE);
            Serializers.COLLECTION(InterestStatus.SERIALIZER)
                .write(out, controller.interestStatus(userId));
          }
        });

    this.commands.put(
        NetworkCode.CHANGE_PRIVILEGE_REQUEST,
        new Command() {
          public void onMessage(InputStream in, OutputStream out) throws IOException {
            final Uuid requesterId = Uuid.SERIALIZER.read(in);
            final Uuid targetId = Uuid.SERIALIZER.read(in);
            final Uuid conversationId = Uuid.SERIALIZER.read(in);
            final UserType newAccess = UserType.SERIALIZER.read(in);

            boolean controllerResponse =
                controller.changeAccess(requesterId, targetId, conversationId, newAccess);
            if (controllerResponse) {
              Serializers.INTEGER.write(out, NetworkCode.SUFFICIENT_PRIVILEGES_RESPONSE);
            } else {
              Serializers.INTEGER.write(out, NetworkCode.INSUFFICIENT_PRIVILEGES_RESPONSE);
            }
          }
        });

    this.commands.put(
        NetworkCode.ADD_USER_REQUEST,
        new Command() {
          public void onMessage(InputStream in, OutputStream out) throws IOException {
            final Uuid userId = Uuid.SERIALIZER.read(in);
            final Uuid addUserId = Uuid.SERIALIZER.read(in);
            final Uuid convoId = Uuid.SERIALIZER.read(in);
            final UserType memberBit = UserType.SERIALIZER.read(in);

            String message = controller.addUser(userId, addUserId, convoId, memberBit);

            Serializers.INTEGER.write(out, NetworkCode.ADD_USER_RESPONSE);
            Serializers.STRING.write(out, message);
          }
        });

    this.commands.put(
        NetworkCode.REMOVE_USER_REQUEST,
        new Command() {
          public void onMessage(InputStream in, OutputStream out) throws IOException {
            final Uuid userId = Uuid.SERIALIZER.read(in);
            final Uuid removeUserId = Uuid.SERIALIZER.read(in);
            final Uuid convoId = Uuid.SERIALIZER.read(in);

            String message = controller.removeUser(userId, removeUserId, convoId);

            Serializers.INTEGER.write(out, NetworkCode.REMOVE_USER_RESPONSE);
            Serializers.STRING.write(out, message);
          }
        });

    this.commands.put(
        NetworkCode.USER_LIST_REQUEST,
        new Command() {
          @Override
          public void onMessage(InputStream in, OutputStream out) throws IOException {
            final Uuid cpID = Uuid.SERIALIZER.read(in);
            Serializers.MAP(Uuid.SERIALIZER, UserType.SERIALIZER)
                .write(out, controller.getConversationPermission(cpID));
          }
        });

    this.timeline.scheduleNow(
        new Runnable() {
          @Override
          public void run() {
            try {

              LOG.info("Reading update from relay...");

              for (final Relay.Bundle bundle : relay.read(id, secret, lastSeen, 32)) {
                onBundle(bundle);
                lastSeen = bundle.id();
              }

            } catch (Exception ex) {

              LOG.error(ex, "Failed to read update from relay.");
            }

            timeline.scheduleIn(RELAY_REFRESH_MS, this);
          }
        });
    
    this.timeline.scheduleNow(
        new Runnable() {
          @Override
          public void run() {
            try {
              if(!model.getRestoredLog()) {
            	LOG.info("Restoring log...");
            	boolean restored = model.restore(new File(model.createFilePath()));
            	model.setRestoredLog(restored);
              }

              LOG.info("Updating log...");
                  
              controller.refreshLog();

            } catch (Exception ex) {

              LOG.error(ex, "Failed to update log.");
            }

            timeline.scheduleIn(LOG_REFRESH_MS, this);
          }
        });
  }

  public void handleConnection(final Connection connection) {
    timeline.scheduleNow(
        new Runnable() {
          @Override
          public void run() {
            try {

              LOG.info("Handling connection...");

              final int type = Serializers.INTEGER.read(connection.in());
              final Command command = commands.get(type);

              if (command == null) {
                // The message type cannot be handled so return a dummy message.
                Serializers.INTEGER.write(connection.out(), NetworkCode.NO_MESSAGE);
                LOG.info("Connection rejected");
              } else {
                command.onMessage(connection.in(), connection.out());
                LOG.info("Connection accepted");
              }

            } catch (Exception ex) {

              LOG.error(ex, "Exception while handling connection.");
            }

            try {
              connection.close();
            } catch (Exception ex) {
              LOG.error(ex, "Exception while closing connection.");
            }
          }
        });
  }

  private void onBundle(Relay.Bundle bundle) {

    final Relay.Bundle.Component relayUser = bundle.user();
    final Relay.Bundle.ConversationComponent relayConversation = bundle.conversation();
    final Relay.Bundle.Component relayMessage = bundle.user();

    User user = model.userById().first(relayUser.id());

    if (user == null) {
      user = controller.newUser(relayUser.id(), relayUser.text(), relayUser.time());
    }

    ConversationHeader conversation = model.conversationById().first(relayConversation.id());

    if (conversation == null) {

      // As the relay does not tell us who made the conversation - the first
      // person who has a message in the conversation will get ownership over
      // this server's copy of the conversation.
      conversation =
          controller.newConversation(
              relayConversation.id(),
              relayConversation.text(),
              relayConversation.creator(),
              relayConversation.time(),
              relayConversation.defaultAccess());
    }

    Message message = model.messageById().first(relayMessage.id());

    if (message == null) {
      message =
          controller.newMessage(
              relayMessage.id(),
              user.id,
              conversation.id,
              relayMessage.text(),
              relayMessage.time());
    }
  }

  private Runnable createSendToRelayEvent(
      final Uuid userId, final Uuid conversationId, final Uuid messageId) {
    return new Runnable() {
      @Override
      public void run() {
        final User user = view.findUser(userId);
        final ConversationHeader conversation = view.findConversation(conversationId);
        final Message message = view.findMessage(messageId);
        relay.write(
            id,
            secret,
            relay.pack(user.id, user.name, user.creation),
            relay.pack(
                conversation.id,
                conversation.title,
                conversation.creation,
                conversation.creator,
                conversation.defaultAccess),
            relay.pack(message.id, message.content, message.creation));
      }
    };
  }
}
