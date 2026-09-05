package com.sanedge.email.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.email.EmailService;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.runtime.ShutdownEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    Vertx vertx;

    @Mock
    ReactiveMailer mailer;

    @Mock
    TracingMetrics tracingMetrics;

    @InjectMocks
    EmailService emailService;

    private JsonObject validEmailPayload;
    private JsonObject incompleteEmailPayload;

    @BeforeEach
    void setUp() {

        validEmailPayload = new JsonObject()
                .put("email", "test@example.com")
                .put("subject", "Test Subject")
                .put("body", "<h1>Test Body</h1>");

        incompleteEmailPayload = new JsonObject()
                .put("email", "test@example.com")
                .put("subject", "Test Subject");

    }

    @Test
    void sendEmail_shouldSucceed_withValidPayload() throws Exception {

        when(mailer.send(any(Mail.class)))
                .thenReturn(Uni.createFrom().voidItem());

        invokePrivateMethod("sendEmail", validEmailPayload);

        verify(mailer).send(any(Mail.class));
    }

    @Test
    void sendEmail_shouldNotSend_whenPayloadIsIncomplete() throws Exception {

        invokePrivateMethod("sendEmail", incompleteEmailPayload);

        verify(mailer, never()).send(any(Mail.class));
    }

    @Test
    void sendEmail_shouldHandleNullEmail() throws Exception {

        JsonObject payloadWithNullEmail = new JsonObject()
                .put("email", (String) null)
                .put("subject", "Test")
                .put("body", "Test Body");

        invokePrivateMethod("sendEmail", payloadWithNullEmail);

        verify(mailer, never()).send(any(Mail.class));
    }

    @Test
    void sendEmail_shouldHandleNullSubject() throws Exception {

        JsonObject payloadWithNullSubject = new JsonObject()
                .put("email", "test@example.com")
                .put("subject", (String) null)
                .put("body", "Test Body");

        invokePrivateMethod("sendEmail", payloadWithNullSubject);

        verify(mailer, never()).send(any(Mail.class));
    }

    @Test
    void sendEmail_shouldHandleNullBody() throws Exception {

        JsonObject payloadWithNullBody = new JsonObject()
                .put("email", "test@example.com")
                .put("subject", "Test Subject")
                .put("body", (String) null);

        invokePrivateMethod("sendEmail", payloadWithNullBody);

        verify(mailer, never()).send(any(Mail.class));
    }

    @Test
    void sendEmail_shouldCallMailerWithAnyMailObject() throws Exception {

        when(mailer.send(any(Mail.class)))
                .thenReturn(Uni.createFrom().voidItem());

        JsonObject customPayload = new JsonObject()
                .put("email", "recipient@example.com")
                .put("subject", "Custom Subject")
                .put("body", "<p>Custom HTML body</p>");

        invokePrivateMethod("sendEmail", customPayload);

        verify(mailer).send(any(Mail.class));
    }

    @Test
    void sendEmail_shouldHandleEmptyPayload() throws Exception {

        JsonObject emptyPayload = new JsonObject();

        invokePrivateMethod("sendEmail", emptyPayload);

        verify(mailer, never()).send(any(Mail.class));
    }

    @Test
    void sendEmail_shouldHandleEmptyStrings() throws Exception {

        JsonObject emptyStringPayload = new JsonObject()
                .put("email", "")
                .put("subject", "")
                .put("body", "");

        invokePrivateMethod("sendEmail", emptyStringPayload);

        verify(mailer).send(any(Mail.class));
    }

    @Test
    void sendEmail_shouldNotThrow_whenMailerFails() throws Exception {

        when(mailer.send(any(Mail.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("SMTP connection failed")));

        assertThatCode(() -> invokePrivateMethod("sendEmail", validEmailPayload))
                .doesNotThrowAnyException();

        verify(mailer).send(any(Mail.class));
    }

    @Test
    void onStop_shouldNotThrow_whenConsumerIsNull() throws Exception {

        assertThatCode(() -> invokePrivateMethod("onStop", new ShutdownEvent()))
                .doesNotThrowAnyException();
    }

    private Object invokePrivateMethod(String methodName, Object... args) throws Exception {
        Class<?>[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }
        java.lang.reflect.Method method = EmailService.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(emailService, args);
    }
}
