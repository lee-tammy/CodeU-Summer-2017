package codeu.chat.util;

public class Duration implements Comparable<Duration> {
  public final double offset;
  public final int scale;

  public Duration(int offset, int scale) {
    this.offset = offset;
    this.scale = scale;
  }

  public Duration(int offset) {
    this(offset, 0);
  }

  public Duration(Time start, Time end) {
    this.offset = Math.toIntExact(end.inMs() - start.inMs());
    this.scale = -3;
  }

  @Override
  public int compareTo(Duration other) {
    double diff = this.offset * Math.pow(10, this.scale) - other.offset * Math.pow(10, other.scale);
    if (diff < 0) return -1;
    if (diff > 0) return 1;
    return 0;
  }
}
