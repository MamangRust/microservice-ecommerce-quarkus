package com.sanedge.gateway.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

/**
 * RESTEasy/Jackson does not know how to construct generated protobuf messages.
 * Use protobuf's canonical JSON parser for command request bodies instead.
 *
 * Accepts both camelCase and snake_case field names by preprocessing the JSON.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Priority(Priorities.ENTITY_CODER)
public class ProtobufJsonMessageBodyReader implements MessageBodyReader<Message> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Message.class.isAssignableFrom(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Message readFrom(Class<Message> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream) throws IOException, WebApplicationException {
        try (InputStreamReader reader = new InputStreamReader(entityStream, StandardCharsets.UTF_8)) {
            // Read entire JSON and convert camelCase to snake_case
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            String json = camelToSnakeJson(sb.toString());

            Method newBuilder = type.getMethod("newBuilder");
            Message.Builder builder = (Message.Builder) newBuilder.invoke(null);
            JsonFormat.parser().merge(new java.io.StringReader(json), builder);
            return builder.build();
        } catch (ReflectiveOperationException | IOException e) {
            throw new WebApplicationException(
                    "Invalid protobuf JSON payload: " + e.getMessage(),
                    e,
                    Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Convert camelCase JSON keys to snake_case.
     * Skips keys inside quoted string values.
     */
    static String camelToSnakeJson(String json) {
        StringBuilder out = new StringBuilder(json.length());
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                out.append(c);
                escape = false;
                continue;
            }
            if (c == '\\' && inString) {
                out.append(c);
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                out.append(c);
                continue;
            }
            if (inString) {
                out.append(c);
                continue;
            }
            // Outside strings: look for key patterns like "camelCase":
            // A key starts after '{' or ',' and ends with ':'
            if (c == '"' && !inString) {
                // Start of a key
                int keyStart = i;
                i++; // skip opening quote
                StringBuilder key = new StringBuilder();
                while (i < json.length() && json.charAt(i) != '"') {
                    key.append(json.charAt(i));
                    i++;
                }
                // i is at closing quote
                // Check if this is followed by ":"
                int colonPos = i + 1;
                while (colonPos < json.length() && Character.isWhitespace(json.charAt(colonPos))) {
                    colonPos++;
                }
                if (colonPos < json.length() && json.charAt(colonPos) == ':') {
                    // This is a key — convert to snake_case
                    out.append('"');
                    out.append(toSnakeCase(key.toString()));
                    out.append('"');
                    i++; // skip closing quote
                } else {
                    // Not a key (e.g., string value), output as-is
                    out.append('"');
                    out.append(key);
                    out.append('"');
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    static String toSnakeCase(String camel) {
        if (camel.isEmpty()) return camel;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
