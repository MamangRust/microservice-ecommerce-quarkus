package com.sanedge.common.clickhouse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClickHouseClient {
    private static final Logger log = LoggerFactory.getLogger(ClickHouseClient.class);
    private static final DateTimeFormatter CH_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ConfigProperty(name = "clickhouse.host", defaultValue = "localhost") String host;
    @ConfigProperty(name = "clickhouse.http-port", defaultValue = "8123") int port;
    @ConfigProperty(name = "clickhouse.database", defaultValue = "ecommerce_stats") String database;
    @ConfigProperty(name = "clickhouse.username", defaultValue = "default") String username;
    @ConfigProperty(name = "clickhouse.password", defaultValue = "none") String password;

    @Inject Vertx vertx;

    private HttpClient httpClient;

    @PostConstruct void init() {
        httpClient = vertx.createHttpClient();
        log.info("ClickHouseClient initialized. host={}:{} db={}", host, port, database);
    }

    @PreDestroy void destroy() { if (httpClient != null) httpClient.close(); }

    public Uni<Void> execute(String sql) { return executeWithDatabase(sql, database); }
    public Uni<Void> executeNoDatabase(String sql) { return executeWithDatabase(sql, null); }

    private Uni<Void> executeWithDatabase(String sql, String db) {
        String path = buildPath(db);
        return Uni.createFrom().emitter(emitter -> {
            httpClient.request(HttpMethod.POST, port, host, path)
                    .onSuccess(req -> {
                        req.putHeader("Content-Type", "text/plain");
                        req.send(Buffer.buffer(sql), ar -> {
                            if (ar.failed()) { emitter.fail(ar.cause()); return; }
                            HttpClientResponse resp = ar.result();
                            resp.bodyHandler(body -> {
                                if (resp.statusCode() >= 400) {
                                    emitter.fail(new RuntimeException("CH error " + resp.statusCode() + ": " + body.toString()));
                                } else {
                                    emitter.complete(null);
                                }
                            });
                        });
                    })
                    .onFailure(emitter::fail);
        });
    }

    /**
     * Executes a SELECT query and returns TSV rows (each row is a String[]).
     * The SQL should NOT include FORMAT clause — this method appends
     * {@code FORMAT TabSeparatedWithNames} automatically.
     */
    public Uni<List<String[]>> query(String sql) {
        String fullSql = sql + " FORMAT TabSeparatedWithNames";
        String path = buildPath(database) + "default_format=TabSeparatedWithNames&";
        return Uni.createFrom().emitter(emitter -> {
            httpClient.request(HttpMethod.POST, port, host, path)
                    .onSuccess(req -> {
                        req.putHeader("Content-Type", "text/plain");
                        req.send(Buffer.buffer(fullSql), ar -> {
                            if (ar.failed()) { emitter.fail(ar.cause()); return; }
                            HttpClientResponse resp = ar.result();
                            resp.bodyHandler(body -> {
                                if (resp.statusCode() >= 400) {
                                    emitter.fail(new RuntimeException("CH query error " + resp.statusCode()));
                                } else {
                                    String bodyStr = body.toString();
                                    List<String[]> rows = new ArrayList<>();
                                    if (!bodyStr.isBlank()) {
                                        for (String line : bodyStr.split("\n")) {
                                            if (!line.isBlank()) rows.add(line.split("\t", -1));
                                        }
                                    }
                                    emitter.complete(rows);
                                }
                            });
                        });
                    })
                    .onFailure(emitter::fail);
        });
    }

    /**
     * Executes a SELECT query with {@code FORMAT JSON} and returns the {@code data}
     * JsonArray. Useful for stats-reader handlers that need structured JSON rows.
     * The SQL should NOT include FORMAT clause — this method appends it automatically.
     */
    public Uni<JsonArray> queryJson(String sql) {
        String fullSql = sql + " FORMAT JSON";
        String path = buildPath(database) + "default_format=JSON&";
        return Uni.createFrom().emitter(emitter -> {
            httpClient.request(HttpMethod.POST, port, host, path)
                    .onSuccess(req -> {
                        req.putHeader("Content-Type", "text/plain");
                        req.send(Buffer.buffer(fullSql), ar -> {
                            if (ar.failed()) { emitter.fail(ar.cause()); return; }
                            HttpClientResponse resp = ar.result();
                            resp.bodyHandler(body -> {
                                if (resp.statusCode() >= 400) {
                                    emitter.fail(new RuntimeException("CH query error " + resp.statusCode()));
                                } else {
                                    try {
                                        JsonObject json = new JsonObject(body.toString());
                                        JsonArray data = json.getJsonArray("data");
                                        emitter.complete(data != null ? data : new JsonArray());
                                    } catch (Exception e) {
                                        emitter.fail(new RuntimeException("CH JSON parse error: " + e.getMessage(), e));
                                    }
                                }
                            });
                        });
                    })
                    .onFailure(emitter::fail);
        });
    }

    public static String normalizeDateTime(LocalDateTime dt) { return dt != null ? dt.format(CH_DATETIME) : null; }
    public static String normalizeDateTime(String isoString) {
        if (isoString == null || isoString.isBlank()) return null;
        try { return LocalDateTime.parse(isoString).format(CH_DATETIME); } catch (Exception e) {
            return isoString.replace("T", " ").replace("Z", "").substring(0, Math.min(19, isoString.length()));
        }
    }

    private String buildPath(String db) {
        StringBuilder sb = new StringBuilder("/?");
        if (db != null && !db.isBlank()) sb.append("database=").append(urlEncode(db)).append("&");
        String auth = auth();
        if (!auth.isEmpty()) sb.append(auth).append("&");
        return sb.toString();
    }

    private String auth() {
        StringBuilder sb = new StringBuilder();
        if (username != null && !username.isBlank()) sb.append("user=").append(urlEncode(username));
        if (password != null && !password.isBlank() && !"none".equals(password)) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append("password=").append(urlEncode(password));
        }
        return sb.toString();
    }

    private static String urlEncode(String v) { return URLEncoder.encode(v, StandardCharsets.UTF_8); }
}
