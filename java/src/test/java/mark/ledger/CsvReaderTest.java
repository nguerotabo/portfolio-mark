package mark.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CsvReaderTest {

  @Test
  void refuseColumnNamesTheMissingColumn() {
    CsvReader.Error error = error("refuse-column");
    assertEquals("column", error.code());
    assertEquals("symbol", error.fields().get("column"));
  }

  @Test
  void refuseTypeNamesTheTypeSubTypeAndRow() {
    CsvReader.Error error = error("refuse-type");
    assertEquals("type", error.code());
    assertEquals("Trade", error.fields().get("type"));
    assertEquals("SPLIT", error.fields().get("subType"));
    assertEquals(2, error.fields().get("row"));
  }

  @Test
  void refuseCurrencyNamesTheCurrencyAndTheRow() {
    CsvReader.Error error = error("refuse-currency");
    assertEquals("currency", error.code());
    assertEquals("USD", error.fields().get("currency"));
    assertEquals(3, error.fields().get("row"));
  }

  @Test
  void refuseAccountNamesTheRow() {
    CsvReader.Error error = error("refuse-account");
    assertEquals("account", error.code());
    assertEquals(3, error.fields().get("row"));
  }

  @Test
  void refuseCashBelowZeroNamesTheDateAndTheShortfall() {
    CsvReader.Error error = error("refuse-cash-below-zero");
    assertEquals("cash-below-zero", error.code());
    assertEquals("2026-06-02", error.fields().get("date"));
    assertEquals(100L, error.fields().get("shortfall"));
  }

  @Test
  void buysOnlyHasNoDepositSoNegativeCashIsAllowed() {
    assertEquals(3, success("buys-only").size());
  }

  @Test
  void refuseCashNamesTheRowAndTheGap() {
    CsvReader.Error error = error("refuse-cash");
    assertEquals("cash", error.code());
    assertEquals(2, error.fields().get("row"));
    assertEquals(50L, error.fields().get("gap"));
  }

  @Test
  void refuseWithdrawalNamesTheRow() {
    CsvReader.Error error = error("refuse-withdrawal");
    assertEquals("withdrawal", error.code());
    assertEquals(2, error.fields().get("row"));
  }

  @Test
  void depositQuantityKeepsTheQuantityText() {
    List<Activity> activities = success("deposit-quantity");
    assertEquals(1, activities.size());
    Deposit deposit = assertInstanceOf(Deposit.class, activities.get(0));
    assertEquals(2, deposit.sourceRow());
    assertEquals(LocalDate.parse("2026-06-15"), deposit.date());
    assertNull(deposit.time());
    assertNull(deposit.symbol());
    assertEquals("CAD", deposit.currency());
    assertEquals("1000.0000", deposit.quantity());
    assertNull(deposit.unitPrice());
    assertEquals(100000L, deposit.cash());
  }

  @Test
  void dividendQuantityMapsABuyAndADividend() {
    List<Activity> activities = success("dividend-quantity");
    Buy buy = assertInstanceOf(Buy.class, activities.get(0));
    Dividend dividend = assertInstanceOf(Dividend.class, activities.get(1));
    assertEquals("10", buy.quantity());
    assertEquals(-10000L, buy.cash());
    assertEquals("5.0000", dividend.quantity());
    assertEquals("Stock A", dividend.symbol());
    assertEquals(500L, dividend.cash());
    assertEquals(3, dividend.sourceRow());
  }

  @Test
  void cashCheckParsesThePrintedPriceAndTheCents() {
    Buy buy = assertInstanceOf(Buy.class, success("cash-check").get(0));
    assertEquals("2.6897", buy.quantity());
    assertEquals("185.8904", buy.unitPrice());
    assertEquals(-49999L, buy.cash());
  }

  @Test
  void sameSecondKeepsFileOrderAndTheClockTime() {
    List<Activity> activities = success("same-second");
    Buy first = assertInstanceOf(Buy.class, activities.get(0));
    Buy second = assertInstanceOf(Buy.class, activities.get(1));
    assertEquals("Stock A", first.symbol());
    assertEquals("Stock B", second.symbol());
    assertEquals(LocalTime.parse("09:30:00"), first.time());
    assertEquals(2, first.sourceRow());
    assertEquals(3, second.sourceRow());
  }

  @Test
  void interestAndTaxAndASaleMapToThoseRecords() {
    List<Activity> interest = success("interest-and-tax");
    assertInstanceOf(Interest.class, interest.get(1));
    Tax tax = assertInstanceOf(Tax.class, interest.get(2));
    assertNull(tax.symbol());
    assertEquals(-100L, tax.cash());

    List<Activity> walkthrough = success("walkthrough");
    Sell sale = assertInstanceOf(Sell.class, walkthrough.get(2));
    assertEquals("50", sale.quantity());
    assertEquals(300000L, sale.cash());
  }

  private static CsvReader.Error error(String fixture) {
    return assertInstanceOf(CsvReader.Error.class, CsvReader.read(activities(fixture)));
  }

  private static List<Activity> success(String fixture) {
    CsvReader.Success success =
        assertInstanceOf(CsvReader.Success.class, CsvReader.read(activities(fixture)));
    return success.activities();
  }

  private static Path activities(String fixture) {
    Path dir = Path.of("").toAbsolutePath();
    while (dir != null) {
      Path candidate =
          dir.resolve("spec").resolve("fixtures").resolve(fixture).resolve("activities.csv");
      if (Files.exists(candidate)) {
        return candidate;
      }
      dir = dir.getParent();
    }
    throw new IllegalStateException("missing fixture " + fixture);
  }
}
