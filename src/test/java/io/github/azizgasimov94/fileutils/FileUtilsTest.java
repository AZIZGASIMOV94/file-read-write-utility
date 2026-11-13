package io.github.azizgasimov94.fileutils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsTest {

    @TempDir
    Path dir;

    @Nested
    class Text {

        @Test
        void writeThenReadReturnsSameText() throws IOException {
            Path file = dir.resolve("hello.txt");
            FileUtils.writeText(file, "Salam, dünya!\nSecond line");

            assertEquals("Salam, dünya!\nSecond line", FileUtils.readText(file));
        }

        @Test
        void writeReplacesExistingContent() throws IOException {
            Path file = dir.resolve("file.txt");
            FileUtils.writeText(file, "a much longer original text");
            FileUtils.writeText(file, "short");

            assertEquals("short", FileUtils.readText(file));
        }

        @Test
        void appendAddsToEnd() throws IOException {
            Path file = dir.resolve("log.txt");
            FileUtils.appendText(file, "one\n");
            FileUtils.appendText(file, "two\n");

            assertEquals(List.of("one", "two"), FileUtils.readLines(file));
        }

        @Test
        void writeCreatesMissingParentDirectories() throws IOException {
            Path file = dir.resolve("a/b/c/nested.txt");
            FileUtils.writeText(file, "deep");

            assertTrue(Files.exists(file));
        }

        @Test
        void readMissingFileThrows() {
            assertThrows(NoSuchFileException.class,
                    () -> FileUtils.readText(dir.resolve("missing.txt")));
        }

        @Test
        void textIsStoredAsUtf8() throws IOException {
            Path file = dir.resolve("utf8.txt");
            FileUtils.writeText(file, "ə ş ğ ç ö ü");

            assertArrayEquals("ə ş ğ ç ö ü".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file));
        }

        @Test
        void nullArgumentsAreRejected() {
            assertThrows(NullPointerException.class, () -> FileUtils.writeText(null, "x"));
            assertThrows(NullPointerException.class, () -> FileUtils.writeText(dir.resolve("x"), null));
        }
    }

    @Nested
    class Bytes {

        @Test
        void writeThenReadReturnsSameBytes() throws IOException {
            Path file = dir.resolve("data.bin");
            byte[] data = {0, 1, 2, (byte) 255, 42};
            FileUtils.writeBytes(file, data);

            assertArrayEquals(data, FileUtils.readBytes(file));
        }

        @Test
        void emptyArrayCreatesEmptyFile() throws IOException {
            Path file = dir.resolve("empty.bin");
            FileUtils.writeBytes(file, new byte[0]);

            assertEquals(0, Files.size(file));
        }
    }

    @Nested
    class Copy {

        @Test
        void copiesContent() throws IOException {
            Path source = dir.resolve("source.txt");
            Path target = dir.resolve("out/target.txt");
            FileUtils.writeText(source, "copy me");

            FileUtils.copy(source, target, false);

            assertEquals("copy me", FileUtils.readText(target));
        }

        @Test
        void refusesToOverwriteByDefault() throws IOException {
            Path source = dir.resolve("source.txt");
            Path target = dir.resolve("target.txt");
            FileUtils.writeText(source, "new");
            FileUtils.writeText(target, "old");

            assertThrows(FileAlreadyExistsException.class, () -> FileUtils.copy(source, target, false));
            assertEquals("old", FileUtils.readText(target));
        }

        @Test
        void overwritesWhenAsked() throws IOException {
            Path source = dir.resolve("source.txt");
            Path target = dir.resolve("target.txt");
            FileUtils.writeText(source, "new");
            FileUtils.writeText(target, "old");

            FileUtils.copy(source, target, true);

            assertEquals("new", FileUtils.readText(target));
        }
    }

    @Nested
    class Serialization {

        record Student(String name, int grade) implements Serializable {
        }

        @Test
        void objectSurvivesRoundTrip() throws IOException {
            Path file = dir.resolve("student.ser");
            Student student = new Student("Aziz", 95);

            FileUtils.writeObject(file, student);

            assertEquals(student, FileUtils.readObject(file, Student.class));
        }

        @Test
        void wrongTypeIsReported() throws IOException {
            Path file = dir.resolve("string.ser");
            FileUtils.writeObject(file, "just a string");

            IOException e = assertThrows(IOException.class,
                    () -> FileUtils.readObject(file, Student.class));
            assertTrue(e.getMessage().contains("java.lang.String"));
        }

        @Test
        void corruptFileThrowsIOException() throws IOException {
            Path file = dir.resolve("corrupt.ser");
            FileUtils.writeText(file, "not a serialized object");

            assertThrows(IOException.class, () -> FileUtils.readObject(file, Object.class));
        }
    }
}
