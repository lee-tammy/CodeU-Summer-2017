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

package codeu.chat.client.commandline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.io.IOException;

import codeu.chat.client.core.Context;
import codeu.chat.client.core.ConversationContext;
import codeu.chat.client.core.MessageContext;
import codeu.chat.client.core.UserContext;
import codeu.chat.common.InterestStatus;
import codeu.chat.common.Type;
import codeu.chat.util.Uuid;
import codeu.chat.util.Tokenizer;
import codeu.chat.util.Time;
import codeu.chat.common.ServerInfo;

public final class Chat {

  // PANELS
  //
  // We are going to use a stack of panels to track where in the application
  // we are. The command will always be routed to the panel at the top of the
  // stack. When a command wants to go to another panel, it will add a new
  // panel to the top of the stack. When a command wants to go to the previous
  // panel all it needs to do is pop the top panel.
  private final Stack<Panel> panels = new Stack<>();

  private Context context;

  public Chat(Context context) {
    this.context = context;
    this.panels.push(createRootPanel(context));
  }

  // HANDLE COMMAND
  //
  // Take a single line of input and parse a command from it. If the system
  // is willing to take another command, the function will return true. If
  // the system wants to exit, the function will return false.
  //
  public boolean handleCommand(String line) throws IOException {

    final List<String> args = new ArrayList<>();
    final Tokenizer tokenizer = new Tokenizer(line);
    for (String token = tokenizer.next(); token != null; token = tokenizer.next()){
      args.add(token);
    }
    final String command = args.get(0);
    args.remove(0);

    // Because "exit" and "back" are applicable to every panel, handle
    // those commands here to avoid having to implement them for each
    // panel.

    if ("exit".equals(command)) {
      // The user does not want to process any more commands
      return false;
    }

    // Do not allow the root panel to be removed.
    if ("back".equals(command) && panels.size() > 1) {
      panels.pop();
      return true;
    }

    if (panels.peek().handleCommand(command, args)) {
      // the command was handled
      return true;
    }

    // If we get to here it means that the command was not correctly handled
    // so we should let the user know. Still return true as we want to continue
    // processing future commands.
    System.out.println("ERROR: Unsupported command");
    return true;
  }

