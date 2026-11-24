package mark.ledger;

import java.time.LocalDate;
import java.time.LocalTime;

public sealed interface Activity
    permits Buy, Sell, Deposit, Dividend, Interest, Tax {

    int sourceRow();

    LocalDate date();

    LocalTime time();
    }
