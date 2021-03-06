package codeu.chat.util;

// SCHEDULED
//
// Scheduled is a class that uses command to create a scheduled event. This class should be used in
// conjunction with the Scheduler class which provides a clean interface to schedule multiple
// events. There are two options for creating a Scheduled object. Option 1: use a one-time timer.
// Option 2: Use a repeating timer that can either start for the first time at a specified moment or
// (if unspecified) at Time.now() + repeat.
public class Scheduled implements Comparable<Scheduled> {

  // ACTION
  //
  // Action is a simple interface that is used to create handlers for events.
  public interface Action {
    public void invoke();
  }

  public final Action handler;
  public final Time firstTime;
  public final Duration repeat;
  private Time nextEventTime;

  public Scheduled(Action handler, Time eventTime, Duration repeat) {
    this.handler = handler;
    this.nextEventTime = eventTime;
    this.firstTime = eventTime;
    this.repeat = repeat;
  }

  public Scheduled(Action handler, Duration repeat) {
    this(handler, Time.add(Time.now(), repeat), repeat);
  }

  public Scheduled(Action handler, Time eventTime) {
    this(handler, eventTime, null);
  }

  // Lower time means higher priority.
  @Override
  public int compareTo(Scheduled other) {
    return other.nextEventTime.compareTo(this.nextEventTime);
  }

  // Returns true iff the Scheduled object has a repeat value.
  public boolean runHandler(Time now) {
    if (now.compareTo(this.nextEventTime) >= 0) {
      handler.invoke();
      if (repeat == null) {
        return false;
      }
      nextEventTime = Time.add(now, repeat);
    }
    return true;
  }
}
