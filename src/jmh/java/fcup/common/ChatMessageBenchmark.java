package fcup.common;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class ChatMessageBenchmark {

    private static final String[] RAW_MESSAGES = {
        "OK",
        "ERROR nick already in use",
        "MESSAGE alice hello everyone, how is the networking assignment going?",
        "NEWNICK alice alice_2",
        "JOINED bob",
        "LEFT bob",
        "BYE",
        "PRIVATE carol can you review my pull request before the deadline tonight?",
        "SALA alice redes",
    };

    private String longMessage;
    private ChatMessage[] parsedMessages;

    @Setup
    public void setup() {
        final StringBuilder builder = new StringBuilder("MESSAGE alice");
        for (int i = 0; i < 200; i++) {
            builder.append(" word").append(i);
        }
        longMessage = builder.toString();

        parsedMessages = new ChatMessage[RAW_MESSAGES.length];
        for (int i = 0; i < RAW_MESSAGES.length; i++) {
            parsedMessages[i] = ChatMessage.parseString(RAW_MESSAGES[i]);
        }
    }

    @Benchmark
    public void parseAllMessageTypes(final Blackhole blackhole) {
        for (final String raw : RAW_MESSAGES) {
            blackhole.consume(ChatMessage.parseString(raw));
        }
    }

    @Benchmark
    public ChatMessage parseLongMessage() {
        return ChatMessage.parseString(longMessage);
    }

    @Benchmark
    public void toRawString(final Blackhole blackhole) {
        for (final ChatMessage message : parsedMessages) {
            blackhole.consume(message.toString(false));
        }
    }

    @Benchmark
    public void toPrettyString(final Blackhole blackhole) {
        for (final ChatMessage message : parsedMessages) {
            blackhole.consume(message.toString(true));
        }
    }

    @Benchmark
    public void parseAndPrettify(final Blackhole blackhole) {
        for (final String raw : RAW_MESSAGES) {
            blackhole.consume(ChatMessage.parseString(raw).toString(true));
        }
    }
}
