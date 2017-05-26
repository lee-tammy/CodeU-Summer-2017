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

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

public final class TimeTest {

  @Test
  public void testFromMs() {
    assertEquals(0, Time.fromMs(0).inMs());
    assertEquals(10, Time.fromMs(10).inMs());
  }

  @Test
  public void testDurationConversion() {
    assertEquals(10, Time.duration(Time.fromMs(0), Time.fromMs(10), -3));
    assertEquals(100, Time.duration(Time.fromMs(0), Time.fromMs(10), -4));
    assertEquals(1, Time.duration(Time.fromMs(0), Time.fromMs(1000), 0));
    assertEquals(1, Time.duration(Time.fromMs(2200), Time.fromMs(3200), 0));
  }
}
