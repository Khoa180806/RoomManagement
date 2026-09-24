package com.khoa.roommanagement.reminders.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.khoa.roommanagement.reminders.telegram.TelegramApiClient;
import com.khoa.roommanagement.reminders.telegram.TelegramNotConfiguredException;
import com.khoa.roommanagement.reminders.telegram.TelegramProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramTestMessageServiceTest {

    @Mock
    private TelegramApiClient apiClient;

    private final TelegramProperties properties = new TelegramProperties(
        "123456:TEST-TOKEN", "987654321", TelegramProperties.DEFAULT_API_BASE_URL);

    private TelegramTestMessageService service;

    @BeforeEach
    void setUp() {
        service = new TelegramTestMessageService(apiClient, properties);
    }

    @Test
    void sendsTestMessageToConfiguredChat() {
        service.sendTestMessage();

        verify(apiClient).sendMessage(eq("987654321"), anyString());
    }

    @Test
    void throwsWhenNotConfigured() {
        TelegramProperties empty = new TelegramProperties("", "", TelegramProperties.DEFAULT_API_BASE_URL);
        TelegramTestMessageService unconfigured = new TelegramTestMessageService(apiClient, empty);

        assertThatThrownBy(unconfigured::sendTestMessage)
            .isInstanceOf(TelegramNotConfiguredException.class);

        verifyNoInteractions(apiClient);
    }
}
