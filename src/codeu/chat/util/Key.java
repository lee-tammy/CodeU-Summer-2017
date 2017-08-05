package codeu.chat.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Key {

  public static String encrypt(String password) {
    String ePassword = "";
    int length = password.length();
    char ch;

    for (int i = 0; i <  length; i++) {
      ch = password.charAt(i);
      ch += 20;
      ePassword += ch;
    }
    return ePassword;
  }

  public static boolean pass(String pw, String ep) {
    return pw.equals(decrypt(ep));
  }

  private static String decrypt(String key) {
    String password = "";
    int length = key.length();
    char ch;

    for (int i = 0; i <  length; i++) {
      ch = key.charAt(i);
      ch -= 20;
      password += ch;
    }
    return password;
  }
}
