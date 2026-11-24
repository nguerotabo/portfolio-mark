package mark.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import mark.ledger.Activity;
import mark.ledger.Buy;
import mark.market.Market;
import org.junit.jupiter.api.Test;

/** Stock selection is the account minus the benchmark, and the pick impacts sum to it. */
final class PicksTest {

  @Test
  void pickImpactsSumToStockSelection() {
    Picks.Replay replay = replay();
    long stockSelection = replay.account() - replay.benchmark();

    assertEquals(stockSelection, replay.stockSelection());
    assertTrue(replay.picks().size() >= 2, "needs two symbols, so dropping one changes the sum");
    assertEquals(stockSelection, sum(replay.picks()));
    assertNotEquals(stockSelection, sum(replay.picks().subList(1, replay.picks().size())));
  }

  private static long sum(List<Picks.Pick> picks) {
    long total = 0;
    for (Picks.Pick pick : picks) {
      total += pick.impact();
    }
    return total;
  }

  private static Picks.Replay replay() {
    LocalDate asOf = LocalDate.parse("2026-11-13");
    List<Activity> activities =
        List.of(
            new Buy(2, LocalDate.parse("2026-11-10"), null, "Stock A", "CAD", "10", "100", -100_000),
            new Buy(3, LocalDate.parse("2026-11-11"), null, "Stock B", "CAD", "5", "40", -20_000));
    Market.Book market =
        new Market.Book(
            List.of(
                new Market.Close("FUND", LocalDate.parse("2026-11-10"), "50"),
                new Market.Close("FUND", LocalDate.parse("2026-11-11"), "40"),
                new Market.Close("FUND", asOf, "60"),
                new Market.Close("Stock A", asOf, "50"),
                new Market.Close("Stock B", asOf, "30")),
            List.of(),
            List.of());
    return (Picks.Replay) Picks.calculatePicks(activities, market, "FUND", asOf);
  }
}