  // CREATE ROOT PANEL
  //
  // Create a panel for the root of the application. Root in this context means
  // the first panel and the only panel that should always be at the bottom of
  // the panels stack.
  //
  // The root panel is for commands that require no specific contextual
  // information.
  // This is before a user has signed in. Most commands handled by the root
  // panel
  // will be user selection focused.
  //
  private Panel createRootPanel(final Context context) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command to print a list of all commands and their description when
    // the user for "help" while on the root panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("ROOT MODE");
        System.out.println("  u-list");
        System.out.println("    List all users.");
        System.out.println("  u-add <name>");
        System.out.println("    Add a new user with the given name.");
        System.out.println("  u-sign-in <name>");
        System.out.println("    Sign in as the user with the given name.");
        System.out.println("  info");
        System.out.println("    Get server info.");
        System.out.println("    Show the server information.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });
    
    panel.register("info", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final ServerInfo info = context.getInfo();
        if (info == null) {	
          System.out.format("ERROR: Failed to retrieve version info", args);
        } else {
          // Print the server info to the user in a pretty way
           // Print the server info to the user in a pretty way
          System.out.println("Server Information:");
          System.out.format("  Start Time : %s\n", info.startTime.toString());
          System.out.format("  Time now   : %s\n", Time.now());
          System.out.format("  Duration   : %s sec\n", (Time.duration(info.startTime,
                Time.now()).inMs() / 1000));
          System.out.println("Version: " + info.version);
        }
      }
    });
    

    // U-LIST (user list)
    //
    // Add a command to print all users registered on the server when the user
    // enters "u-list" while on the root panel.
    //
    panel.register("u-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for (final UserContext user : context.allUsers()) {
          System.out.format("USER %s (UUID:%s)\n", user.user.name, user.user.id);
        }
      }
    });

    // U-ADD (add user)
    //
    // Add a command to add and sign-in as a new user when the user enters
    // "u-add" while on the root panel.
    //
    panel.register("u-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.size() > 0 ? args.get(0).trim() : "";
        if (name.length() > 0) {
          if (context.create(name) == null) {
            System.out.println("ERROR: Failed to create new user");
          }
        } else {
          System.out.println("ERROR: Missing <username>");
        }
      }
    });

    // U-SIGN-IN (sign in user)
    //
    // Add a command to sign-in as a user when the user enters "u-sign-in"
    // while on the root panel.
    //
    panel.register("u-sign-in", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.size() > 0 ? args.get(0).trim() : "";
        if (name.length() > 0) {
          final UserContext user = findUser(name, context);
          if (user == null) {
            System.out.format("ERROR: Failed to sign in as '%s'\n", name);
          } else {
            panels.push(createUserPanel(user));
          }
        } else {
          System.out.println("ERROR: Missing <username>");
        }
      }

      // Find the first user with the given name and return a user context
      // for that user. If no user is found, the function will return null.

    });


    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }

  private UserContext findUser(String name, Context context) {
    for (final UserContext user : context.allUsers()) {
      if (user.user.name.equals(name)) {
        return user;
      }
    }
    return null;
  }

  private ConversationContext find(String title, UserContext user) {
    for (final ConversationContext conversation : user.conversations()) {
      if (title.equals(conversation.conversation.title)) {
        return conversation;
      }
    }
    return null;
  }

  private Panel createUserPanel(final UserContext user) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command that will print a list of all commands and their
    // descriptions when the user enters "help" while on the user panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("USER MODE");
        System.out.println("  c-list");
        System.out.println("    List all conversations that the current user can interact with.");
        System.out.println("  c-add <title>");
        System.out
            .println("    Add a new conversation with the given title and join it as the current user.");
        System.out.println("  c-join <title>");
        System.out.println("    Join the conversation as the current user.");
        System.out.println("  i-add <u for user or c for conversation> <username or title>.");
        System.out.println("    Get updates on conversations and users.");
        System.out
            .println("  i-remove <u for user or c for conversation>" + " <username or title>.");
        System.out.println("    Remove interest");
        System.out.println("  status-update");
        System.out.println("    Get status of interests");
        System.out.println("  info");
        System.out.println("    Display all info for the current user");
        System.out.println("  back");
        System.out.println("    Go back to ROOT MODE.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    // C-LIST (list conversations)
    //
    // Add a command that will print all conversations when the user enters
    // "c-list" while on the user panel.
    //
    panel.register("c-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for (final ConversationContext conversation : user.conversations()) {
          System.out.format("CONVERSATION %s (UUID:%s)\n",
                            conversation.conversation.title,
                            conversation.conversation.id);
        }
      }
    });

    // C-ADD (add conversation)
    //
    // Add a command that will create and join a new conversation when the user
    // enters "c-add" while on the user panel.
    //
    panel.register("c-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.size() > 0 ? args.get(0).trim() : "";
        if (name.length() > 0) {
          final ConversationContext conversation = user.start(name);
          if (conversation == null) {
            System.out.println("ERROR: Failed to create new conversation");
          }
        } else {
          System.out.println("ERROR: Missing <title>");
        }
      }
    });

    // C-JOIN (join conversation)
    //
    // Add a command that will join a conversation when the user enters
    // "c-join" while on the user panel.
    //
    panel.register("c-join", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.size() > 0 ? args.get(0).trim() : "";
        if (name.length() > 0) {
          final ConversationContext conversation = find(name, user);
          if (conversation == null) {
            System.out.format("ERROR: No conversation with name '%s'\n", name);
          } else {
            panels.push(createConversationPanel(conversation));
          }
        } else {
          System.out.println("ERROR: Missing <title>");
        }
      }
    });

    // C-FOLLOW (follows an interest)
    //
    // Adds a command that will allow the user to follow users and conversations
    // in order to get updates
    //
    panel.register("i-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        Type type = Type.USER;

        if(args.size() == 2){

          final UserContext userInterest = findUser(args.get(1), context);
          final Uuid userId = user.user.id;
          final ConversationContext convoInterest = find(args.get(1), user);

          if (args.get(0).equals("u")) {
            type = Type.USER;
            if (userInterest != null) {
              user.getController().newInterest(userId, userInterest.user.id, type);
            }else{
              System.out.format("ERROR: '%s' does not exist", args.get(1));
            }
          } else if (args.get(0).equals("c")) {
            if (convoInterest != null) {
              type = Type.CONVERSATION;
              user.getController().newInterest(userId,
                  convoInterest.conversation.id, type);
            }else{
              System.out.format("ERROR: '%s' does not exist", args.get(1));
            }

          } else {
            System.out.println("ERROR: Wrong format");
            return;
          }

        } else {
          System.out.println("ERROR: Wrong format");
        }
      }
    });

    // C-UNFOLLOW (unfollows an interest)
    //
    // Adds a command that will allow the user to unfollow users and
    // conversations to stop getting updates
    //
    panel.register("i-remove", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        if (args.size() == 2) {

          final UserContext userInterest = findUser(args.get(1), context);
          final ConversationContext convoInterest = find(args.get(1), user);
          final Uuid userId = user.user.id;

          if (args.get(0).equals("u")) {
            if (userInterest != null) {
              user.getController().removeInterest(userId, userInterest.user.id);
            }else{
              System.out.format("ERROR: '%s' does not exist", args.get(1));
            }
          }else if(args.get(0).equals("c")){

            if(convoInterest != null){
              user.getController().removeInterest(userId,
                  convoInterest.conversation.id);
            }else{
              System.out.format("ERROR: '%s' does not exist", args.get(1));
            }
          } else {
            System.out.println("ERROR: Wrong format");
            return;
          }

        } else {
          System.out.println("ERROR: Wrong format");
        }
      }
    });

    // C-STATUS (status update)
    //
    // Adds a command that will report the status updates of the followed users
    // and conversations
    //
    panel.register("status-update", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {

        Collection<InterestStatus> allInterests = user.getController().statusUpdate(user);
        if (allInterests != null && !allInterests.isEmpty()) {
          System.out.println("STATUS UPDATE");
          System.out.println("===============");

          for (InterestStatus interest : allInterests) {

            if (interest.type == Type.CONVERSATION) {
              System.out.format("Number of unread messages in conversation %s: '%d'\n",
                                interest.name,
                                interest.unreadMessages);

            } else {
              System.out.format("Number of new conversations by user %s: '%d'\n",
                                interest.name,
                                interest.newConversations.size());
              for (int j = 0; j < interest.newConversations.size(); j++) {
                System.out.println(" " + interest.newConversations.get(j));
              }
              System.out.format("Number of conversations the user %s contributed to: '%d'\n",
                                interest.name,
                                interest.addedConversations.size());
              for (int k = 0; k < interest.addedConversations.size(); k++) {
                System.out.println(" " + interest.addedConversations.get(k));
              }
            }
            System.out.println("===============");
          }
        }
      }
    });

    // INFO
    //
    // Add a command that will print info about the current context when the
    // user enters "info" while on the user panel.
    //
    panel.register("info", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("User Info:");
        System.out.format("  Name : %s\n", user.user.name);
        System.out.format("  Id   : UUID:%s\n", user.user.id);
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }

  private Panel createConversationPanel(final ConversationContext conversation) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command that will print all the commands and their descriptions
    // when the user enters "help" while on the conversation panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("USER MODE");
        System.out.println("  m-list");
        System.out.println("    List all messages in the current conversation.");
        System.out.println("  m-add <message>");
        System.out
            .println("    Add a new message to the current conversation as the current user.");
        System.out.println("  info");
        System.out.println("    Display all info about the current conversation.");
        System.out.println("  back");
        System.out.println("    Go back to USER MODE.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    // M-LIST (list messages)
    //
    // Add a command to print all messages in the current conversation when the
    // user enters "m-list" while on the conversation panel.
    //
    panel.register("m-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("--- start of conversation ---");
        for (MessageContext message = conversation
            .firstMessage(); message != null; message = message.next()) {
          System.out.println();
          System.out.format("USER : %s\n", message.message.author);
          System.out.format("SENT : %s\n", message.message.creation);
          System.out.println();
          System.out.println(message.message.content);
          System.out.println();
        }
        System.out.println("---  end of conversation  ---");
      }
    });

    // M-ADD (add message)
    //
    // Add a command to add a new message to the current conversation when the
    // user enters "m-add" while on the conversation panel.
    //
    panel.register("m-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String message = args.size() > 0 ? args.get(0).trim() : "";
        if (message.length() > 0) {
          conversation.add(message);
        } else {
          System.out.println("ERROR: Messages must contain text");
        }
      }
    });

    // INFO
    //
    // Add a command to print info about the current conversation when the user
    // enters "info" while on the conversation panel.
    //
    panel.register("info", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("Conversation Info:");
        System.out.format("  Title : %s\n", conversation.conversation.title);
        System.out.format("  Id    : UUID:%s\n", conversation.conversation.id);
        System.out.format("  Owner : %s\n", conversation.conversation.owner);
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }
}
