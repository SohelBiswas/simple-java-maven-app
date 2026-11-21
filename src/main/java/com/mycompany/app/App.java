package com.mycompany.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * A tiny web service that stands in for Chapter's backend until the real code arrives.
 *
 *   GET /        returns "Hello World!"
 *   GET /health  returns status, environment and version (the pipeline checks this after each deploy)
 */
public class App {

    private static final String MESSAGE = "Hello World!";
    private static final Logger LOG = Logger.getLogger(App.class.getName());

    public App() {}

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(env("PORT", "8080"));
        HttpServer server = start(port, env("APP_ENV", "local"), env("APP_VERSION", "dev"));
        LOG.info(() -> "Listening on port " + server.getAddress().getPort());
    }

    /** Starts the web server. Port 0 picks a free port (used by the tests). */
    static HttpServer start(int port, String environment, String version) throws IOException {
        App app = new App();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", exchange ->
                respond(exchange, "application/json", healthJson(environment, version)));
        server.createContext("/", exchange ->
                respond(exchange, "text/plain", app.getMessage()));
        server.start();
        return server;
    }

    static String healthJson(String environment, String version) {
        return "{\"status\":\"UP\",\"environment\":\"" + environment
                + "\",\"version\":\"" + version + "\"}";
    }

    private static void respond(HttpExchange exchange, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** Reads a setting from the environment, or returns the fallback if it is missing or empty. */
    static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public String getMessage() {
        return MESSAGE;
    }
}
