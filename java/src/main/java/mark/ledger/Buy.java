package mark.ledger;

import java.time.LocalDate;
import java.time.LocalTime;

public record Buy(int sourceRow, LocalDate date, LocalTime time, String symbol, String currency, String quantity, String unitPrice, long cash) implements Activity{}
