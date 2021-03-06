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

package codeu.chat.common;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ConversationHeader {

  public static final Serializer<ConversationHeader> SERIALIZER =
      new Serializer<ConversationHeader>() {

        @Override
        public void write(OutputStream out, ConversationHeader value) throws IOException {

          Uuid.SERIALIZER.write(out, value.id);
          Uuid.SERIALIZER.write(out, value.creator);
          Time.SERIALIZER.write(out, value.creation);
          Serializers.STRING.write(out, value.title);
          UserType.SERIALIZER.write(out, value.defaultAccess);
        }

        @Override
        public ConversationHeader read(InputStream in) throws IOException {

          return new ConversationHeader(
              Uuid.SERIALIZER.read(in),
              Uuid.SERIALIZER.read(in),
              Time.SERIALIZER.read(in),
              Serializers.STRING.read(in),
              UserType.SERIALIZER.read(in));
        }
      };

  public final Uuid id;
  public final Uuid creator;
  public final Time creation;
  public final String title;
  public final UserType defaultAccess;

  public ConversationHeader(
      Uuid id, Uuid creator, Time creation, String title, UserType defaultAccess) {

    this.id = id;
    this.creator = creator;
    this.creation = creation;
    this.title = title;
    this.defaultAccess = defaultAccess;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Title: ");
    builder.append(title);
    builder.append("\tTime of creation: ");
    builder.append(creation.toString());
    builder.append("\n");
    return builder.toString();
  }
}
