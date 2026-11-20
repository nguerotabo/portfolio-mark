package mark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CaseReadTest {

  @Test
  void aRunFileSuppliesTheAsOfDateAndTheFund() {
    CaseRead.WithRun read = assertInstanceOf(CaseRead.WithRun.class, CaseRead.read(fixture("deposit-quantity")));
    assertEquals(LocalDate.parse("2026-06-22"), read.asOf());
    assertEquals("FUND", read.fund());
    assertEquals(1, read.activities().size());
    assertEquals(2, read.activities().get(0).sourceRow());
    assertTrue(read.warnings().isEmpty());
    assertEquals(2, read.market().closes().size());
    assertEquals("100", read.market().closes().get(0).close());
    assertTrue(read.market().distributions().isEmpty());
  }

  @Test
  void aFolderWithoutRunJsonOnlyReads(@TempDir Path folder) throws Exception {
    Files.writeString(
        folder.resolve("activities.csv"),
        """
        effective_date,effective_time,settlement_date,account_id,account_type,activity_type,activity_sub_type,description,direction,symbol,name,currency,quantity,unit_price,commission,net_cash_amount
        2026-06-01,,,DEMO,,Trade,BUY,,,Stock A,,CAD,10,10,0,-100
        """);
    CaseRead.ActivitiesOnly read =
        assertInstanceOf(CaseRead.ActivitiesOnly.class, CaseRead.read(folder));
    assertEquals(1, read.activities().size());
    assertTrue(read.warnings().isEmpty());
    assertTrue(read.market().closes().isEmpty());
  }

  @Test
  void anAsOfDateBeforeTheLastActivityRefuses() {
    CaseRead.Refused read = assertInstanceOf(CaseRead.Refused.class, CaseRead.read(fixture("refuse-as-of")));
    assertEquals("as-of", read.error().code());
    assertEquals("2026-06-01", read.error().fields().get("asOf"));
    assertEquals("2026-06-10", read.error().fields().get("lastActivity"));
  }

  @Test
  void aReaderRefusalStopsBeforeTheRunFile() {
    CaseRead.Refused read =
        assertInstanceOf(CaseRead.Refused.class, CaseRead.read(fixture("refuse-column")));
    assertEquals("column", read.error().code());
    assertEquals("symbol", read.error().fields().get("column"));
  }

  private static Path fixture(String name) {
    Path dir = Path.of("").toAbsolutePath();
    while (dir != null) {
      Path candidate = dir.resolve("spec").resolve("fixtures").resolve(name);
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
      dir = dir.getParent();
    }
    throw new IllegalStateException("missing fixture " + name);
  }
}
