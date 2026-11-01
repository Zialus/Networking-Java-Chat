package fcup.server;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class ChatRoomBenchmark {

    @Param({"10", "1000"})
    private int userCount;

    private ChatUser[] users;
    private ChatRoom populatedRoom;

    @Setup
    public void setup() {
        users = new ChatUser[userCount];
        for (int i = 0; i < userCount; i++) {
            final ChatUser user = new ChatUser(null);
            user.setNick("user" + i);
            users[i] = user;
        }

        populatedRoom = new ChatRoom("lobby");
        for (final ChatUser user : users) {
            populatedRoom.userJoin(user);
        }
    }

    @Benchmark
    public ChatRoom joinAllUsers() {
        final ChatRoom room = new ChatRoom("lobby");
        for (final ChatUser user : users) {
            room.userJoin(user);
        }
        return room;
    }

    @Benchmark
    public ChatRoom joinThenLeaveAllUsers() {
        final ChatRoom room = new ChatRoom("lobby");
        for (final ChatUser user : users) {
            room.userJoin(user);
        }
        for (final ChatUser user : users) {
            room.userLeft(user);
        }
        return room;
    }

    @Benchmark
    public ChatUser[] getUsers() {
        return populatedRoom.getUsers();
    }
}
