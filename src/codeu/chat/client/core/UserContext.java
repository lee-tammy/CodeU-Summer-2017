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
import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.User;
import codeu.chat.common.UserType;
import codeu.chat.util.connections.Connection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public final class UserContext {

  public final User user;
  private final BasicView view;
  private final BasicController controller;

  public UserContext(User user, BasicView view, BasicController controller) {
    this.user = user;
    this.view = view;
    this.controller = controller;
  }

  public Connection getSource() throws IOException {
    Connection connect = ((Controller) controller).getSource().connect();
    if (connect != null) {
      return connect;
    } else {
      throw new IOException();
    }
  }

  public Controller getController() {
    return (Controller) controller;
  }

  public ConversationContext start(String name, UserType access) {
    final ConversationHeader conversation = controller.newConversation(name, user.id, access);
    return conversation == null
        ? null
        : new ConversationContext(user, conversation, view, controller);
  } 

  public void stop(ConversationHeader conversation){
    controller.removeConversation(conversation);
  }

  public Iterable<ConversationContext> conversations() {

    // Use all the ids to get all the conversations and convert them to
    // Conversation Contexts.
    final Collection<ConversationContext> all = new ArrayList<>();
    for (final ConversationHeader conversation : view.getConversations()) {
      all.add(new ConversationContext(user, conversation, view, controller));
    }

    return all;
  }
}
