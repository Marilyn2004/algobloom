package com.ziyi.leetcodereviewsystem.coach;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CoachProviderTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final AtomicReference<String> request = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicInteger hits = new AtomicInteger();
    private HttpServer server;
    private URI endpoint;
    private int status;
    private String response;

    @BeforeEach
    void start() throws Exception {
        status = 200;
        response = """
                {"status":"completed","output":[
                  {"type":"message","content":[{"type":"output_text","text":"A hint"}]}
                ]}
                """;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            hits.incrementAndGet();
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            } finally {
                exchange.close();
            }
        });
        server.start();
        endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses");
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    private OpenAiCoachProvider provider(String key, boolean demo, int limit) {
        return new OpenAiCoachProvider(mapper, HttpClient.newHttpClient(), endpoint,
                key, "gpt-4.1-mini", demo, 777, limit);
    }

    private CoachProvider.Context context() {
        return new CoachProvider.Context("Tutor policy", "A synthetic question", List.of());
    }

    @Test
    void sendsResponsesContractAndParsesNestedText() {
        var reply = provider("synthetic-test-key", false, 24000).respond(context());
        var body = mapper.readTree(request.get());
        assertEquals("gpt-4.1-mini", body.path("model").asText());
        assertFalse(body.path("store").asBoolean());
        assertEquals(777, body.path("max_output_tokens").asInt());
        assertEquals("Tutor policy", body.path("instructions").asText());
        assertEquals("A synthetic question", body.path("input").asText());
        assertEquals("Bearer synthetic-test-key", authorization.get());
        assertEquals("A hint", reply.text());
        assertFalse(reply.demo());
    }

    @Test
    void sendsStrictFunctionAndUsesCallIdRatherThanOutputItemId() {
        response = """
                {"status":"completed","output":[{"type":"function_call","id":"fc_item",
                "call_id":"call_1","name":"get_context","arguments":"{}"}]}
                """;
        Map<String, Object> tool = Map.of("type", "function", "name", "get_context",
                "parameters", Map.of("type", "object", "properties", Map.of(),
                        "required", List.of(), "additionalProperties", false));
        var reply = provider("synthetic-test-key", false, 24000).respond(
                new CoachProvider.Context("policy", "input", List.of(tool)));
        assertTrue(mapper.readTree(request.get()).path("tools").get(0).path("strict").asBoolean());
        assertEquals(List.of(new CoachProvider.ToolCall("call_1", "get_context", "{}")), reply.calls());
        assertFalse(tool.containsKey("strict"));
    }

    @Test
    void missingKeyFailsUnlessDemoIsExplicit() {
        var failure = assertThrows(ResponseStatusException.class,
                () -> provider("", false, 24000).respond(context()));
        assertEquals(503, failure.getStatusCode().value());
        var demo = provider("", true, 24000).respond(context());
        assertTrue(demo.demo());
        assertTrue(demo.text().contains("DEMO"));
        assertEquals(demo, provider("", true, 24000).respond(context()));
        assertEquals(0, hits.get());
    }

    @Test
    void rejectsOversizedInputBeforeHttp() {
        var failure = assertThrows(ResponseStatusException.class,
                () -> provider("synthetic-test-key", false, 2).respond(context()));
        assertEquals(400, failure.getStatusCode().value());
        assertEquals(0, hits.get());
    }

    @Test
    void upstreamFailureDoesNotExposeResponseBody() {
        status = 401;
        response = "sensitive-upstream-detail";
        var failure = assertThrows(ResponseStatusException.class,
                () -> provider("synthetic-test-key", false, 24000).respond(context()));
        assertEquals(502, failure.getStatusCode().value());
        assertFalse(failure.toString().contains(response));
    }

    @Test
    void rejectsMalformedIncompleteAndUnrequestedCalls() {
        for (String invalid : List.of("not-json",
                "{\"status\":\"incomplete\",\"output\":[]}",
                "{\"status\":\"completed\",\"output\":[{\"type\":\"function_call\","
                        + "\"call_id\":\"x\",\"name\":\"not_supplied\",\"arguments\":\"{}\"}]}")) {
            response = invalid;
            assertEquals(502, assertThrows(ResponseStatusException.class,
                    () -> provider("synthetic-test-key", false, 24000).respond(context()))
                    .getStatusCode().value());
        }
    }
}
