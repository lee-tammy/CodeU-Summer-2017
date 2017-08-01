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

import codeu.chat.util.Duration;
import codeu.chat.util.Time;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

// PANEL
//
// A panel is a collection of commands that are to be executed within a specific
// context. Similar to how panels are used in a GUI, is is a command line
// equivalent.
//
final class Panel {

  public interface Command {
    void invoke(List<String> args);
  }

  class TimerCommand {
    Command handler;
    Time firstTime;
    Time nextEventTime;
    Duration repeat;

    public TimerCommand(Command handler, Time eventTime, Duration repeat) {
      this.handler = handler;
      this.nextEventTime = eventTime;
      this.firstTime = eventTime;
      this.repeat = repeat;
    }

    public TimerCommand(Command handler, Duration repeat) {
      this(handler, Time.add(Time.now(), repeat), repeat);
    }

    public TimerCommand(Command handler, Time eventTime) {
      this(handler, eventTime, null);
    }

    // Lower time means higher priority.
    public int compareTo(TimerCommand other) {
      return other.nextEventTime.compareTo(this.nextEventTime);
    }

    public TimerCommand runHandler(Time now, List<String> args) {
      if (now.compareTo(this.nextEventTime) >= 0) {
        handler.invoke(args);
        if (repeat == null) {
          return null;
        }
        nextEventTime = Time.add(now, repeat);
      }
      return this;
    }
  }

  private final Map<String, Command> commands = new HashMap<>();
  private final PriorityQueue<TimerCommand> timerCommands = new PriorityQueue<>();

  // REGISTER
  //
  // Register the command to be called when the given command name is
  // given on the command line.
  //
  public void register(String commandName, Command command) {
    commands.put(commandName, command);
  }

  public void register(Time eventTime, Command command) {
    timerCommands.add(new TimerCommand(command, eventTime));
  }

  public void register(Duration repeatedDuration, Command command) {
    timerCommands.add(new TimerCommand(command, repeatedDuration));
  }

  // HANDLE COMMAND
  //
  // Given a command name and the rest of the line (from the command line) call
  // the correct command. If no command is found for the givem command name, false
  // will be returned. True will be return if a command is found. Whether or not
  // the command was successful is not returned.
  //
  public boolean handleCommand(String commandName, List<String> args) {
    final Command command = commands.get(commandName);
    if (command != null) {
      command.invoke(args);
    }
    return command != null;
  }

  // Given the current time call the correct command.
  public void handleNextEvent(Time now) {
    TimerCommand response = null;
    TimerCommand lastResponse = null;
    do {
      final TimerCommand command = timerCommands.poll();
      if (command != null) {
        lastResponse = response;
        response = command.runHandler(now, null);
        if (response != null) {
          timerCommands.add(response);
        }
      }
    } while (response != null && lastResponse != response);
  }
}
