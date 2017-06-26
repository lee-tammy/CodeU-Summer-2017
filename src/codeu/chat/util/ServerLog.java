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
import java.util.Map;

import codeu.chat.server.Controller;

public final class ServerLog {
  private File log;	
  private Map<Integer,String> lines;
  private BufferedReader in;

  /**
   * Constructor for ServerLog
   */
  public ServerLog(File log) {
    try {
      this.log = log;
      in = new BufferedReader(new FileReader(log.getAbsolutePath()));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    lines = readFile(in);
  }
  
  
  /**
   * gets the location of the log
   * @return a String that represents the location of the ServerLog
  */
  public static String createFilePath() {
    String workingDirectory = System.getProperty("user.dir");
    return workingDirectory + File.separator + "serverLog.txt";
  }
  
  /**
   * Gets the length of logs
   * @return the number of lines in the ServerLog
  */
  public int getLength() { 
    return lines.size();
  }

  /**
   * getter for Log
   * @return File log
   */
  public File getLog() {
    return log;
  }
  
  /**
   * Reads log line and create the object based on logs
   * @param index of the line in the log
   */
  public void readLine(int index, Controller controller) {
    String toParse = lines.get(index);

    // toParse check if empty or null
    if (toParse.length() == 0 || toParse == null) {
      return;
    }
    // turns the string from log into an array
    String[] ParArr = toParse.split("_");
    char commandType = toParse.charAt(0);

    try {
      if (commandType == 'M') {
        // parse a message
        controller.newMessage(Uuid.parse(ParArr[2]), Uuid.parse(ParArr[1]), 
                              Uuid.parse(ParArr[3]), ParArr[5], stringToTime(ParArr[4]) );

      } else if (commandType == 'U') {
        // parse a user
        controller.newUser(Uuid.parse(ParArr[2]), ParArr[1], stringToTime(ParArr[3]) );

      } else if (commandType == 'C') {
        // parse a conversation
        controller.newConversation(Uuid.parse(ParArr[2]), ParArr[1], 
                                   Uuid.parse(ParArr[3]), stringToTime(ParArr[4]) );
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private Time stringToTime(String s) {
    return Time.parse(s);
  }

  private Map<Integer,String> readFile(BufferedReader br) {
    String line;
    Map<Integer,String> lines = new HashMap<Integer,String>();
    try {
      int index = 0;
      while ((line = br.readLine()) != null) {
        lines.put(index, line);
        index++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return lines;
  }
}
