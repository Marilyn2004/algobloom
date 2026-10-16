package com.ziyi.leetcodereviewsystem.coach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Returns requested tool calls; authorization and execution belong to the caller. */
@Component
public final class OpenAiCoachProvider implements CoachProvider {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(OpenAiCoachProvider.class);
    @Value("${coach.openai.input-usd-per-million:0.40}") private double inputUsdPerMillion=0.40;
    @Value("${coach.openai.output-usd-per-million:1.60}") private double outputUsdPerMillion=1.60;

    private static final URI ENDPOINT = URI.create("https://api.openai.com/v1/responses");
    private static final Duration TIMEOUT = Duration.ofSeconds(45);
    private final JsonMapper mapper;
    private final HttpClient client;
    private final URI endpoint;
    private final String key;
    private final String model;
    private final boolean demo;
    private final int maxOutputTokens;
    private final int maxInputChars;

    @Autowired
    public OpenAiCoachProvider(JsonMapper mapper,
            @Value("${coach.openai.api-key:}") String key,
            @Value("${coach.openai.model:gpt-4.1-mini}") String model,
            @Value("${coach.demo:false}") boolean demo,
            @Value("${coach.openai.max-output-tokens:1200}") int maxOutputTokens,
            @Value("${coach.openai.max-input-chars:24000}") int maxInputChars) {
        this(mapper, HttpClient.newBuilder().connectTimeout(TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NEVER).build(),
                ENDPOINT, key, model, demo, maxOutputTokens, maxInputChars);
    }

    // Test seam only: there is deliberately no configurable destination URL.
    OpenAiCoachProvider(JsonMapper mapper, HttpClient client, URI endpoint, String key,
                        String model, boolean demo, int maxOutputTokens, int maxInputChars) {
        this.mapper = Objects.requireNonNull(mapper);
        this.client = Objects.requireNonNull(client);
        Objects.requireNonNull(endpoint);
        boolean loopback = "http".equals(endpoint.getScheme())
                && List.of("127.0.0.1", "[::1]").contains(endpoint.getHost())
                && endpoint.getUserInfo() == null && endpoint.getFragment() == null;
        if (!ENDPOINT.equals(endpoint) && !loopback) {
            throw new IllegalArgumentException("Only OpenAI or a literal loopback test endpoint is allowed");
        }
        if (client.followRedirects() != HttpClient.Redirect.NEVER) {
            throw new IllegalArgumentException("Redirects must be disabled");
        }
        if (maxOutputTokens < 1 || maxOutputTokens > 32768 || maxInputChars < 1
                || maxInputChars > 100000 || model == null || model.isBlank()) {
            throw new IllegalArgumentException("Invalid Coach limits or model");
        }
        this.endpoint = endpoint;
        this.key = key == null ? "" : key.strip();
        this.model = model;
        this.demo = demo;
        this.maxOutputTokens = maxOutputTokens;
        this.maxInputChars = maxInputChars;
    }

    @Override
    public Reply respond(Context context) {
        Objects.requireNonNull(context);
        if (key.isBlank()) {
            if (demo) {
                return new Reply("[DEMO / 演示模式 — no AI request] "
                        + "请描述你尝试过的步骤，以及具体卡住的位置。", List.of(), true);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Coach is not configured");
        }
        // Reject instead of truncating away policy or the current message.
        if ((long) context.instructions().length() + context.input().length() + mapper.writeValueAsString(context.continuation()).length() > maxInputChars
                || context.tools().size() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coach input exceeds limit");
        }
        var tools = new ArrayList<Map<String, Object>>();
        for (var supplied : context.tools()) {
            if (!"function".equals(supplied.get("type"))
                    || !(supplied.get("name") instanceof String name) || name.isBlank()
                    || !(supplied.get("parameters") instanceof Map<?, ?>)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid function tool");
            }
            var tool = new LinkedHashMap<>(supplied);
            tool.put("strict", true);
            // The caller supplies strict-compatible schemas; do not weaken them here.
            tools.add(tool);
        }
        var body = new LinkedHashMap<String, Object>();
        body.put("model", model);
        body.put("instructions", context.instructions());
        body.put("input", context.continuation().isEmpty()?context.input():context.continuation());
        body.put("store", false);
        body.put("max_output_tokens", maxOutputTokens);
        body.put("tools", tools);
        body.put("parallel_tool_calls", false);
        try {
            String json = mapper.writeValueAsString(body);
            if (json.length() > maxInputChars + 16000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coach request exceeds limit");
            }
            Duration remaining=Duration.between(java.time.Instant.now(),context.deadline());
            if(remaining.isNegative()||remaining.isZero())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Coach request timed out");
            var request = HttpRequest.newBuilder(endpoint).timeout(remaining.compareTo(TIMEOUT)>0?TIMEOUT:remaining)
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json)).build();
            long started=System.nanoTime();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.info("Coach provider status={} elapsedMs={}",response.statusCode(),(System.nanoTime()-started)/1_000_000);
            if (response.statusCode() == 429 || response.statusCode() == 503) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Coach temporarily unavailable");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw upstream();
            }
            var parsed=parse(response.body(),tools);
            var usage=mapper.readTree(response.body()).path("usage");
            long inputTokens=Math.max(0,usage.path("input_tokens").asLong(0)),outputTokens=Math.max(0,usage.path("output_tokens").asLong(0));
            double estimate=(inputTokens*Math.max(0,inputUsdPerMillion)+outputTokens*Math.max(0,outputUsdPerMillion))/1_000_000.0;
            LOG.info("Coach provider usage inputTokens={} outputTokens={} estimatedUsd={}",inputTokens,outputTokens,estimate);
            return parsed;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Coach request interrupted");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Coach connection unavailable");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            // Never expose bodies, prompts, keys, or parsing exception details.
            throw upstream();
        }
    }

    private Reply parse(String body, List<Map<String, Object>> tools) {
        JsonNode root = mapper.readTree(body);
        if (root == null || !"completed".equals(root.path("status").asText())
                || !root.path("output").isArray()) {
            throw upstream();
        }
        var text = new StringBuilder();
        var calls = new ArrayList<ToolCall>();
        for (var item : root.path("output")) {
            if ("function_call".equals(item.path("type").asText())) {
                String id = item.path("call_id").asText("");
                String name = item.path("name").asText("");
                JsonNode arguments = item.path("arguments");
                if (id.isBlank() || tools.stream().noneMatch(t -> name.equals(t.get("name")))
                        || !arguments.isString() || calls.size() >= 5
                        || calls.stream().anyMatch(c -> c.id().equals(id))) {
                    throw upstream();
                }
                JsonNode parsedArguments = mapper.readTree(arguments.asText());
                if (parsedArguments == null || !parsedArguments.isObject()) {
                    throw upstream();
                }
                calls.add(new ToolCall(id, name, arguments.asText()));
            } else if ("message".equals(item.path("type").asText())) {
                if (!item.path("content").isArray()) throw upstream();
                for (var content : item.path("content")) {
                    if ("output_text".equals(content.path("type").asText())
                            && content.path("text").isString()) {
                        if (!text.isEmpty()) text.append("\n");
                        text.append(content.path("text").asText());
                    }
                }
            }
        }
        if (text.toString().isBlank() && calls.isEmpty()) throw upstream();
        return new Reply(text.toString(), calls, false);
    }

    private static ResponseStatusException upstream() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Invalid or failed Coach response");
    }
}
