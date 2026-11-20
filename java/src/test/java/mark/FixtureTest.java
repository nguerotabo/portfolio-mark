package mark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/** Runs every fixture folder. Skips only the pairs listed in {@code spec/fixtures/pending.txt}. */
final class FixtureTest {
  private static final ObjectMapper JSON = new ObjectMapper();

  @TestFactory
  List<DynamicTest> everyFixture() throws IOException {
    Path root = fixturesRoot();
    List<Path> fixtures = fixtureFolders(root);
    List<Pending.Entry> pending = Pending.load(root.resolve("pending.txt"));
    Set<String> names = new HashSet<>();
    for (Path fixture : fixtures) {
      names.add(fixture.getFileName().toString());
    }
    for (Pending.Entry entry : pending) {
      if (!names.contains(entry.fixture())) {
        throw new IllegalArgumentException(
            "pending.txt:" + entry.line() + ": unknown fixture " + entry.fixture());
      }
    }
    List<DynamicTest> tests = new ArrayList<>();
    for (Path fixture : fixtures) {
      String name = fixture.getFileName().toString();
      tests.add(DynamicTest.dynamicTest(name, () -> runOne(fixture, pendingFor(name, pending))));
    }
    return tests;
  }

  private static void runOne(Path folder, List<Pending.Entry> pending) throws IOException {
    String name = folder.getFileName().toString();
    JsonNode expected = JSON.readTree(folder.resolve("expected.json").toFile());
    Set<String> fields = new HashSet<>();
    for (Pending.Entry entry : pending) {
      if (!pathExists(expected, entry.field())) {
        Assertions.fail(
            "pending.txt:" + entry.line() + ": " + name + " has no field " + entry.field());
      }
      System.out.println("skip " + name + " " + entry.field());
      fields.add(entry.field());
    }
    SkipSet skipped = SkipSet.of(fields);
    if (ReportCompare.fullySkipped(expected, skipped)) {
      return;
    }
    String report;
    try {
      report = Cases.reportJson(folder);
    } catch (UnsupportedOperationException ex) {
      if (ex.getMessage() == null || !ex.getMessage().startsWith("reader is not built")) {
        throw ex;
      }
      Assertions.fail(
          name
              + " still checks "
              + ReportCompare.stillChecked(expected, skipped)
              + ". "
              + ex.getMessage());
      return;
    }
    ReportCompare.compare(name, expected, JSON.readTree(report), skipped);
  }

  private static List<Pending.Entry> pendingFor(String fixture, List<Pending.Entry> pending) {
    List<Pending.Entry> mine = new ArrayList<>();
    for (Pending.Entry entry : pending) {
      if (entry.fixture().equals(fixture)) {
        mine.add(entry);
      }
    }
    return mine;
  }

  static boolean pathExists(JsonNode node, String fieldPath) {
    JsonNode current = node;
    for (String segment : fieldPath.split("\\.", -1)) {
      if (current == null || current.isNull() || current.isMissingNode()) {
        return false;
      }
      if (current.isArray()) {
        int index;
        try {
          index = Integer.parseInt(segment);
        } catch (NumberFormatException ex) {
          return false;
        }
        if (index < 0 || index >= current.size()) {
          return false;
        }
        current = current.get(index);
      } else if (current.isObject()) {
        if (!current.has(segment)) {
          return false;
        }
        current = current.get(segment);
      } else {
        return false;
      }
    }
    return current != null;
  }

  private static List<Path> fixtureFolders(Path root) throws IOException {
    try (Stream<Path> children = Files.list(root)) {
      return children
          .filter(Files::isDirectory)
          .sorted(Comparator.comparing(path -> path.getFileName().toString()))
          .toList();
    }
  }

  private static Path fixturesRoot() {
    Path dir = Path.of("").toAbsolutePath();
    while (dir != null) {
      Path candidate = dir.resolve("spec").resolve("fixtures");
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
      dir = dir.getParent();
    }
    throw new UncheckedIOException(
        new IOException("spec/fixtures not found from " + Path.of("").toAbsolutePath()));
  }
}
