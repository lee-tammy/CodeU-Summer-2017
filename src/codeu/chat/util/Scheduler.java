package codeu.chat.util;

import java.util.PriorityQueue;

// SCHEDULER
//
// Given scheduled events, the Scheduler will check whehter a timer has expired and run its handler
// if necessary.
public class Scheduler {
  private final PriorityQueue<Scheduled> scheduled = new PriorityQueue<>();

  // Must be run often enough to prevent missing cycles.
  public void runIteration(Time now) {
    final Scheduled command = scheduled.poll();
    if (command != null) {
      Scheduled response = command.runHandler(now);
      if (response != null) {
        scheduled.add(response);
      }
    }
  }

  public void addEvent(Action handler, Time eventTime, Duration repeat) {
    scheduled.add(new Scheduled(handler, eventTime, repeat));
  }

  public void addEvent(Action handler, Duration repeat) {
    scheduled.add(new Scheduled(handler, repeat));
  }

  public void addEvent(Action handler, Time eventTime) {
    scheduled.add(new Scheduled(handler, eventTime));
  }
}
