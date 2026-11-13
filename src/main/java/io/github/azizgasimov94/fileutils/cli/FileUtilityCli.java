package io.github.azizgasimov94.fileutils.cli;

import io.github.azizgasimov94.fileutils.FileUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Command-line entry point.
 *
 * <pre>
 * java -jar file-util.jar read     &lt;file&gt;
 * java -jar file-util.jar write    &lt;file&gt; &lt;text&gt;
 * java -jar file-util.jar append   &lt;file&gt; &lt;text&gt;
 * java -jar file-util.jar copy     &lt;source&gt; &lt;target&gt; [--overwrite]
 * java -jar file-util.jar download &lt;url&gt; &lt;target&gt;
 * </pre>
 */
public final class FileUtilityCli {

    static final int OK = 0;
    static final int USAGE_ERROR = 2;
    static final int IO_ERROR = 1;

    static final String USAGE = String.join(System.lineSeparator(),
            "Usage: java -jar file-util.jar <command> [arguments]",
            "",
            "Commands:",
            "  read     <file>                             Print a text file",
            "  write    <file> <text>                      Write text (replaces content)",
            "  append   <file> <text>                      Append text to the end",
            "  copy     <source> <target> [--overwrite]    Copy a file",
            "  download <url> <target>                     Download a file over HTTP(S)",
            "  help                                        Show this message");

    private final PrintStream out;
    private final PrintStream err;

    FileUtilityCli(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public static void main(String[] args) {
        int exitCode = new FileUtilityCli(System.out, System.err).run(args);
        System.exit(exitCode);
    }

    /** Runs a command and returns the process exit code. */
    int run(String[] args) {
        if (args.length == 0 || args[0].equals("help") || args[0].equals("--help")) {
            out.println(USAGE);
            return args.length == 0 ? USAGE_ERROR : OK;
        }

        try {
            return switch (args[0]) {
                case "read" -> read(args);
                case "write" -> write(args, false);
                case "append" -> write(args, true);
                case "copy" -> copy(args);
                case "download" -> download(args);
                default -> usageError("Unknown command: " + args[0]);
            };
        } catch (NoSuchFileException e) {
            err.println("Error: file not found: " + e.getFile());
            return IO_ERROR;
        } catch (FileAlreadyExistsException e) {
            err.println("Error: target already exists (use --overwrite): " + e.getFile());
            return IO_ERROR;
        } catch (IOException e) {
            err.println("Error: " + e.getMessage());
            return IO_ERROR;
        }
    }

    private int read(String[] args) throws IOException {
        if (args.length != 2) {
            return usageError("read expects exactly 1 argument");
        }
        out.print(FileUtils.readText(Path.of(args[1])));
        return OK;
    }

    private int write(String[] args, boolean append) throws IOException {
        if (args.length != 3) {
            return usageError(args[0] + " expects exactly 2 arguments");
        }
        Path file = Path.of(args[1]);
        if (append) {
            FileUtils.appendText(file, args[2]);
        } else {
            FileUtils.writeText(file, args[2]);
        }
        out.println((append ? "Appended to " : "Wrote ") + file);
        return OK;
    }

    private int copy(String[] args) throws IOException {
        boolean overwrite = args.length == 4 && args[3].equals("--overwrite");
        if (args.length != 3 && !overwrite) {
            return usageError("copy expects <source> <target> [--overwrite]");
        }
        FileUtils.copy(Path.of(args[1]), Path.of(args[2]), overwrite);
        out.println("Copied " + args[1] + " -> " + args[2]);
        return OK;
    }

    private int download(String[] args) throws IOException {
        if (args.length != 3) {
            return usageError("download expects exactly 2 arguments");
        }
        URI url;
        try {
            url = new URI(args[1]);
        } catch (URISyntaxException e) {
            return usageError("Invalid URL: " + args[1]);
        }
        if (url.getScheme() == null
                || !(url.getScheme().equals("http") || url.getScheme().equals("https"))) {
            return usageError("URL must start with http:// or https://");
        }
        long bytes = FileUtils.download(url, Path.of(args[2]));
        out.println("Downloaded " + bytes + " bytes to " + args[2]);
        return OK;
    }

    private int usageError(String message) {
        err.println("Error: " + message);
        err.println();
        err.println(USAGE);
        return USAGE_ERROR;
    }
}
