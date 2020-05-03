package fcup.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatRoomTest {

    private final String chatRoomName = "OLA";

    private ChatRoom chatRoomUnderTest;

    @BeforeEach
    void setUp() {
        chatRoomUnderTest = new ChatRoom(chatRoomName);
    }

    @Test
    public void testGetUsers() {
        final ChatUser user1 = new ChatUser(null);
        user1.setNick("user1");
        final ChatUser user2 = new ChatUser(null);
        user2.setNick("user2");

        chatRoomUnderTest.userJoin(user1);
        chatRoomUnderTest.userJoin(user2);

        final List<ChatUser> arrayOfUsers = new ArrayList<>();
        arrayOfUsers.add(user1);
        arrayOfUsers.add(user2);

        assertEquals(Arrays.toString(arrayOfUsers.toArray()), Arrays.toString(chatRoomUnderTest.getUsers()));
    }

    @Test
    void getName() {
        assertEquals(chatRoomName, chatRoomUnderTest.getName());
    }

    @Test
    void userJoin() {
        final ChatUser user1 = new ChatUser(null);
        user1.setNick("user1");
        final ChatUser user2 = new ChatUser(null);
        user2.setNick("user2");

        chatRoomUnderTest.userJoin(user1);
        chatRoomUnderTest.userJoin(user2);

        assertEquals(2, chatRoomUnderTest.getUsers().length, "Room should have 2 users");
    }

    @Test
    void userLeft() {
        final ChatUser user1 = new ChatUser(null);
        user1.setNick("user1");
        final ChatUser user2 = new ChatUser(null);
        user2.setNick("user2");

        chatRoomUnderTest.userJoin(user1);
        chatRoomUnderTest.userJoin(user2);
        chatRoomUnderTest.userLeft(user2);

        assertEquals(1, chatRoomUnderTest.getUsers().length, "Room should have 1 user");
    }

}
