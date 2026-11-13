package io.github.azizgasimov94.fileutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Static helpers for common file operations: text, bytes, copying,
 * Java object serialization and downloading files over HTTP.
 *
 * <p>All methods use {@link Path} and the NIO.2 {@link Files} API,
 * default to UTF-8 for text, create missing parent directories when
 * writing, and close every resource they open.</p>
 */
public final class FileUtils {

    /** Timeout used by {@link #download(URI, Path)}. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private FileUtils() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    // ------------------------------------------------------------------ text

    /** Reads the whole file as a UTF-8 string. */
    public static String readText(Path file) throws IOException {
        return readText(file, StandardCharsets.UTF_8);
    }

    /** Reads the whole file as a string using the given charset. */
    public static String readText(Path file, Charset charset) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(charset, "charset must not be null");
        return Files.readString(file, charset);
    }

    /** Reads the file as a list of UTF-8 lines (line terminators removed). */
    public static List<String> readLines(Path file) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    /** Writes text to the file as UTF-8, replacing any existing content. */
    public static void writeText(Path file, String text) throws IOException {
        Objects.requireNonNull(text, "text must not be null");
        createParentDirectories(file);
        Files.writeString(file, text, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    /** Appends text to the end of the file as UTF-8, creating the file if needed. */
    public static void appendText(Path file, String text) throws IOException {
        Objects.requireNonNull(text, "text must not be null");
        createParentDirectories(file);
        Files.writeString(file, text, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE);
    }

    // ----------------------------------------------------------------- bytes

    /** Reads every byte of the file. */
    public static byte[] readBytes(Path file) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        return Files.readAllBytes(file);
    }

    /** Writes the bytes to the file, replacing any existing content. */
    public static void writeBytes(Path file, byte[] data) throws IOException {
        Objects.requireNonNull(data, "data must not be null");
        createParentDirectories(file);
        Files.write(file, data);
    }

    // ------------------------------------------------------------------ copy

    /**
     * Copies a file.
     *
     * @param overwrite if {@code false} and the target exists, a
     *                  {@link java.nio.file.FileAlreadyExistsException} is thrown
     */
    public static void copy(Path source, Path target, boolean overwrite) throws IOException {
        Objects.requireNonNull(source, "source must not be null");
        createParentDirectories(target);
        if (overwrite) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.copy(source, target);
        }
    }

    // ---------------------------------------------------------- serialization

    /** Serializes the object to the file using Java serialization. */
    public static void writeObject(Path file, Serializable object) throws IOException {
        Objects.requireNonNull(object, "object must not be null");
        createParentDirectories(file);
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(object);
        }
    }

    /**
     * Deserializes an object from the file and checks its type.
     *
     * <p><b>Security:</b> only use this on files your own application wrote.
     * Deserializing untrusted data can execute arbitrary code.</p>
     *
     * @throws IOException if the file can't be read, the class is unknown,
     *                     or the stored object is not of the expected type
     */
    public static <T> T readObject(Path file, Class<T> type) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(type, "type must not be null");
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Object value = in.readObject();
            if (!type.isInstance(value)) {
                throw new IOException("Expected " + type.getName() + " but file contains "
                        + (value == null ? "null" : value.getClass().getName()));
            }
            return type.cast(value);
        } catch (ClassNotFoundException e) {
            throw new IOException("Unknown class in serialized file: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------- download

    /** Downloads the resource at {@code url} to {@code target} using {@link #DEFAULT_TIMEOUT}. */
    public static long download(URI url, Path target) throws IOException {
        return download(url, target, DEFAULT_TIMEOUT);
    }

    /**
     * Downloads the resource at {@code url} to {@code target}, replacing the
     * target if it exists. Follows redirects.
     *
     * @return number of bytes written
     * @throws IOException on network errors or a non-2xx HTTP status
     */
    public static long download(URI url, Path target, Duration timeout) throws IOException {
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        createParentDirectories(target);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(url).timeout(timeout).GET().build();

        try {
            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                int status = response.statusCode();
                if (status < 200 || status >= 300) {
                    throw new IOException("Download failed: HTTP " + status + " for " + url);
                }
                return Files.copy(body, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted: " + url, e);
        }
    }

    // --------------------------------------------------------------- helpers

    private static void createParentDirectories(Path file) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
