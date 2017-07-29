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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import codeu.chat.common.UserType;
import codeu.chat.server.Controller;

public final class ServerLog {
  private File log;

  /** Constructor for ServerLog */
  public ServerLog(File log) {
      this.log = log;
  }

  /**
   * gets the location of the log
   *
   * @return a String that represents the location of the ServerLog
   */
  public static String createFilePath() {
    String workingDirectory = System.getProperty("user.dir");
    return workingDirectory + File.separator + "serverLog.json";
  }


  /**
   * getter for Log
   *
   * @return File log
   */
  public File getLog() {
    return log;
  }
  
}
