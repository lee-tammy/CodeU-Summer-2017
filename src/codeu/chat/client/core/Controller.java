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

package codeu.chat.client.core;

import codeu.chat.common.BasicController;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.InterestStatus;
import codeu.chat.common.InterestType;
import codeu.chat.common.Message;
import codeu.chat.common.NetworkCode;
import codeu.chat.common.User;
import codeu.chat.common.UserType;
import codeu.chat.util.Logger;
import codeu.chat.util.Serializers;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import codeu.chat.util.connections.Connection;
import codeu.chat.util.connections.ConnectionSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class Controller implements BasicController {

  private static final Logger.Log LOG = Logger.newLog(Controller.class);

  private final ConnectionSource source;

  public Controller(ConnectionSource source) {
    this.source = source;
  }

  public ConnectionSource getSource() {
    return source;
  }

  @Override
  public Message newMessage(Uuid author, Uuid conversation, String body) {

    Message response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_MESSAGE_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), author);
      Uuid.SERIALIZER.write(connection.out(), conversation);
      Serializers.STRING.write(connection.out(), body);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_MESSAGE_RESPONSE) {
        response = Serializers.NULLABLE(Message.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public User newUser(String name) {

    User response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_USER_REQUEST);
      Serializers.STRING.write(connection.out(), name);
      LOG.info("newUser: Request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_USER_RESPONSE) {
        response = Serializers.NULLABLE(User.SERIALIZER).read(connection.in());
        LOG.info("newUser: Response completed.");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public ConversationHeader newConversation(String title, Uuid owner, UserType defaultAccess) {

    ConversationHeader response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_CONVERSATION_REQUEST);
      Serializers.STRING.write(connection.out(), title);
      Uuid.SERIALIZER.write(connection.out(), owner);
      UserType.SERIALIZER.write(connection.out(), defaultAccess);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_CONVERSATION_RESPONSE) {
        response = Serializers.NULLABLE(ConversationHeader.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public void removeConversation(ConversationHeader conversation) {
    try (final Connection connection = source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.REMOVE_CONVERSATION_REQUEST);
      Serializers.NULLABLE(ConversationHeader.SERIALIZER).write(connection.out(), conversation);
      if (Serializers.INTEGER.read(connection.in()) != NetworkCode.REMOVE_CONVERSATION_RESPONSE) {
        LOG.error("Response during call on server.");
      }
    } catch (Exception ex) {
      LOG.error(ex, "Exception during call on server.");
    }
  }

  public void newInterest(Uuid userId, Uuid interestId, InterestType interestType) {

    try (final Connection connection = this.source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_INTEREST_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), userId);
      Uuid.SERIALIZER.write(connection.out(), interestId);
      InterestType.SERIALIZER.write(connection.out(), interestType);

      if (Serializers.INTEGER.read(connection.in()) != NetworkCode.NEW_INTEREST_RESPONSE) {
        LOG.error("Response from server failed.");
      }

    } catch (Exception ex) {
      LOG.error(ex, "Exception during call on server.");
    }
  }

  public void removeInterest(Uuid userId, Uuid interestId) {

    try (final Connection connection = source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.REMOVE_INTEREST_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), userId);
      Uuid.SERIALIZER.write(connection.out(), interestId);
      if (Serializers.INTEGER.read(connection.in()) != NetworkCode.REMOVE_INTEREST_RESPONSE) {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      LOG.error(ex, "Exception during call on server.");
    }
  }

  @Override
  public boolean hasNewMessage(Uuid conversationId, Time lastUpdate) {
    try (final Connection connection = source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.HAS_NEW_MESSAGE_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), conversationId);
      Time.SERIALIZER.write(connection.out(), lastUpdate);
      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.HAS_NEW_MESSAGE_RESPONSE) {
        return Serializers.BOOLEAN.read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      LOG.error(ex, "Exception during call on server.");
    }
    return false;
  }

  public Collection<InterestStatus> statusUpdate(UserContext user) {
    Collection<InterestStatus> allInterests = null;
    try (final Connection connection = source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.INTEREST_STATUS_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), user.user.id);
      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.INTEREST_STATUS_RESPONSE) {
        allInterests = Serializers.COLLECTION(InterestStatus.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      LOG.error(ex, "Exception during call on server.");
    }
    return allInterests;
  }

  @Override
  public boolean changeAccess(Uuid requester, Uuid target, Uuid conversation, UserType newAccess) {
    try (final Connection connection = source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.CHANGE_PRIVILEGE_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), requester);
      Uuid.SERIALIZER.write(connection.out(), target);
      Uuid.SERIALIZER.write(connection.out(), conversation);
      UserType.SERIALIZER.write(connection.out(), newAccess);

      int reply = Serializers.INTEGER.read(connection.in());

      if (reply == NetworkCode.SUFFICIENT_PRIVILEGES_RESPONSE) {
        LOG.info("Changed user privilege");
        return true;
      } else if (reply == NetworkCode.INSUFFICIENT_PRIVILEGES_RESPONSE) {
        LOG.error("Insufficient privilege to complete action");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }
    return false;
  }

  @Override
  public String addUser(Uuid userId, Uuid addUserId, Uuid convoId, UserType memberBit) {

    String message = "";

    try (final Connection connection = this.source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.ADD_USER_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), userId);
      Uuid.SERIALIZER.write(connection.out(), addUserId);
      Uuid.SERIALIZER.write(connection.out(), convoId);
      UserType.SERIALIZER.write(connection.out(), memberBit);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.ADD_USER_RESPONSE) {
        message = Serializers.STRING.read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }

    } catch (Exception ex) {
      LOG.error(ex, "Exception during call on server.");
    }
    return message;
  }

  @Override
  public String removeUser(Uuid userId, Uuid removeUserId, Uuid convoId) {
    String message = "";
    try (final Connection connection = this.source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.REMOVE_USER_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), userId);
      Uuid.SERIALIZER.write(connection.out(), removeUserId);
      Uuid.SERIALIZER.write(connection.out(), convoId);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.REMOVE_USER_RESPONSE) {
        message = Serializers.STRING.read(connection.in());
      }
    } catch (Exception ex) {
      LOG.error(ex, "Exception during call on server.");
    }
    return message;
  }

  @Override
  public Map<Uuid, UserType> getConversationPermission(Uuid id) {
    Map<Uuid, UserType> hm = new HashMap<Uuid, UserType>();
    try (final Connection connection = this.source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.USER_LIST_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), id);
      hm = Serializers.MAP(Uuid.SERIALIZER, UserType.SERIALIZER).read(connection.in());
    } catch (Exception ex) {
      LOG.error(ex, "Exception during call on server.");
    }
    return hm;
  }
}
