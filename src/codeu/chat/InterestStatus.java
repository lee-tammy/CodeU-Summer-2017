package codeu.chat.common;

import codeu.chat.util.Serializers;
import codeu.chat.util.Serializer;
import java.util.HashSet;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public final class InterestStatus {
  public final HashSet<String> status;
  public final int size;
  public InterestStatus(HashSet<String> status) {
    this.status = status;
    this.size = status.size();
  }

  public static final Serializer<InterestStatus> SERIALIZER =
    new Serializer<InterestStatus>() {
      @Override
      public void write(OutputStream out, InterestStatus interestStatus) throws IOException {
        Serializers.INTEGER.write(out, interestStatus.size);
        for (String each : interestStatus.status) {
          Serializers.STRING.write(out, each);
        }
      }

      @Override
      public InterestStatus read(InputStream in) throws IOException {
        int size = Serializers.INTEGER.read(in);
        HashSet<String> status = new HashSet<>();
        for (int i = 0; i < size; i++) {
          status.add(Serializers.STRING.read(in));
        }
        return new InterestStatus(status);
      }
    };
}
