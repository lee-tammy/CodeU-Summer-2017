package codeu.chat.util;

import java.lang.StringBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public final class Password {

  public static final Password NULL = new Password(null);

  public static final Serializer<Password> SERIALIZER = new Serializer<Password>() {

    @Override
    public void write(OutputStream out, Password value) throws IOException {
      BYTES.write(out, value.getBytes());
    }

    @Override
    public String read(InputStream input) throws IOException {
      return new Password(BYTES.read(input));
    }
  };

  public static final Serializer<byte[]> BYTES =
    new Serializer<byte[]>() {

    @Override
    public void write(OutputStream out, byte[] value) throws IOException {

      INTEGER.write(out, value.length);
      out.write(value);
    }

    @Override
    public byte[] read(InputStream input) throws IOException {

      final int length = INTEGER.read(input);
      final byte[] array = new byte[length];

      for (int i = 0; i < length; i++) {
        array[i] = (byte) input.read();
      }

      return array;
    }
  };

  public static final Serializer<Integer> INTEGER =
      new Serializer<Integer>() {

        @Override
        public void write(OutputStream out, Integer value) throws IOException {

          for (int i = 24; i >= 0; i -= 8) {
            out.write(0xFF & (value >>> i));
          }
        }

        @Override
        public Integer read(InputStream in) throws IOException {

          int value = 0;

          for (int i = 0; i < 4; i++) {
            value = (value << 8) | in.read();
          }

          return value;
        }
      };
  private final String ePassword;

  public Password(String ePassword) {
    this.ePassword = ePassword;  
  }

  public String toString() {
    return ePassword;
  }
}

