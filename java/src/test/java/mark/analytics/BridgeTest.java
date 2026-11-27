package mark.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import mark.ledger.Activity;
import mark.ledger.Buy;
import mark.ledger.Deposit;
import mark.ledger.Sell;
import mark.market.Market;
import org.junit.jupiter.api.Test;

/** The steps add up to your account, and the earliest refusal wins. */
final class BridgeTest {

  private static final LocalDate MONDAY = LocalDate.parse("2026-06-01");
  private static final LocalDate TUESDAY = LocalDate.parse("2026-06-02");
  private static final LocalDate AS_OF = LocalDate.parse("2026-06-05");

  @Test
  void allOnDayOnePlusTheStepsIsYourAccount() {
    List<Activity> activities =
        List.of(
            new Deposit(2, MONDAY, null, null, "CAD", "500", null, 50_000),
            new Buy(3, MONDAY, null, "Stock A", "CAD", "7", "30", -21_000),
            new Deposit(4, TUESDAY, null, null, "CAD", "300", null, 30_000),
            new Buy(5, TUESDAY, null, "Stock B", "CAD", "3", "70", -21_000),
            new Sell(6, AS_OF, null, "Stock A", "CAD", "2", "33", 6_600));
    Market.Book market =
        new Market.Book(
            List.of(
                new Market.Close("FUND", MONDAY, "17"),
                new Market.Close("FUND", TUESDAY, "19"),
                new Market.Close("FUND", AS_OF, "23"),
                new Market.Close("Stock A", AS_OF, "33"),
                new Market.Close("Stock B", AS_OF, "61")),
            List.of(),
            List.of());

    Bridge bridge =
        assertInstanceOf(Bridge.Built.class, Bridge.calculate(activities, market, "FUND", AS_OF))
            .bridge();

    assertEquals(
        bridge.account(),
        bridge.allOnDayOne()
            + bridge.whenMoneyArrived()
            + bridge.cashAndSales()
            + bridge.stockSelection());
    assertEquals(bridge.cashAndSales() + bridge.stockSelection(), bridge.headline());
  }

  @Test
  void aDepositsMissingCloseBeatsALaterBuysMissingClose() {
    List<Activity> activities =
        List.of(
            new Deposit(2, LocalDate.parse("2026-05-01"), null, null, "CAD", "100", null, 10_000),
            new Buy(3, AS_OF, null, "Stock A", "CAD", "1", "10", -1_000));
    Market.Book market =
        new Market.Book(List.of(new Market.Close("FUND", AS_OF, "10")), List.of(), List.of());

    Bridge.Refused refused =
        assertInstanceOf(
            Bridge.Refused.class, Bridge.calculate(activities, market, "FUND", AS_OF));

    assertEquals("missing-close", refused.error().code());
    assertEquals(Map.of("symbol", "FUND", "date", "2026-05-01"), refused.error().fields());
  }
}
