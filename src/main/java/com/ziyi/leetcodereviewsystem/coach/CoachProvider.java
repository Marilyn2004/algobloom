package com.ziyi.leetcodereviewsystem.coach;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface CoachProvider {
    record Context(String instructions, String input, List<Map<String, Object>> tools,
                   List<Map<String,Object>> continuation, java.time.Instant deadline) {
        public Context(String instructions,String input,List<Map<String,Object>> tools){
            this(instructions,input,tools,List.of(),java.time.Instant.now().plusSeconds(45));
        }
        public Context {
            Objects.requireNonNull(instructions);
            Objects.requireNonNull(input);
            continuation=List.copyOf(continuation);
            Objects.requireNonNull(deadline);
            tools = tools == null ? List.of() : tools.stream().map(Map::copyOf).toList();
        }
    }

    record Reply(String text, List<ToolCall> calls, boolean demo) {
        public Reply {
            calls = List.copyOf(calls);
        }
    }

    record ToolCall(String id, String name, String arguments) { }

    Reply respond(Context context);
}
