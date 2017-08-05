package codeu.chat.util;

import java.util.PriorityQueue;

// SCHEDULER
//
// Given scheduled events, the Scheduler will check whehter a timer has expired and run its handler
// if necessary.
public class Scheduler {
  private final PriorityQueue<Scheduled> events = new PriorityQueue<>();

  // Must be run often enough to prevent missing cycles.
  public void runIteration(Time now) {
    Scheduled command = events.poll();
    if (command != null) {
      if (command.runHandler(now)) {
        events.add(command);
      }
    }
  }

  public void addEvent(Scheduled event) {
    events.add(event);
  }
}
