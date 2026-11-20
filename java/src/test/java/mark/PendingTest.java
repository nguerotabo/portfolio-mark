package mark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PendingTest {
  @TempDir Path dir;

  @Test
  void blankLinesAndCommentsAreIgnored() throws Exception {
    Path file = dir.resolve("pending.txt");
    Files.writeString(file, "\n# note\nwalkthrough activities\n\nwalkthrough paths.account\n");
    List<Pending.Entry> entries = Pending.load(file);
    assertEquals(2, entries.size());
    assertEquals("walkthrough", entries.get(0).fixture());
    assertEquals("activities", entries.get(0).field());
    assertEquals("paths.account", entries.get(1).field());
  }

  @Test
  void aMissingFileSkipsNothing() throws Exception {
    assertTrue(Pending.load(dir.resolve("pending.txt")).isEmpty());
  }

  @Test
  void aBadLineNamesItsNumber() throws Exception {
    Path file = dir.resolve("pending.txt");
    Files.writeString(file, "walkthrough activities\nnot a pair of tokens here\n");
    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> Pending.load(file));
    assertTrue(error.getMessage().contains(":2:"));
  }
}
