package io.github.azizgasimov94.fileutils.cli;

import io.github.azizgasimov94.fileutils.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilityCliTest {

    @TempDir
    Path dir;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final FileUtilityCli cli = new FileUtilityCli(
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));

    private String stdout() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String stderr() {
        return err.toString(StandardCharsets.UTF_8);
    }

    @Test
    void writeAppendAndRead() {
        String file = dir.resolve("notes.txt").toString();

        assertEquals(FileUtilityCli.OK, cli.run(new String[]{"write", file, "first "}));
        assertEquals(FileUtilityCli.OK, cli.run(new String[]{"append", file, "second"}));
        out.reset();
        assertEquals(FileUtilityCli.OK, cli.run(new String[]{"read", file}));

        assertEquals("first second", stdout());
    }

    @Test
    void copyWithAndWithoutOverwrite() throws IOException {
        Path source = dir.resolve("a.txt");
        Path target = dir.resolve("b.txt");
        FileUtils.writeText(source, "new");
        FileUtils.writeText(target, "old");

        int refused = cli.run(new String[]{"copy", source.toString(), target.toString()});
        assertEquals(FileUtilityCli.IO_ERROR, refused);
        assertTrue(stderr().contains("--overwrite"));

        int copied = cli.run(new String[]{"copy", source.toString(), target.toString(), "--overwrite"});
        assertEquals(FileUtilityCli.OK, copied);
        assertEquals("new", FileUtils.readText(target));
    }

    @Test
    void readMissingFileReportsError() {
        int code = cli.run(new String[]{"read", dir.resolve("nope.txt").toString()});

        assertEquals(FileUtilityCli.IO_ERROR, code);
        assertTrue(stderr().contains("file not found"));
    }

    @Test
    void unknownCommandPrintsUsage() {
        int code = cli.run(new String[]{"delete-everything"});

        assertEquals(FileUtilityCli.USAGE_ERROR, code);
        assertTrue(stderr().contains("Unknown command"));
        assertTrue(stderr().contains("Usage:"));
    }

    @Test
    void wrongArgumentCountPrintsUsage() {
        assertEquals(FileUtilityCli.USAGE_ERROR, cli.run(new String[]{"write", "only-one-arg"}));
    }

    @Test
    void downloadRejectsNonHttpUrl() {
        int code = cli.run(new String[]{"download", "file:///etc/passwd", dir.resolve("x").toString()});

        assertEquals(FileUtilityCli.USAGE_ERROR, code);
        assertTrue(stderr().contains("http://"));
    }

    @Test
    void noArgumentsShowsUsage() {
        assertEquals(FileUtilityCli.USAGE_ERROR, cli.run(new String[0]));
        assertTrue(stdout().contains("Usage:"));
    }

    @Test
    void helpShowsUsage() {
        assertEquals(FileUtilityCli.OK, cli.run(new String[]{"help"}));
        assertTrue(stdout().contains("Commands:"));
    }
}
