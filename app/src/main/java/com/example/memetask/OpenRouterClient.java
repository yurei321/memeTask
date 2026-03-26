package com.example.memetask;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class OpenRouterClient {
    private static final String TAG = "OpenRouterClient";
    private static final String ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "openrouter/free";

    private final String apiKey;

    public OpenRouterClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public List<String> requestTags(JSONObject weeklyEntriesJson) throws IOException, JSONException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IOException("OpenRouter API key is empty");
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("HTTP-Referer", "https://memetask.local");
        connection.setRequestProperty("X-Title", "MemeTask");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);

        JSONObject requestBody = buildRequestBody(weeklyEntriesJson);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String responseText = readAll(stream);
        if (status < 200 || status >= 300) {
            throw new IOException("OpenRouter error " + status + ": " + responseText);
        }

        return parseTagsFromResponse(responseText);
    }

    private JSONObject buildRequestBody(JSONObject weeklyEntriesJson) throws JSONException {
        String systemPrompt = "You classify mood data and return meme tags. " +
                "Prioritize newer entries over older entries when inferring tags. " +
                "Return ONLY JSON: {\"tags\":[\"tag1\",\"tag2\",\"tag3\"]}. " +
                "Use short lowercase tags in Russian.";
        String userPrompt = "Weekly entries JSON:\n" + weeklyEntriesJson;

        JSONArray messages = new JSONArray()
                .put(new JSONObject().put("role", "system").put("content", systemPrompt))
                .put(new JSONObject().put("role", "user").put("content", userPrompt));

        return new JSONObject()
                .put("model", MODEL)
                .put("temperature", 0.2)
                .put("messages", messages);
    }

    private List<String> parseTagsFromResponse(String responseText) throws JSONException {
        JSONObject root = new JSONObject(responseText);
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return new ArrayList<>();
        }

        String content = choices.getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "");

        JSONObject tagsObject = extractTagsObject(content);
        JSONArray tagsArray = tagsObject.optJSONArray("tags");
        List<String> tags = new ArrayList<>();
        if (tagsArray == null) {
            return tags;
        }
        for (int i = 0; i < tagsArray.length(); i++) {
            String tag = tagsArray.optString(i, "").trim();
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        Log.d(TAG, "tagsFromModel=" + tags);
        return tags;
    }

    private JSONObject extractTagsObject(String content) throws JSONException {
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            int firstNewLine = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNewLine > 0 && lastFence > firstNewLine) {
                cleaned = cleaned.substring(firstNewLine + 1, lastFence).trim();
            }
        }

        if (cleaned.startsWith("{")) {
            return new JSONObject(cleaned);
        }

        int objStart = cleaned.indexOf('{');
        int objEnd = cleaned.lastIndexOf('}');
        if (objStart >= 0 && objEnd > objStart) {
            return new JSONObject(cleaned.substring(objStart, objEnd + 1));
        }
        return new JSONObject().put("tags", new JSONArray());
    }

    private String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}
