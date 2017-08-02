package codeu.chat.util;

// DURATION
//
// Duration is meant to abstract deltas of Time objects by allowing arithmetic operations such as
// addition and subtraction with Time objects. For instance if Time.now() + Duration(5) will give
// you the same Time as calling Time.now() 5 seconds later.
public class Duration implements Comparable<Duration> {
  public final long millis;

  public Duration(long millis) {
    this.millis = millis;
  }

  public Duration(Time start, Time end) {
    this.millis = end.inMs() - start.inMs();
  }

  @Override
  public int compareTo(Duration other) {
    double diff = this.millis - other.millis;
    if (diff < 0) return -1;
    if (diff > 0) return 1;
    return 0;
  }
}
