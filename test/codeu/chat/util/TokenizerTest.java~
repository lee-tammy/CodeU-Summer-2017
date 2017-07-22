package codeu.chat.util;

import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Test;

/*
 * Tester File for the Tokenizer Class
 */
public class TokenizerTest {

  /*
   * Test with no Arguments
   */
  @Test
  public void testWithNoArgs() throws IOException {
    final Tokenizer tokenizer = new Tokenizer(null);
    assertEquals(tokenizer.next(), null);
  }

  /**
   * Test with empty string
   */
  @Test
  public void testWithEmptyString() throws IOException {
    final Tokenizer tokenizer = new Tokenizer("");
    assertEquals(tokenizer.next(), null);
  }
  
  /* 
   * Test with Quotes in input
   */
  @Test
  public void testWithQuotes() throws IOException {
    final Tokenizer tokenizer = new Tokenizer("\"hello world\" \"how are you\"");
    assertEquals(tokenizer.next(), "hello world");
    assertEquals(tokenizer.next(), "how are you");
    assertEquals(tokenizer.next(), null);
  }

  /*
   * Test with no quotes in input
   */
  @Test
  public void testWithNoQuotes() throws IOException {
    final Tokenizer tokenizer = new Tokenizer("hello world how are you");
    assertEquals(tokenizer.next(), "hello");
    assertEquals(tokenizer.next(), "world");
    assertEquals(tokenizer.next(), "how");
    assertEquals(tokenizer.next(), "are");
    assertEquals(tokenizer.next(), "you");
    assertEquals(tokenizer.next(), null);
  }
}
