package codeu.chat.relay;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.Relay;
import codeu.chat.common.Secret;
import codeu.chat.common.User;
import codeu.chat.server.Controller;
import codeu.chat.server.Model;
import codeu.chat.util.ServerLog;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

import org.junit.Before;


public final class LogTest {
  private Model model;
  private Controller controller;
  private ServerLog log;

  @Before
  public void doBefore() {
	model = new Model();
	try {
	  controller = new Controller(Uuid.NULL, model);
	} catch (IOException e) {
			e.printStackTrace();
	}
	log = new ServerLog(new File(ServerLog.createFilePath()));
  }
  
  @Test
  public void testRestoreConversation() {
	  
	final User user = controller.newUser("user");

	assertFalse(
	    "Check that user has a valid reference",
	    user == null);

	final ConversationHeader conversation = controller.newConversation(
	    "conversation",
	    user.id);

	assertFalse(
	    "Check that conversation has a valid reference",
	    conversation == null);
  }

  @Test
  public void testWriteSuccess() {

    final Server relay = new Server(8, 8);

	final Uuid team = new Uuid(3);
	final Secret secret = new Secret((byte)0x00, (byte)0x01, (byte)0x02);

	assertTrue(relay.addTeam(team, secret));

	assertTrue(relay.write(team,
	                       secret,
	                       relay.pack(new Uuid(4), "User", Time.now()),
	                       relay.pack(new Uuid(5), "Conversation", Time.now()),
	                       relay.pack(new Uuid(6), "Hello World", Time.now())));
	}

}
