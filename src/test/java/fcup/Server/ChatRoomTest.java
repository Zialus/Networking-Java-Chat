package fcup.Server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatRoomTest {

    @Test
    void getUsers() {
        ChatRoom room = new ChatRoom("OLA");

        ChatUser user1 = new ChatUser(null);
        user1.setNick("user1");
        ChatUser user2 = new ChatUser(null);
        user2.setNick("user2");
        ChatUser user3 = new ChatUser(null);
        user3.setNick("user3");
        ChatUser user4 = new ChatUser(null);
        user4.setNick("user4");

        room.userJoin(user1);
        room.userJoin(user2);
        room.userJoin(user3);
        room.userJoin(user4);
        assertEquals(4, room.getUsers().length, "Room should have 4 people");
    }

}
