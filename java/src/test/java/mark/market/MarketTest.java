package mark.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import mark.ledger.CsvReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MarketTest {

  @Test
  void aCloseOnTheEighthDayIsMissing() {
    Market.Book book = Market.read(fixture("refuse-missing-close"));
    Market.Missing missing =
        assertInstanceOf(
            Market.Missing.class,
            book.tradeClose("FUND", LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-01")));
    assertEquals("FUND", missing.symbol());
    assertEquals(LocalDate.parse("2026-06-01"), missing.date());
    CsvReader.Error error = missing.error();
    assertEquals("missing-close", error.code());
    assertEquals("FUND", error.fields().get("symbol"));
    assertEquals("2026-06-01", error.fields().get("date"));

    Market.Found stock =
        assertInstanceOf(
            Market.Found.class,
            book.valuation("Stock A", LocalDate.parse("2026-06-01")));
    assertEquals(LocalDate.parse("2026-06-01"), stock.closeDate());
    assertEquals("10", stock.close());
  }

  @Test
  void aCloseOnTheSeventhDayCounts(@TempDir Path folder) throws Exception {
    Files.writeString(
        folder.resolve("prices.csv"),
        """
        symbol,date,close
        FUND,2026-06-08,40
        """);
    Market.Book book = Market.read(folder);
    Market.Found found =
        assertInstanceOf(
            Market.Found.class,
            book.tradeClose("FUND", LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-20")));
    assertEquals(LocalDate.parse("2026-06-08"), found.closeDate());
    assertEquals("40", found.close());
  }

  @Test
  void theWalkthroughUsesTheCloseOnTheTradeDayAndTheAsOfClose() {
    Market.Book book = Market.read(fixture("walkthrough"));
    assertEquals("100", closeOn(book, "FUND", "2021-01-15", "2021-12-01"));
    assertEquals("125", closeOn(book, "FUND", "2021-03-01", "2021-12-01"));
    assertEquals("120", closeOn(book, "FUND", "2021-06-01", "2021-12-01"));
    assertEquals("150", valueOn(book, "FUND", "2021-12-01"));
    assertEquals("70", valueOn(book, "Stock A", "2021-12-01"));
    assertEquals("25", valueOn(book, "Stock B", "2021-12-01"));
    assertTrue(book.distributions().isEmpty());
    assertTrue(book.warnings().isEmpty());
  }

  @Test
  void aSundayDepositUsesMondayWhenThatCloseIsOnOrBeforeTheAsOfDate() {
    Market.Book book = Market.read(fixture("weekend-deposit"));
    Market.Found found =
        assertInstanceOf(
            Market.Found.class,
            book.tradeClose("FUND", LocalDate.parse("2026-06-14"), LocalDate.parse("2026-06-22")));
    assertEquals(LocalDate.parse("2026-06-15"), found.closeDate());
    assertEquals("100", found.close());
    assertEquals("110", valueOn(book, "FUND", "2026-06-22"));
    assertTrue(book.distributions().isEmpty());
  }

  @Test
  void aNextCloseAfterTheAsOfDateUsesTheValuationClose(@TempDir Path folder) throws Exception {
    Files.writeString(
        folder.resolve("prices.csv"),
        """
        symbol,date,close
        FUND,2026-06-08,30
        FUND,2026-06-14,40
        """);
    Market.Book book = Market.read(folder);
    Market.Found found =
        assertInstanceOf(
            Market.Found.class,
            book.tradeClose("FUND", LocalDate.parse("2026-06-10"), LocalDate.parse("2026-06-12")));
    assertEquals(LocalDate.parse("2026-06-08"), found.closeDate());
    assertEquals("30", found.close());

    Files.writeString(
        folder.resolve("prices.csv"),
        """
        symbol,date,close
        FUND,2026-06-14,40
        """);
    Market.Missing missing =
        assertInstanceOf(
            Market.Missing.class,
            Market.read(folder)
                .tradeClose("FUND", LocalDate.parse("2026-06-10"), LocalDate.parse("2026-06-12")));
    assertEquals(LocalDate.parse("2026-06-12"), missing.date());
  }

  @Test
  void valuationIgnoresACloseAfterTheAsOfDateAndACloseEightDaysEarlier(@TempDir Path folder)
      throws Exception {
    Files.writeString(
        folder.resolve("prices.csv"),
        """
        symbol,date,close
        FUND,2026-06-01,10
        FUND,2026-06-10,99
        """);
    Market.Book book = Market.read(folder);
    Market.Missing missing =
        assertInstanceOf(Market.Missing.class, book.valuation("FUND", LocalDate.parse("2026-06-09")));
    assertEquals("FUND", missing.symbol());
    assertEquals(LocalDate.parse("2026-06-09"), missing.date());

    Market.Found aWeekBefore =
        assertInstanceOf(Market.Found.class, book.valuation("FUND", LocalDate.parse("2026-06-08")));
    assertEquals(LocalDate.parse("2026-06-01"), aWeekBefore.closeDate());
    assertEquals("10", aWeekBefore.close());
  }

  @Test
  void distributionsKeepThePrintedAmountAndAreAbsentWhenTheFileIsAbsent() {
    Market.Book paid = Market.read(fixture("self-holding"));
    assertEquals(1, paid.distributions().size());
    Market.Distribution distribution = paid.distributions().get(0);
    assertEquals("FUND", distribution.symbol());
    assertEquals(LocalDate.parse("2026-06-15"), distribution.exDate());
    assertEquals("0.50", distribution.amountPerShare());

    Market.Book one = Market.read(fixture("dividend-and-distribution"));
    assertEquals("1", one.distributions().get(0).amountPerShare());
    assertEquals(LocalDate.parse("2026-06-16"), one.distributions().get(0).exDate());

    assertTrue(Market.read(fixture("buys-only")).distributions().isEmpty());
  }

  @Test
  void aCloseMoreThanTwentyPercentWithinSevenDaysWarns() {
    Market.Book book = Market.read(fixture("half-cent"));
    assertEquals(1, book.warnings().size());
    Market.Warning warning = book.warnings().get(0);
    assertEquals("price-jump", warning.code());
    assertEquals("FUND", warning.fields().get("symbol"));
    assertEquals("2026-11-13", warning.fields().get("date"));
    assertEquals("2026-11-10", warning.fields().get("previousDate"));
    assertEquals("0.125", book.closes().get(1).close());
  }

  @Test
  void twentyPercentExactlyIsNotAJumpAndBuysOnlyStaysQuiet(@TempDir Path folder) throws Exception {
    Files.writeString(
        folder.resolve("prices.csv"),
        """
        symbol,date,close
        FUND,2026-06-01,100
        FUND,2026-06-05,120
        """);
    assertTrue(Market.read(folder).warnings().isEmpty());
    assertTrue(Market.read(fixture("buys-only")).warnings().isEmpty());
  }

  private static String closeOn(Market.Book book, String symbol, String tradeDate, String asOf) {
    Market.Found found =
        assertInstanceOf(
            Market.Found.class,
            book.tradeClose(symbol, LocalDate.parse(tradeDate), LocalDate.parse(asOf)));
    assertEquals(LocalDate.parse(tradeDate), found.closeDate());
    return found.close();
  }

  private static String valueOn(Market.Book book, String symbol, String asOf) {
    Market.Found found =
        assertInstanceOf(Market.Found.class, book.valuation(symbol, LocalDate.parse(asOf)));
    assertEquals(LocalDate.parse(asOf), found.closeDate());
    return found.close();
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
