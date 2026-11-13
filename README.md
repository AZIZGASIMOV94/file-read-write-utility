# File Read/Write Utility

[![Build](https://github.com/AZIZGASIMOV94/file-read-write-utility/actions/workflows/build.yml/badge.svg)](https://github.com/AZIZGASIMOV94/file-read-write-utility/actions/workflows/build.yml)
![Java 17](https://img.shields.io/badge/Java-17-orange)
![License: MIT](https://img.shields.io/badge/License-MIT-blue)

A small Java library and command-line tool for everyday file operations: reading and writing text, working with bytes, copying files, saving Java objects, and downloading files over HTTP.

It started as a learning project for Java I/O and was later rewritten to use the modern NIO.2 API (`java.nio.file`), `java.net.http.HttpClient` and JUnit 5 tests.

## Features

| Area | Methods |
|---|---|
| Text (UTF-8) | `readText`, `readLines`, `writeText`, `appendText` |
| Bytes | `readBytes`, `writeBytes` |
| Copy | `copy(source, target, overwrite)` |
| Serialization | `writeObject`, `readObject(file, Type.class)` with a type check |
| Download | `download(url, target)` with timeout, redirects and HTTP status check |

Design choices:

- **`Path`-based API** with `java.nio.file.Files`, not the legacy `java.io.File`.
- **UTF-8 by default**, so Azerbaijani characters (ə, ş, ğ…) are stored correctly on every OS.
- **Parent directories are created automatically** when writing.
- **No resource leaks**: every stream is closed with try-with-resources.
- **Clear errors**: methods throw `IOException`; null arguments fail fast with `NullPointerException`.
- **Safe defaults**: `copy` won't overwrite unless asked; `download` fails on 4xx/5xx instead of saving an error page.

## Requirements

- Java 17+
- Maven 3.8+

## Build and test

```bash
mvn clean verify
```

This compiles the code, runs all tests and builds `target/file-util.jar`.

## Command-line usage

```bash
java -jar target/file-util.jar write notes.txt "Hello "
java -jar target/file-util.jar append notes.txt "Baku"
java -jar target/file-util.jar read notes.txt          # -> Hello Baku
java -jar target/file-util.jar copy notes.txt backup/notes.txt
java -jar target/file-util.jar copy notes.txt backup/notes.txt --overwrite
java -jar target/file-util.jar download https://example.com/ page.html
java -jar target/file-util.jar help
```

Exit codes: `0` success, `1` I/O error (e.g. file not found), `2` invalid usage.

## Library usage

```java
import io.github.azizgasimov94.fileutils.FileUtils;

Path file = Path.of("data/notes.txt");

FileUtils.writeText(file, "first line\n");
FileUtils.appendText(file, "second line\n");
List<String> lines = FileUtils.readLines(file);

record Student(String name, int grade) implements Serializable {}
FileUtils.writeObject(Path.of("student.ser"), new Student("Aziz", 95));
Student s = FileUtils.readObject(Path.of("student.ser"), Student.class);

long size = FileUtils.download(URI.create("https://example.com/"), Path.of("page.html"));
```

> ⚠️ Only use `readObject` on files your own program created. Java deserialization of untrusted data is a known security risk.

## Project structure

```
src
├── main/java/io/github/azizgasimov94/fileutils
│   ├── FileUtils.java              # the library
│   └── cli/FileUtilityCli.java     # command-line entry point
└── test/java/io/github/azizgasimov94/fileutils
    ├── FileUtilsTest.java          # text, bytes, copy, serialization
    ├── FileUtilsDownloadTest.java  # downloads against a local HTTP server
    └── cli/FileUtilityCliTest.java # commands, errors and exit codes
```

## Tests

25 JUnit 5 tests cover normal cases and error cases (missing files, overwrite protection, wrong object types, corrupt files, HTTP 404, bad CLI arguments). Tests use `@TempDir`, so they never touch real files, and the download tests run against an in-process HTTP server, so no internet connection is needed.

## What I learned

- The difference between `java.io` streams and the NIO.2 `Files` API
- Why character encoding matters, and how to handle it explicitly
- Resource management with try-with-resources
- Risks of Java serialization and how to validate deserialized types
- Writing testable CLI code by injecting `PrintStream`s instead of calling `System.out` directly
- Testing network code without the network

## License

[MIT](LICENSE)
