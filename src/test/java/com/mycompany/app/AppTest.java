package com.mycompany.app;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for App.
 */
public class AppTest
{
    @Test
    public void testAppConstructor() {
        App app1 = new App();
        App app2 = new App();
        assertEquals(app1.getMessage(), app2.getMessage());
    }

    @Test
    public void testAppMessage()
    {
        App app = new App();
        assertEquals("Hello World!", app.getMessage());
    }

    @Test
    public void testHomeAndHealthEndpoints() throws Exception {
        HttpServer server = App.start(0, "test", "1.2.3");
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            HttpClient client = HttpClient.newHttpClient();

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
        } finally {
            server.stop(0);
        }
    }
}
