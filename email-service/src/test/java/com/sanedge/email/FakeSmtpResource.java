package com.sanedge.email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Test resource that runs a minimal fake SMTP server (220 greeting + EHLO
 * acknowledgement) on a random local port and points {@code quarkus.mailer.*}
 * + {@code smtp.health.enabled} at it. Lets the {@code SmtpHealthCheck}
 * integration test exercise a real protocol round trip without an external
 * SMTP server.
 */
public class FakeSmtpResource implements QuarkusTestResourceLifecycleManager {

    /**
     * Fixed port so the {@code SmtpHealthCheckIT} TestProfile can reference it:
     * {@code smtp.health.enabled} is a build-time property, so it must come from
     * a TestProfile (available at augmentation), not from this runtime resource.
     */
    public static final int SMTP_PORT = 25252;

    private ServerSocket server;
    private ExecutorService acceptor;
    private final AtomicBoolean running = new AtomicBoolean();

    @Override
    public Map<String, String> start() {
        try {
            // Fixed port is a deliberate tradeoff: smtp.health.enabled is a
            // build-time property that must come from the static TestProfile,
            // which therefore cannot use an ephemeral port. A BindException
            // fails the test loudly rather than silently testing the wrong
            // server.
            server = new ServerSocket(SMTP_PORT);
            running.set(true);
            acceptor = Executors.newSingleThreadExecutor();
            acceptor.submit(this::acceptLoop);
            return Map.of(
                    "quarkus.mailer.host", "127.0.0.1",
                    "quarkus.mailer.port", String.valueOf(SMTP_PORT));
        } catch (IOException e) {
            throw new RuntimeException("Failed to start fake SMTP server on port " + SMTP_PORT, e);
        }
    }

    private void acceptLoop() {
        while (running.get()) {
            try (Socket socket = server.accept()) {
                serve(socket);
            } catch (IOException e) {
                // Connection closed (stop()) or transient accept error -> keep
                // looping while the resource is still marked running.
            }
        }
    }

    private void serve(Socket socket) throws IOException {
        socket.setSoTimeout(5000);
        OutputStream out = socket.getOutputStream();
        out.write("220 fake-smtp ESMTP ready\r\n".getBytes(StandardCharsets.UTF_8));
        out.flush();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        String line = reader.readLine();
        if (line != null && line.startsWith("EHLO")) {
            out.write("250-fake-smtp\r\n250 OK\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (acceptor != null) {
            acceptor.shutdownNow();
        }
        if (server != null) {
            try {
                server.close();
            } catch (IOException ignored) {
                // closing best effort
            }
        }
    }
}
