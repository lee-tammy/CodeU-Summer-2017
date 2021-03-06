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

package codeu.chat.common;

import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import java.util.Map;

// BASIC CONTROLLER
//
//   The controller component in the Model-View-Controller pattern. This
//   component is used to write information to the model where the model
//   is the current state of the server. Data returned from the controller
//   should be treated as read only data as manipulating any data returned
//   from the controller may have no effect on the server's state.
public interface BasicController {

  // NEW MESSAGE
  //
  //   Create a new message on the server. All parameters must be provided
  //   or else the server won't apply the change. If the operation is
  //   successful, a Message object will be returned representing the full
  //   state of the message on the server.
  Message newMessage(Uuid author, Uuid conversation, String body);

  // NEW USER
  //
  //   Create a new user on the server. All parameters must be provided
  //   or else the server won't apply the change. If the operation is
  //   successful, a User object will be returned representing the full
  //   state of the user on the server. Whether user names can be shared
  //   is undefined.
  User newUser(String name);

  // NEW CONVERSATION
  //
  //  Create a new conversation on the server. All parameters must be
  //  provided or else the server won't apply the change. If the
  //  operation is successful, a Conversation object will be returned
  //  representing the full state of the conversation on the server.
  //  Whether conversations can have the same title is undefined.
  ConversationHeader newConversation(String title, Uuid owner, UserType access);

  void removeConversation(ConversationHeader conversation);

  // CHANGE ACCESS
  //
  // Change the access of a target user in a given conversation. The requester must have both higher
  // permission than the target user as well as the newAccess. The function will return true iff the
  // function successfully changed access to the target user. If access could not be changed for any
  // reason, the function will return false.
  boolean changeAccess(Uuid requester, Uuid target, Uuid conversation, UserType newAccess);

  // ADD USER
  //
  // Add a user with access type to the current conversation.
  // The requester must be a creator or owner in order to have the permission
  // to add a user. If the access type is not specified, the default access
  // type will be assigned to the added user.
  String addUser(Uuid requester, Uuid target, Uuid conversation, UserType memberBit);

  // GET CONVERSATION PERMISSION
  //
  // Returns the list of users in a conversation along with their
  // level of access.
  Map<Uuid, UserType> getConversationPermission(Uuid id);

  // REMOVE USER
  //
  // Removes specified user from specified conversation
  // if the user is performing the action has adequate privileges
  String removeUser(Uuid id, Uuid target, Uuid id2);

  // LEAVE CONVERSATION
  //
  // Removes specified user from specified conversation 
  void leaveConversation(Uuid userId, Uuid conversationId);
  
  // HAS NEW MESSAGES
  //
  // Returns true iff the last message sent by the conversation was created after the timestamp in
  // the parameter
  boolean hasNewMessage(Uuid conversationId, Time lastUpdate);
}
