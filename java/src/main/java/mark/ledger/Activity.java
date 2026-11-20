package mark.ledger;

import java.time.LocalDate;
import java.time.LocalTime;

public sealed interface Activity
    permits Buy, Sell, Deposit, Dividend, Interest, Tax {

    int sourceRow();

    LocalDate date();

    LocalTime time();
    }

record Deposit(int sourceRow, LocalDate date, LocalTime time, String symbol, String currency, String quantity, String unitPrice, long cash) implements Activity{}

record Buy(int sourceRow, LocalDate date, LocalTime time, String symbol, String currency, String quantity, String unitPrice, long cash) implements Activity{}

record Sell(int sourceRow, LocalDate date, LocalTime time, String symbol, String currency, String quantity, String unitPrice, long cash) implements Activity{}

record Dividend(int sourceRow, LocalDate date, LocalTime time, String symbol, String currency, String quantity, String unitPrice, long cash) implements Activity{}

record Interest(int sourceRow, LocalDate date, LocalTime time, String symbol, String currency, String quantity, String unitPrice, long cash) implements Activity{}

record Tax(int sourceRow, LocalDate date, LocalTime time, String symbol, String currency, String quantity, String unitPrice, long cash) implements Activity{}