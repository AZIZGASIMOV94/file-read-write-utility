package io.github.azizgasimov94.fileutils;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests downloads against a local in-process HTTP server, so no internet is needed. */
class FileUtilsDownloadTest {

    private static final byte[] BODY = "downloaded content".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path dir;

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/file.txt", exchange -> {
            exchange.sendResponseHeaders(200, BODY.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(BODY);
            }
        });
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void downloadsFileAndReturnsSize() throws IOException {
        Path target = dir.resolve("downloads/file.txt");

        long bytes = FileUtils.download(URI.create(baseUrl + "/file.txt"), target);

        assertEquals(BODY.length, bytes);
        assertEquals("downloaded content", FileUtils.readText(target));
    }

    @Test
    void httpErrorThrowsAndWritesNothing() {
        Path target = dir.resolve("missing.txt");

        IOException e = assertThrows(IOException.class,
                () -> FileUtils.download(URI.create(baseUrl + "/missing"), target));

        assertTrue(e.getMessage().contains("404"));
        assertFalse(Files.exists(target));
    }
}
