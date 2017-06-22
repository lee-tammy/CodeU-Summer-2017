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
import java.util.ArrayList;
import java.util.HashMap;

public final class ServerLog { 
  private HashMap<Integer,String> lines;
  private BufferedReader in;

  public ServerLog(File log) {
	try {
	  in = new BufferedReader(new FileReader(log.getAbsolutePath()));
	} catch (FileNotFoundException e) {
	  e.printStackTrace();
	}
	lines = readFile(in);
  }
  
  
  /**
   * @return a String that represents the location of the ServerLog
  */
  public static String createFilePath() {
	String workingDirectory = System.getProperty("user.dir");
	return workingDirectory + File.separator + "serverLog.txt";
  }
  
  /**
   * @return the number of lines in the ServerLog
  */
  public int getLength() { 
	return lines.size();
  }
  
  public String[] readLine(int index) {
	String toParse = lines.get(index);
	char commandType = toParse.charAt(0);

	if(commandType == 'M') {
	  // parse a message
	} else if (commandType == 'U') {
		// parse a user
	} else if (commandType == 'C') {
		// parse a conversation
	}
	return null;
  }

  private HashMap<Integer,String> readFile(BufferedReader br) {
    String line;
    HashMap<Integer,String> lines = new HashMap<Integer,String>();
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
