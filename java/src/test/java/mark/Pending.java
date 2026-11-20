package mark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * One line per skipped pair: {@code <fixture> <field.path>}. {@code deposit-quantity activities}
 * skips that field and everything under it. Blank lines and {@code #} comments are ignored.
 */
final class Pending {
  record Entry(int line, String fixture, String field) {}

  private Pending() {}

  static List<Entry> load(Path file) throws IOException {
    if (!Files.exists(file)) {
      return List.of();
    }
    List<Entry> entries = new ArrayList<>();
    int number = 0;
    for (String raw : Files.readAllLines(file)) {
      number++;
      String line = raw.trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      String[] parts = line.split("\\s+");
      if (parts.length != 2 || !isFieldPath(parts[1])) {
        throw new IllegalArgumentException(
            file + ":" + number + ": expected '<fixture> <field.path>'");
      }
      entries.add(new Entry(number, parts[0], parts[1]));
    }
    return List.copyOf(entries);
  }

  private static boolean isFieldPath(String field) {
    return field.matches("[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*");
  }
}
