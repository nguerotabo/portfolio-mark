package mark.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import mark.ledger.Activity;
import mark.ledger.Buy;
import mark.ledger.Sell;
import mark.market.Market;
import org.junit.jupiter.api.Test;

/**
 * A fund lot is paid a distribution only for the shares it held at the end of the day before the
 * ex-date. Each test compares the same case with and without the distribution.
 */
final class DistributionsTest {

  private static final LocalDate DAY_ONE = LocalDate.parse("2026-06-01");
  private static final LocalDate EX_DATE = LocalDate.parse("2026-06-02");
  private static final LocalDate AS_OF = LocalDate.parse("2026-06-03");

  @Test
  void aLotBoughtTheDayBeforeTheExDateIsPaidOnEveryFundPath() {
    List<Activity> activities = List.of(buy(2, DAY_ONE));

    Bridge paid = bridge(activities, paying(EX_DATE));
    Bridge unpaid = bridge(activities, List.of());

    assertTrue(paid.benchmark() > unpaid.benchmark());
    assertTrue(paid.fundOnYourDates() > unpaid.fundOnYourDates());
    assertTrue(paid.allOnDayOne() > unpaid.allOnDayOne());
    assertEquals(unpaid.account(), paid.account());
  }

  @Test
  void aLotBoughtOnTheExDateIsNotPaid() {
    List<Activity> activities = List.of(buy(2, EX_DATE));

    assertEquals(bridge(activities, List.of()), bridge(activities, paying(EX_DATE)));
  }

  @Test
  void anExDateAfterTheAsOfDateIsNotPaid() {
    List<Activity> activities = List.of(buy(2, DAY_ONE));

    assertEquals(bridge(activities, List.of()), bridge(activities, paying(AS_OF.plusDays(1))));
  }

  @Test
  void aBenchmarkLotSoldBeforeTheExDateIsNotPaid() {
    List<Activity> activities =
        List.of(
            buy(2, DAY_ONE), new Sell(3, DAY_ONE, null, "Stock A", "CAD", "10", "10", 10_000));

    assertEquals(
        bridge(activities, List.of()).benchmark(), bridge(activities, paying(EX_DATE)).benchmark());
  }

  @Test
  void aDistributionForAnotherFundIsNotPaid() {
    List<Activity> activities = List.of(buy(2, DAY_ONE));
    List<Market.Distribution> other =
        List.of(new Market.Distribution("OTHER", EX_DATE, "1"));

    assertEquals(bridge(activities, List.of()), bridge(activities, other));
  }

  private static Buy buy(int row, LocalDate date) {
    return new Buy(row, date, null, "Stock A", "CAD", "10", "10", -10_000);
  }

  private static List<Market.Distribution> paying(LocalDate exDate) {
    return List.of(new Market.Distribution("FUND", exDate, "1"));
  }

  private static Bridge bridge(List<Activity> activities, List<Market.Distribution> paid) {
    Market.Book market =
        new Market.Book(
            List.of(
                new Market.Close("FUND", DAY_ONE, "10"),
                new Market.Close("FUND", EX_DATE, "10"),
                new Market.Close("FUND", AS_OF, "10"),
                new Market.Close("Stock A", AS_OF, "10")),
            paid,
            List.of());
    Bridge.Built built =
        assertInstanceOf(
            Bridge.Built.class, Bridge.calculate(activities, market, "FUND", AS_OF));
    return built.bridge();
  }
}
