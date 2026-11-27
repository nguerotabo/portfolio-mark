package mark.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import mark.ledger.Activity;
import mark.ledger.Buy;
import mark.ledger.Sell;
import mark.market.Market;
import org.junit.jupiter.api.Test;

/** Every lot of a symbol sells the same fraction, and an oversell refuses. */
final class SaleFractionTest {

  private static final LocalDate BUY = LocalDate.parse("2026-06-01");
  private static final LocalDate SALE = LocalDate.parse("2026-06-02");
  private static final LocalDate AS_OF = LocalDate.parse("2026-06-03");

  @Test
  void everyLotSellsTheSameFraction() {
    List<Activity> activities =
        List.of(
            new Buy(2, BUY, null, "Stock A", "CAD", "10", "10", -10_000),
            new Buy(3, BUY, null, "Stock A", "CAD", "30", "10", -30_000),
            new Sell(4, SALE, null, "Stock A", "CAD", "20", "12", 24_000));

    Picks.Replay replay =
        assertInstanceOf(
            Picks.Replay.class, Picks.calculatePicks(activities, market(), "FUND", AS_OF));

    List<Picks.Lot> lots = replay.picks().get(0).lots();
    assertEquals(3 * lots.get(0).account(), lots.get(1).account());
    assertEquals(3 * lots.get(0).fund(), lots.get(1).fund());
  }

  @Test
  void aSaleLargerThanTheSharesHeldRefusesWithTheSymbolAndTheDate() {
    List<Activity> activities =
        List.of(
            new Buy(2, BUY, null, "Stock A", "CAD", "10", "10", -10_000),
            new Sell(3, SALE, null, "Stock A", "CAD", "11", "10", 11_000));

    Picks.Refused refused =
        assertInstanceOf(
            Picks.Refused.class, Picks.calculatePicks(activities, market(), "FUND", AS_OF));

    assertEquals("oversell", refused.error().code());
    assertEquals(Map.of("symbol", "Stock A", "date", "2026-06-02"), refused.error().fields());
    assertEquals(3, refused.sourceRow());
  }

  private static Market.Book market() {
    return new Market.Book(
        List.of(
            new Market.Close("FUND", BUY, "20"),
            new Market.Close("FUND", SALE, "25"),
            new Market.Close("FUND", AS_OF, "30"),
            new Market.Close("Stock A", AS_OF, "14")),
        List.of(),
        List.of());
  }
}
