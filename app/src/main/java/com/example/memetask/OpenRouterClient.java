package com.example.memetask;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class OpenRouterClient {
    private static final String TAG = "OpenRouterClient";
    private static final String BASE_URL = "https://openrouter.ai/api/v1/";
    private static final String MODEL = "openrouter/free";

    private final String apiKey;
    private final OpenRouterApi api;

    public OpenRouterClient(String apiKey) {
        this.apiKey = apiKey;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.api = retrofit.create(OpenRouterApi.class);
    }

    public List<String> requestTags(JSONObject weeklyEntriesJson) throws IOException, JSONException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IOException("OpenRouter API key is empty");
        }

        OpenRouterRequest requestBody = buildRequestBody(weeklyEntriesJson);
        Call<OpenRouterResponse> call = api.createCompletion("Bearer " + apiKey, requestBody);
        Response<OpenRouterResponse> response = call.execute();

        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
            throw new IOException("OpenRouter error " + response.code() + ": " + errorBody);
        }

        OpenRouterResponse body = response.body();
        if (body == null || body.choices == null || body.choices.isEmpty()) {
            return new ArrayList<>();
        }

        OpenRouterMessage message = body.choices.get(0).message;
        String content = message != null && message.content != null ? message.content : "";
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

    private OpenRouterRequest buildRequestBody(JSONObject weeklyEntriesJson) {
        String systemPrompt = "You classify mood data and return meme tags. " +
                "Prioritize newer entries over older entries when inferring tags. " +
                "Return ONLY JSON: {\"tags\":[\"tag1\",\"tag2\",\"tag3\"]}. " +
                "Use short lowercase tags in Russian.";
        String userPrompt = "Weekly entries JSON:\n" + weeklyEntriesJson;

        OpenRouterRequest request = new OpenRouterRequest();
        request.model = MODEL;
        request.temperature = 0.2;
        request.messages = new ArrayList<>();
        request.messages.add(new OpenRouterMessage("system", systemPrompt));
        request.messages.add(new OpenRouterMessage("user", userPrompt));
        return request;
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

    interface OpenRouterApi {
        @Headers({
                "Content-Type: application/json",
                "HTTP-Referer: https://memetask.local",
                "X-Title: MemeTask"
        })
        @POST("chat/completions")
        Call<OpenRouterResponse> createCompletion(
                @Header("Authorization") String authorization,
                @Body OpenRouterRequest body
        );
    }

    static class OpenRouterRequest {
        String model;
        double temperature;
        List<OpenRouterMessage> messages;
    }

    static class OpenRouterResponse {
        List<OpenRouterChoice> choices;
    }

    static class OpenRouterChoice {
        OpenRouterMessage message;
    }

    static class OpenRouterMessage {
        String role;
        String content;

        OpenRouterMessage() {
        }

        OpenRouterMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
