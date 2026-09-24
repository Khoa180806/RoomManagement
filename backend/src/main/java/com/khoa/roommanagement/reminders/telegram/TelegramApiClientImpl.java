package com.khoa.roommanagement.reminders.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Triển khai adapter Telegram bằng RestClient với timeout hữu hạn.
 *
 * <p>Bot token chỉ xuất hiện trong URL gọi ra tới Telegram (cách thức API này
 * yêu cầu); nó không được ghi log, không xuất hiện trong thông báo lỗi và
 * không bao giờ trả về client.</p>
 */
@Component
public class TelegramApiClientImpl implements TelegramApiClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String botToken;
    private final RestClient restClient;

    @Autowired
    public TelegramApiClientImpl(TelegramProperties properties) {
        this(properties.botToken(), properties.apiBaseUrl(), CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    TelegramApiClientImpl(String botToken, String baseUrl, Duration connectTimeout, Duration readTimeout) {
        this.botToken = botToken;
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build();
    }

    @Override
    public void sendMessage(String chatId, String text) {
        TelegramApiResponse response;
        try {
            response = restClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .body(TelegramApiResponse.class);
        } catch (RestClientResponseException exception) {
            // retrieve() ném exception với status 4xx/5xx: đọc body lỗi để lấy
            // mã/description an toàn, không truyền exception gốc (chứa URL token).
            TelegramApiResponse errorResponse = parseResponse(exception.getResponseBodyAsString());
            if (errorResponse != null && !errorResponse.ok()) {
                throw new TelegramSendException(safeFailureMessage(errorResponse));
            }
            throw new TelegramSendException("Telegram từ chối yêu cầu với lỗi không xác định.");
        } catch (RestClientException exception) {
            // Không log/truyền exception gốc: message có thể chứa URL kèm token.
            throw new TelegramSendException("Không thể kết nối đến Telegram. Hãy thử lại sau.");
        }

        if (response == null || !response.ok()) {
            throw new TelegramSendException(safeFailureMessage(response));
        }
    }

    private TelegramApiResponse parseResponse(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(rawBody, TelegramApiResponse.class);
        } catch (JacksonException ignored) {
            return null;
        }
    }

    private String safeFailureMessage(TelegramApiResponse response) {
        if (response == null || response.errorCode() == null) {
            return "Telegram từ chối yêu cầu với lỗi không xác định.";
        }
        String description = response.description() == null ? "" : response.description();
        String sanitized = description.length() > 120 ? description.substring(0, 120) : description;
        return "Telegram từ chối yêu cầu (mã %d): %s".formatted(response.errorCode(), sanitized);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TelegramApiResponse(
        boolean ok,
        @JsonProperty("error_code") Integer errorCode,
        String description
    ) {
    }
}
