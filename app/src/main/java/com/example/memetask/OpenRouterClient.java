package com.example.memetask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
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
    private static final String HTTP_TAG = "OpenRouterHttp";
    private static final String BASE_URL = "https://openrouter.ai/api/v1/";
    private static final String PRIMARY_MODEL = "openrouter/free";
    private static final String[] FALLBACK_MODELS = {
            "stepfun/step-3.5-flash:free",
            "arcee-ai/trinity-mini:free",
            "nvidia/nemotron-nano-9b-v2:free"
    };

    private final String apiKey;
    private final OpenRouterApi api;

    public OpenRouterClient(String apiKey) {
        this.apiKey = apiKey;
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new DetailedLoggingInterceptor())
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.api = retrofit.create(OpenRouterApi.class);
        AiLogger.i(TAG, "OpenRouterClient initialized baseUrl=" + BASE_URL
                + ", model=" + PRIMARY_MODEL
                + ", fallbackModels=" + joinModels(FALLBACK_MODELS)
                + ", apiKey=" + AiLogger.redactToken(apiKey));
    }

    public List<String> requestTags(JSONObject weeklyEntriesJson, List<String> allowedTags)
            throws IOException, JSONException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            AiLogger.e(TAG, "requestTags aborted: API key is empty", null);
            throw new IOException("OpenRouter API key is empty");
        }
        if (allowedTags == null || allowedTags.isEmpty()) {
            AiLogger.w(TAG, "requestTags aborted: allowedTags is empty");
            return new ArrayList<>();
        }

        String requestId = "req-" + Long.toHexString(System.currentTimeMillis());
        long startedAtNs = System.nanoTime();
        AiLogger.i(TAG, requestId + " requestTags started");
        AiLogger.d(TAG, requestId + " weeklyEntriesJson=" + weeklyEntriesJson);
        AiLogger.d(TAG, requestId + " allowedTags=" + allowedTags);

        List<String> modelPlan = buildModelPlan();
        IOException lastException = null;
        for (int attempt = 0; attempt < modelPlan.size(); attempt++) {
            String modelName = modelPlan.get(attempt);
            OpenRouterRequest requestBody = buildRequestBody(
                    weeklyEntriesJson,
                    allowedTags,
                    modelName,
                    modelPlan,
                    attempt
            );
            logRequestBody(requestId + "#attempt" + (attempt + 1), requestBody);

            try {
                Call<OpenRouterResponse> call = api.createCompletion("Bearer " + apiKey, requestBody);
                Response<OpenRouterResponse> response = call.execute();
                long durationMs = (System.nanoTime() - startedAtNs) / 1_000_000L;
                AiLogger.i(TAG, requestId + " attempt=" + (attempt + 1)
                        + " model=" + modelName
                        + " HTTP completed code=" + response.code()
                        + ", success=" + response.isSuccessful()
                        + ", durationMs=" + durationMs);

                if (!response.isSuccessful()) {
                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                    AiLogger.w(TAG, requestId + " attempt=" + (attempt + 1)
                            + " model=" + modelName
                            + " OpenRouter errorBody=" + errorBody);

                    IOException requestException =
                            new IOException("OpenRouter error " + response.code() + ": " + errorBody);
                    if (shouldRetryWithAnotherModel(response.code(), errorBody) && attempt < modelPlan.size() - 1) {
                        AiLogger.w(TAG, requestId + " attempt=" + (attempt + 1)
                                + " will retry on next model due to retryable provider/rate-limit error");
                        lastException = requestException;
                        continue;
                    }
                    throw requestException;
                }

                OpenRouterResponse body = response.body();
                if (body == null || body.choices == null || body.choices.isEmpty()) {
                    AiLogger.w(TAG, requestId + " attempt=" + (attempt + 1)
                            + " model=" + modelName
                            + " empty response body or choices");
                    return new ArrayList<>();
                }

                OpenRouterMessage message = body.choices.get(0).message;
                String content = message != null && message.content != null ? message.content : "";
                AiLogger.d(TAG, requestId + " attempt=" + (attempt + 1)
                        + " model=" + modelName
                        + " raw model content=" + content);
                JSONObject tagsObject = extractTagsObject(content);
                AiLogger.d(TAG, requestId + " attempt=" + (attempt + 1)
                        + " model=" + modelName
                        + " extracted tags object=" + tagsObject);
                JSONArray tagsArray = tagsObject.optJSONArray("tags");

                List<String> tags = new ArrayList<>();
                if (tagsArray == null) {
                    AiLogger.w(TAG, requestId + " attempt=" + (attempt + 1)
                            + " model=" + modelName
                            + " tags array missing in model output");
                    return tags;
                }
                for (int i = 0; i < tagsArray.length(); i++) {
                    String tag = tagsArray.optString(i, "").trim();
                    if (!tag.isEmpty()) {
                        tags.add(tag);
                    }
                }
                tags = sanitizeTags(tags, allowedTags);
                AiLogger.i(TAG, requestId + " attempt=" + (attempt + 1)
                        + " model=" + modelName
                        + " tagsFromModel=" + tags);
                return tags;
            } catch (IOException exception) {
                if (attempt < modelPlan.size() - 1 && shouldRetryWithAnotherModel(-1, exception.getMessage())) {
                    AiLogger.w(TAG, requestId + " attempt=" + (attempt + 1)
                            + " model=" + modelName
                            + " failed with retryable IO error, switching model", exception);
                    lastException = exception;
                    continue;
                }
                throw exception;
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new IOException("OpenRouter request failed without a concrete exception");
    }

    private OpenRouterRequest buildRequestBody(
            JSONObject weeklyEntriesJson,
            List<String> allowedTags,
            String modelName,
            List<String> modelPlan,
            int currentAttempt
    ) {
        String systemPrompt = "You classify mood data and return meme tags. "
                + "Prioritize newer entries over older entries when inferring tags. "
                + "You must choose tags only from the provided allowed_tags list. "
                + "Do not invent, translate, rephrase, or normalize tags outside that list. "
                + "Return ONLY JSON: {\"tags\":[\"tag1\",\"tag2\",\"tag3\"]}. "
                + "Use short lowercase tags in Russian.";
        String userPrompt = "Allowed tags:\n" + new JSONArray(allowedTags)
                + "\n\nWeekly entries JSON:\n" + weeklyEntriesJson
                + "\n\nChoose 1 to 3 tags only from allowed tags.";

        OpenRouterRequest request = new OpenRouterRequest();
        request.model = modelName;
        request.temperature = 0.2;
        request.models = buildRemainingModels(modelPlan, currentAttempt + 1);
        request.messages = new ArrayList<>();
        request.messages.add(new OpenRouterMessage("system", systemPrompt));
        request.messages.add(new OpenRouterMessage("user", userPrompt));
        return request;
    }

    private List<String> sanitizeTags(List<String> rawTags, List<String> allowedTags) {
        Set<String> allowedLookup = new LinkedHashSet<>();
        for (String allowedTag : allowedTags) {
            if (allowedTag != null && !allowedTag.trim().isEmpty()) {
                allowedLookup.add(allowedTag.trim().toLowerCase(Locale.US));
            }
        }

        List<String> sanitizedTags = new ArrayList<>();
        for (String rawTag : rawTags) {
            if (rawTag == null) {
                continue;
            }
            String normalizedTag = rawTag.trim().toLowerCase(Locale.US);
            if (!allowedLookup.contains(normalizedTag)) {
                AiLogger.w(TAG, "Model returned tag outside whitelist: " + rawTag);
                continue;
            }
            if (!sanitizedTags.contains(normalizedTag)) {
                sanitizedTags.add(normalizedTag);
            }
        }
        return sanitizedTags;
    }

    private JSONObject extractTagsObject(String content) throws JSONException {
        String cleaned = content == null ? "" : content.trim();
        AiLogger.d(TAG, "extractTagsObject rawLength=" + cleaned.length());

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
        AiLogger.w(TAG, "extractTagsObject failed to find JSON object in model content");
        return new JSONObject().put("tags", new JSONArray());
    }

    private void logRequestBody(String requestId, OpenRouterRequest requestBody) {
        StringBuilder builder = new StringBuilder();
        builder.append(requestId)
                .append(" requestBody.model=").append(AiLogger.safe(requestBody.model))
                .append(", fallbackModels=").append(joinModels(requestBody.models))
                .append(", temperature=").append(requestBody.temperature)
                .append(", messages=").append(requestBody.messages == null ? 0 : requestBody.messages.size());
        if (requestBody.messages != null) {
            for (int index = 0; index < requestBody.messages.size(); index++) {
                OpenRouterMessage message = requestBody.messages.get(index);
                builder.append("\nmessage[").append(index).append("].role=")
                        .append(AiLogger.safe(message.role))
                        .append("\nmessage[").append(index).append("].content=")
                        .append(AiLogger.safe(message.content));
            }
        }
        AiLogger.d(TAG, builder.toString());
    }

    private List<String> buildModelPlan() {
        List<String> models = new ArrayList<>();
        models.add(PRIMARY_MODEL);
        for (String fallbackModel : FALLBACK_MODELS) {
            if (!models.contains(fallbackModel)) {
                models.add(fallbackModel);
            }
        }
        return models;
    }

    private List<String> buildRemainingModels(List<String> modelPlan, int startIndex) {
        List<String> remainingModels = new ArrayList<>();
        for (int index = startIndex; index < modelPlan.size(); index++) {
            remainingModels.add(modelPlan.get(index));
        }
        return remainingModels;
    }

    private boolean shouldRetryWithAnotherModel(int statusCode, String errorBodyOrMessage) {
        String message = errorBodyOrMessage == null ? "" : errorBodyOrMessage.toLowerCase(Locale.US);
        if (statusCode == 429 || statusCode == 503 || statusCode == 502) {
            return true;
        }
        return message.contains("temporarily rate-limited")
                || message.contains("provider returned error")
                || message.contains("rate-limited upstream")
                || message.contains("too many requests")
                || message.contains("provider error")
                || message.contains("timeout")
                || message.contains("connection reset")
                || message.contains("unexpected end of stream");
    }

    private String joinModels(List<String> models) {
        if (models == null || models.isEmpty()) {
            return "[]";
        }
        return models.toString();
    }

    private String joinModels(String[] models) {
        if (models == null || models.length == 0) {
            return "[]";
        }
        List<String> asList = new ArrayList<>();
        for (String model : models) {
            asList.add(model);
        }
        return asList.toString();
    }

    private static final class DetailedLoggingInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long startedAtNs = System.nanoTime();
            AiLogger.i(HTTP_TAG, "HTTP request " + request.method() + " " + request.url());
            AiLogger.d(HTTP_TAG, "HTTP request headers:\n" + redactHeaders(request));
            AiLogger.d(HTTP_TAG, "HTTP request body:\n" + readRequestBody(request.body()));

            try {
                okhttp3.Response response = chain.proceed(request);
                long durationMs = (System.nanoTime() - startedAtNs) / 1_000_000L;
                ResponseBody peekBody = response.peekBody(1024 * 1024);
                AiLogger.i(HTTP_TAG, "HTTP response code=" + response.code()
                        + ", message=" + response.message()
                        + ", successful=" + response.isSuccessful()
                        + ", durationMs=" + durationMs);
                AiLogger.d(HTTP_TAG, "HTTP response headers:\n" + response.headers());
                AiLogger.d(HTTP_TAG, "HTTP response body:\n" + (peekBody != null ? peekBody.string() : "<empty>"));
                return response;
            } catch (IOException exception) {
                long durationMs = (System.nanoTime() - startedAtNs) / 1_000_000L;
                AiLogger.e(HTTP_TAG, "HTTP request failed after " + durationMs + " ms", exception);
                throw exception;
            }
        }

        private String readRequestBody(RequestBody body) {
            if (body == null) {
                return "<empty>";
            }
            try {
                Buffer buffer = new Buffer();
                body.writeTo(buffer);
                return buffer.readUtf8();
            } catch (Exception exception) {
                return "unable to read request body: " + exception.getMessage();
            }
        }

        private String redactHeaders(Request request) {
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < request.headers().size(); index++) {
                String name = request.headers().name(index);
                String value = request.headers().value(index);
                if ("Authorization".equalsIgnoreCase(name)) {
                    value = redactAuthorizationValue(value);
                }
                builder.append(name).append(": ").append(value).append('\n');
            }
            return builder.toString().trim();
        }

        private String redactAuthorizationValue(String value) {
            if (value == null || value.trim().isEmpty()) {
                return "<empty>";
            }
            String trimmed = value.trim();
            if (!trimmed.toLowerCase(Locale.US).startsWith("bearer ")) {
                return AiLogger.redactToken(trimmed);
            }
            String token = trimmed.substring("Bearer ".length());
            return "Bearer " + AiLogger.redactToken(token);
        }
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
        List<String> models;
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
