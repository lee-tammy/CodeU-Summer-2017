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

import codeu.chat.client.core.Context;
import codeu.chat.client.core.ConversationContext;
import codeu.chat.client.core.MessageContext;
import codeu.chat.client.core.UserContext;
import codeu.chat.common.InterestStatus;
import codeu.chat.common.InterestType;
import codeu.chat.common.ServerInfo;
import codeu.chat.common.User;
import codeu.chat.common.UserType;
import codeu.chat.util.Duration;
import codeu.chat.util.Scheduled;
import codeu.chat.util.Time;
import codeu.chat.util.Tokenizer;
import codeu.chat.util.Uuid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public final class Chat {

  // PANELS
  //
  // We are going to use a stack of panels to track where in the application
  // we are. The command will always be routed to the panel at the top of the
  // stack. When a command wants to go to another panel, it will add a new
  // panel to the top of the stack. When a command wants to go to the previous
  // panel all it needs to do is pop the top panel.
  private final Stack<Panel> panels = new Stack<>();

  // Handle timers every 50 millisecond.
  public static final int TIMER_WAIT_TIME = 50;
  // Refresh messages every 5000 milliseconds.
  public static final int MESSAGE_REFRESH_RATE = 5000;

  // The time when the conversation was last updated.
  private Time lastUpdate = Time.minTime();

  private Context context;

  public Chat(Context context) {
    this.context = context;
    this.panels.push(createRootPanel(context));
    this.startHandlingTimers();
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
    for (String token = tokenizer.next(); token != null; token = tokenizer.next()) {
      args.add(token);
    }

    if (args.size() == 0) {
      // nothing was actually passed in, trying to get the first command
      // by calling args.get(0) will make the program crash
      return false;
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

  public void startHandlingTimers() {
    new Thread() {
      public void run() {
        while (true) {
          Panel current = panels.peek();
          current.handleTimeEvent(Time.now());
          try {
            Thread.sleep(TIMER_WAIT_TIME);
          } catch (InterruptedException ex) {
            System.err.println(ex);
          }
        }
      }
    }.start();
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
    panel.register(
        "help",
        new Panel.Command() {
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

    panel.register(
        "info",
        new Panel.Command() {
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
              System.out.format(
                  "  Duration   : %s sec\n",
                  (Time.duration(info.startTime, Time.now()).inMs() / 1000));
              System.out.println("Version: " + info.version);
            }
          }
        });

    // U-LIST (user list)
    //
    // Add a command to print all users registered on the server when the user
    // enters "u-list" while on the root panel.
    //
    panel.register(
        "u-list",
        new Panel.Command() {
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
    panel.register(
        "u-add",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            final String name = args.size() > 0 ? args.get(0).trim() : "";
            if (name.length() > 0) {
              if (findUser(name, context) != null) {
                System.out.println("ERROR: Username already taken");
              }
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
    panel.register(
        "u-sign-in",
        new Panel.Command() {
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

  private User userById(Uuid id, Context context) {
    for (final UserContext user : context.allUsers()) {
      if (user.user.id.equals(id)) {
        return user.user;
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

  private boolean hasAccess(Uuid user, ConversationContext cc) {
    Map<Uuid, UserType> hm = cc.getConversationPermission();
    return hm.containsKey(user);
  }

  private Panel createUserPanel(final UserContext user) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command that will print a list of all commands and their
    // descriptions when the user enters "help" while on the user panel.
    //
    panel.register(
        "help",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            System.out.println("USER MODE");
            System.out.println("  c-list");
            System.out.println(
                "    List all conversations that the current user can interact with.");
            System.out.println("  c-add <title> <default permission>");
            System.out.println(
                "    Add a new conversation with the given title and join it as the current user. ");
            System.out.println("    Specify default member/owner permission when a user is added");
            System.out.println("  c-remove <title>");
            System.out.println(
                "    Remove a conversation with the given title (Must"
                    + " be the creator to remove).");
            System.out.println("  c-join <title>");
            System.out.println("    Join the conversation as the current user.");
            System.out.println("  c-leave <title> <optional: username>");
            System.out.println("    Leave the conversation as the current user. If user is creator,");
            System.out.println("    type username after title to promote a user to creator access type.");
            System.out.println("    If optional username is empty, the conversation will be deleted.");
            System.out.println("  i-add <u for user or c for conversation> <username or title>.");
            System.out.println("    Get updates on conversations and users.");
            System.out.println(
                "  i-remove <u for user or c for conversation>" + " <username or title>.");
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
    panel.register(
        "c-list",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            for (final ConversationContext conversation : user.conversations()) {
              if (hasAccess(user.user.id, conversation)) {
                System.out.format(
                    "CONVERSATION %s (UUID:%s)\n",
                    conversation.conversation.title, conversation.conversation.id);
              }
            }
          }
        });

    // C-ADD (add conversation)
    //
    // Add a command that will create and join a new conversation when the user
    // enters "c-add" while on the user panel.
    //
    panel.register(
        "c-add",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            final String name = args.size() > 0 ? args.get(0).trim() : "";
            String defaultAccess = args.size() > 1 ? args.get(1).trim() : "";
            UserType access = null;
            switch (defaultAccess) {
              case "M":
                access = UserType.MEMBER;
                break;
              case "O":
                access = UserType.OWNER;
                break;
              default:
                System.out.println(
                    "ERROR: Please provide valid default access type. 'M' for member or 'O' for owner");
                return;
            }

            if (name.isEmpty()){
              System.out.println("ERROR: Missing <title>");
              return;
            }

            if (find(name, user) != null) {
              System.out.println("ERROR: Conversation name already taken");
              return;
            }

            final ConversationContext conversation = user.start(name, access);
            if (conversation == null) {
              System.out.println("ERROR: Failed to create new conversation");
            }
            
          }
        });

    // C-REMOVE (remove conversation)
    //
    // Add a command that will remove a conversation when the creator  enters
    // "c-remove" while on the user panel.
    //
    panel.register(
        "c-remove",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            final String name = args.size() > 0 ? args.get(0).trim() : "";
            if (name.isEmpty()) {
              System.out.println("ERROR: Missing <title>");
              return;
            }

            ConversationContext conversation = find(name, user);
            if (conversation == null) {
              System.out.println("ERROR: Conversation does not exist");
              return;
            }

            Map<Uuid, UserType> permissions = conversation.getConversationPermission();
            UserType requester = permissions.get(user.user.id);
            if (requester == null || requester != UserType.CREATOR) {
              System.out.println("ERROR: You do not have permission to remove this conversation.");
              return;
            }

            user.stop(conversation.conversation);
          }
        });

    // C-JOIN (join conversation)
    //
    // Add a command that will join a conversation when the user enters
    // "c-join" while on the user panel.
    //
    panel.register(
        "c-join",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            final String name = args.size() > 0 ? args.get(0).trim() : "";
            if (name.length() > 0) {
              final ConversationContext conversation = find(name, user);
              if (conversation == null) {
                System.out.format("ERROR: No conversation with name '%s'\n", name);
              } else {
                if (hasAccess(user.user.id, conversation)) {
                  panels.push(createConversationPanel(conversation));
                } else {
                  System.out.println(
                      "ERROR: You do not currently have access to this conversation.");
                }
              }
            } else {
              System.out.println("ERROR: Missing <title>");
            }
          }
        });
  
    // C-LEAVE (leave conversation)
    //
    // Add command that will leave the conversation when the user enters
    // "c-leave" while on the user panel. If the user is a creator, they have
    // the option to delete the conversation or promote another user to creator
    // access type
    //
    panel.register("c-leave", new Panel.Command(){
      public void invoke(List<String> args){
        final String title = args.size() > 0 ? args.get(0).trim() : "";
        if(title.isEmpty()){
          System.out.println("ERROR: Missing <title>");
          return;
        }

        ConversationContext conversation = find(title, user);
        if(conversation == null){
          System.out.println("ERROR: Conversation does not exist.");
          return;
        }

        Map<Uuid, UserType> permissions = conversation.getConversationPermission(); 
        UserType requester = permissions.get(user.user.id);
        if(requester == null){
          System.out.println("ERROR: You are not in the conversation.");
          return;
        }

        if(requester != UserType.CREATOR){
          conversation.leave(user.user.id);
          return;
        }

        final String name = args.size() == 2 ? args.get(1).trim() : "";
        if(name.isEmpty()){
          user.stop(conversation.conversation);
          return;
        }
        UserContext username = findUser(name, context);
        if(username == null){
          System.out.println("ERROR: User does not exist.");
          return;
        }
        UserType newCreator = permissions.get(username.user.id);
        if(newCreator == null){
          System.out.println("ERROR: User is not in the conversation.");
          return;
        }
        conversation.changeAccess(username.user.id, UserType.CREATOR);
        conversation.leave(user.user.id);
      }
    });

    // I-ADD (adds an interest)
    //
    // Adds a command that will allow the user to add users and conversations
    // as interests
    //
    panel.register(
        "i-add",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            InterestType type = InterestType.USER;

            if (args.size() == 2) {

              final UserContext userInterest = findUser(args.get(1), context);
              final Uuid userId = user.user.id;
              final ConversationContext convoInterest = find(args.get(1), user);

              if (args.get(0).equals("u")) {
                type = InterestType.USER;
                if (userInterest != null) {
                  user.getController().newInterest(userId, userInterest.user.id, type);
                } else {
                  System.out.format("ERROR: '%s' does not exist", args.get(1));
                }
              } else if (args.get(0).equals("c")) {
                if (convoInterest != null) {
                  type = InterestType.CONVERSATION;
                  user.getController().newInterest(userId, convoInterest.conversation.id, type);
                } else {
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

    // I-REMOVE (removes an interest)
    //
    // Adds a command that will allow the user to remove users and
    // conversations as interests
    //
    panel.register(
        "i-remove",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            if (args.size() == 2) {

              final UserContext userInterest = findUser(args.get(1), context);
              final ConversationContext convoInterest = find(args.get(1), user);
              final Uuid userId = user.user.id;

              if (args.get(0).equals("u")) {
                if (userInterest != null) {
                  user.getController().removeInterest(userId, userInterest.user.id);
                } else {
                  System.out.format("ERROR: '%s' does not exist", args.get(1));
                }
              } else if (args.get(0).equals("c")) {

                if (convoInterest != null) {
                  user.getController().removeInterest(userId, convoInterest.conversation.id);
                } else {
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

    // STATUS-UPDATE (status update)
    //
    // Adds a command that will report the status updates of the interests
    //
    panel.register(
        "status-update",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {

            Collection<InterestStatus> allInterests = user.getController().statusUpdate(user);
            if (allInterests != null && !allInterests.isEmpty()) {
              System.out.println("STATUS UPDATE");
              System.out.println("===============");

              for (InterestStatus interest : allInterests) {

                if (interest.type == InterestType.CONVERSATION) {
                  System.out.format(
                      "Number of unread messages in conversation %s: '%d'\n",
                      interest.name, interest.unreadMessages);

                } else {
                  System.out.format(
                      "Number of new conversations by user %s: '%d'\n",
                      interest.name, interest.newConversations.size());
                  for (int j = 0; j < interest.newConversations.size(); j++) {
                    System.out.println(" " + interest.newConversations.get(j));
                  }
                  System.out.format(
                      "Number of conversations the user %s contributed to: '%d'\n",
                      interest.name, interest.addedConversations.size());
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
    panel.register(
        "info",
        new Panel.Command() {
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

  private void listMessages(final ConversationContext conversation, List<String> args) {
    if (hasAccess(conversation.getUser(), conversation)) {
      System.out.println("--- start of conversation ---");
      for (MessageContext message = conversation.firstMessage();
          message != null;
          message = message.next()) {
        System.out.println();
        System.out.format("USER : %s\n", message.message.author);
        System.out.format("SENT : %s\n", message.message.creation);
        System.out.println();
        System.out.println(message.message.content);
        System.out.println();
      }
      System.out.println("---  end of conversation  ---");
    } else {
      System.out.println("ERROR: you no longer have access to this conversation");
    }
  }

  private Panel createConversationPanel(final ConversationContext conversation) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command that will print all the commands and their descriptions
    // when the user enters "help" while on the conversation panel.
    //
    panel.register(
        "help",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            System.out.println("CONVERSATION MODE");
            System.out.println("  m-list");
            System.out.println("    List all messages in the current conversation.");
            System.out.println("  m-add <message>");
            System.out.println(
                "    Add a new message to the current conversation as " + "the current user.");
            System.out.println("  u-add <user> <M for member or O for owner> ");
            System.out.println(
                "    Add a user to the current conversation and"
                    + " declare their membership. Second argument is option if"
                    + " default membership is desired.");
            System.out.println("  u-remove <user>");
            System.out.println("    Remove a user from the current conversation.");
            System.out.println("  modify-access <user> <accessType>");
            System.out.println("    Change permissions of user. <userType> is O for owner,");
            System.out.println("    M for member, and R for remove");
            System.out.println("  u-list");
            System.out.println("    list all users and their access levels");
            System.out.println("  info");
            System.out.println("    Display all info about the current conversation.");
            System.out.println("  back");
            System.out.println("    Go back to USER MODE.");
            System.out.println("  exit");
            System.out.println("    Exit the program.");
          }
        });

    // List messages automatically every 5 seconds.
    panel.register(
        new Duration(MESSAGE_REFRESH_RATE),
        new Scheduled.Action() {

          @Override
          public void invoke() {
            // Get the time before sending the has new message request.
            Time now = Time.now();
            if (conversation.hasNewMessage(lastUpdate)) {
              listMessages(conversation, null);
              System.out.print(">>> ");
            }
            lastUpdate = now;
          }
        });

    // M-LIST (list messages)
    //
    // Add a command to print all messages in the current conversation when the
    // user enters "m-list" while on the conversation panel.
    //
    panel.register(
        "m-list",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            lastUpdate = Time.now();
            listMessages(conversation, args);
          }
        });
    // M-ADD (add message)
    //
    // Add a command to add a new message to the current conversation when the
    // user enters "m-add" while on the conversation panel.
    //
    panel.register(
        "m-add",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            String message = args.size() > 0 ? args.get(0) : "";
            for (int i = 1; i < args.size(); i++) {
              message += " " + args.get(i);
            }
            if (message.length() < 0) {
              System.out.println("ERROR: Messages must contain text");
            }
            if (hasAccess(conversation.getUser(), conversation)) conversation.add(message);
            else {
              System.out.println("ERROR: you no longer have access to this conversation");
            }
          }
        });

    // INFO
    //
    // Add a command to print info about the current conversation when the user
    // enters "info" while on the conversation panel.
    //
    panel.register(
        "info",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            System.out.println("Conversation Info:");
            System.out.format("  Title : %s\n", conversation.conversation.title);
            System.out.format("  Id    : UUID:%s\n", conversation.conversation.id);
            System.out.format("  Owner : %s\n", conversation.conversation.creator);
          }
        });

    panel.register(
        "modify-access",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            if (args.size() >= 2) {
              User targetUser = findUser(args.get(0).trim(), context).user;
              String accessType = args.get(1).trim();
              UserType access = null;
              switch (accessType) {
                case "M":
                  access = UserType.MEMBER;
                  break;
                case "O":
                  access = UserType.OWNER;
                  break;
                case "R":
                  access = UserType.NOTSET;
                  break;
                default:
                  System.out.println("ERROR: Please provide valid access type");
                  return;
              }

              if (targetUser == null) {
                System.out.println("ERROR: Could not find user");
                return;
              }

              if (conversation.changeAccess(targetUser.id, access)) {
                System.out.println("Access was modified successfully");
              } else {
                System.out.println(
                    "ERROR: Couldn't modify target's access. Check your privileges and try again");
              }
            } else {
              System.out.println("ERROR: Please provide the right number of arguments");
            }
          }
        });

    // U-ADD
    //
    // Adds a user to current conversation
    //
    panel.register(
        "u-add",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            final int argSize = args.size();
            if (argSize == 2 || argSize == 1) {
              final UserContext addUser = findUser(args.get(0), context);
              final String arg2 = args.size() == 2 ? args.get(1).trim() : "";
              UserType memberBit = UserType.NOTSET;

              if (addUser != null) {
                if (argSize == 2) {
                  switch (arg2) {
                    case "M":
                      memberBit = UserType.MEMBER;
                      break;
                    case "O":
                      memberBit = UserType.OWNER;
                      break;
                    default:
                      System.out.print("ERROR: Invalid access type");
                  }
                }
                String message = conversation.addUser(addUser.user.id, memberBit);
                System.out.print(message);
              } else {
                System.out.print("ERROR: User does not exist");
              }
            } else {
              System.out.print("ERROR: Wrong format");
            }
            System.out.println();
          }
        });

    // U-REMOVE
    //
    // Removes a user to current conversation
    //
    panel.register(
        "u-remove",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            if (args.size() == 1) {
              final UserContext removeUser = findUser(args.get(0), context);
              if (removeUser != null) {
                String message = conversation.removeUser(removeUser.user.id);
                System.out.print(message);
              } else {
                System.out.print("ERROR: User does not exist");
              }
            } else {
              System.out.print("ERROR: Wrong format");
            }
            System.out.println();
          }
        });

    // U-LIST
    //
    // List all users and their access level in a conversation
    panel.register(
        "u-list",
        new Panel.Command() {
          @Override
          public void invoke(List<String> args) {
            Map<Uuid, UserType> map = conversation.getConversationPermission();
            Set<Uuid> uuids = map.keySet();
            Iterator<Uuid> iter = uuids.iterator();
            while (iter.hasNext()) {
              Uuid id = iter.next();
              User user = userById(id, context);
              System.out.format("USER %s (UUID:%s)\n", user.name, user.id);
              System.out.println("Permission: " + map.get(id));
            }
          }
        });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }
}
