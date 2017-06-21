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

package codeu.chat.util;

import java.io.IOException;

/**
 * Class handles parsing of the input strings into tokens
 */
public final class Tokenizer {
  private StringBuilder token;
  private String source;
  private int at;

  /**
   * Constructor for the Tokenizer class
   *
   * @param source the input String
   */
  public Tokenizer(String source) {
    this.source = source;
    at = 0;
    token = new StringBuilder();
  }

  /**
   * Increment to the next string in the input
   *
   * @return The next String token that was seperated by space
   */
  public String next() {
    try {
      // Skip all leading whitespace
      while (remaining() > 0 && Character.isWhitespace(peek())) {
        read();
	//ignore the result because we already know that it is a whitespace character
      }
      if (remaining() <= 0) {
        return null;
      } else if (peek() == '"') {
        return readWithQuotes();
      } else {
        return readWithNoQuotes();
      }
    } catch(IOException e) {
      return "An IOException was thrown in Tokenizer.next()";
    }
  }

  private int remaining() {
    return source.length() - at;
  }

  private char peek() throws IOException {
    if (at < source.length()) {
      return source.charAt(at);
    } else {
      throw new IOException("unexpectedly exceeded source.length()");
    }
  }

  private char read() throws IOException {
    final char c = peek();
    at += 1;
    return c;
  }

  private String readWithNoQuotes() throws IOException {
    token.setLength(0);  // clear the token
    while (remaining() > 0 && !Character.isWhitespace(peek())) {
      token.append(read());
    }
      return token.toString();
    }
	
  private String readWithQuotes() throws IOException {
    token.setLength(0);  // clear the token
    if (read() != '"') {
      throw new IOException("Strings must start with opening quote");
    }
    while (peek() != '"') {
      token.append(read());
    }
    read();  // read the closing the quote that allowed us to exit the loop
    return token.toString();
  }
}
