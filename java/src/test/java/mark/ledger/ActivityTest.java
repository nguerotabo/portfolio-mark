package mark.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class ActivityTest{

    @Test
    void build_acitvity_records() throws Exception {
            Deposit deposit = new Deposit(2, LocalDate.parse("2026-01-01"), null, null, "CAD", "1000.0000", null, 100000);
            Buy buy = new Buy(3, LocalDate.parse("2026-01-03"), null, "Stock A", "CAD", "10", "100", -100000);
            Sell sell = new Sell(4, LocalDate.parse("2026-01-04"), null, "Stock A", "CAD", "5", "110", 55000);

            assertEquals(2, deposit.sourceRow());
            assertEquals(LocalDate.parse("2026-01-01"), deposit.date());
            assertEquals(null, deposit.time());
            assertEquals(null, deposit.symbol());
            assertEquals("CAD", deposit.currency());
            assertEquals("1000.0000", deposit.quantity());
            assertEquals(null, deposit.unitPrice());
            assertEquals(100000, deposit.cash());

            assertEquals(3, buy.sourceRow());
            assertEquals(LocalDate.parse("2026-01-03"), buy.date());
            assertEquals(null, buy.time());
            assertEquals("Stock A", buy.symbol());
            assertEquals("CAD", buy.currency());
            assertEquals("10", buy.quantity());
            assertEquals("100", buy.unitPrice());
            assertEquals(-100000, buy.cash());

            assertEquals(4, sell.sourceRow());
            assertEquals(LocalDate.parse("2026-01-04"), sell.date());
            assertEquals(null, sell.time());
            assertEquals("Stock A", sell.symbol());
            assertEquals("CAD", sell.currency());
            assertEquals("5", sell.quantity());
            assertEquals("110", sell.unitPrice());
            assertEquals(55000, sell.cash());
    }
}
