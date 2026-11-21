package com.mycompany.app;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for App.
 */
class AppTest
{
    @Test
    void testAppConstructor() {
        App app1 = new App();
        App app2 = new App();
        assertEquals(app1.getMessage(), app2.getMessage());
    }

    @Test
    void testAppMessage()
    {
        App app = new App();
        assertEquals("Hello World!", app.getMessage());
    }

    @Test
    void testEnvFallsBackWhenSettingIsMissing() {
        assertEquals("fallback", App.env("CHAPTER_SETTING_THAT_DOES_NOT_EXIST", "fallback"));
    }

    @Test
    void testEnvReadsSettingWhenPresent() {
        // PATH is always set, so the fallback must not be used
        assertNotEquals("fallback", App.env("PATH", "fallback"));
    }

    @Test
    void testHomeAndHealthEndpoints() throws Exception {
        HttpServer server = App.start(0, "test", "1.2.3");
        AutoCloseable stopServer = () -> server.stop(0);

        // try-with-resources stops the server and closes the client even if a check fails
        try (stopServer; HttpClient client = HttpClient.newHttpClient()) {
            String base = "http://localhost:" + server.getAddress().getPort();

            HttpResponse<String> home = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, home.statusCode());
            assertEquals("Hello World!", home.body());

            HttpResponse<String> health = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/health")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, health.statusCode());
            assertEquals("{\"status\":\"UP\",\"environment\":\"test\",\"version\":\"1.2.3\"}", health.body());
        }
    }
}
