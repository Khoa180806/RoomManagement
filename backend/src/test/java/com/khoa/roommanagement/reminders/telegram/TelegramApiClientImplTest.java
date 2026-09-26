package com.khoa.roommanagement.reminders.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test adapter với mock HTTP server thay vì gọi Telegram thật.
 * Token chỉ được phép xuất hiện trong URL gọi ra; không xuất hiện trong
 * thông báo lỗi của adapter.
 */
class TelegramApiClientImplTest {

    private static final String BOT_TOKEN = "123456:TEST-TOKEN-abcdef";

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respondWith(int status, String json) {
        server.createContext("/", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
    }

    private TelegramApiClientImpl newClient() {
        return new TelegramApiClientImpl(BOT_TOKEN, baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(2));
    }

	@Test
	void rejectsNonHttpsBaseUrlInProductionConstructor() {
		assertThatThrownBy(() -> new TelegramApiClientImpl(
			new TelegramProperties(BOT_TOKEN, "987654321", "http://api.telegram.org")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("HTTPS")
			.hasMessageNotContaining(BOT_TOKEN);
	}

	@Test
	void sendsMessageWithTokenOnlyInUrl() {
        respondWith(200, """
            {"ok": true, "result": {"message_id": 42}}
            """);

        assertThatCode(() -> newClient().sendMessage("987654321", "Tin nhắn thử"))
            .doesNotThrowAnyException();

        assertThat(requestMethod.get()).isEqualTo("POST");
        assertThat(requestPath.get()).isEqualTo("/bot" + BOT_TOKEN + "/sendMessage");
        assertThat(requestBody.get()).contains("\"chat_id\":\"987654321\"");
        assertThat(requestBody.get()).contains("\"text\":\"Tin nhắn thử\"");
    }

    @Test
    void throwsSafeErrorWhenTelegramRejects() {
        respondWith(400, """
            {"ok": false, "error_code": 400, "description": "Bad Request: chat not found"}
            """);

        assertThatThrownBy(() -> newClient().sendMessage("987654321", "Tin nhắn thử"))
            .isInstanceOf(TelegramSendException.class)
            .hasMessageContaining("400")
            .hasMessageNotContaining(BOT_TOKEN);
    }

    @Test
    void throwsSafeErrorWhenTimeout() {
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        TelegramApiClientImpl client = new TelegramApiClientImpl(
            BOT_TOKEN, baseUrl, Duration.ofSeconds(1), Duration.ofMillis(200));

        assertThatThrownBy(() -> client.sendMessage("987654321", "Tin nhắn thử"))
            .isInstanceOf(TelegramSendException.class)
            .hasMessageNotContaining(BOT_TOKEN)
            .hasMessageNotContaining(baseUrl);
    }
}
