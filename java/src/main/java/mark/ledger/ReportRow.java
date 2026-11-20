package mark.ledger;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** One activity in the report shape. Proves deposit-quantity, dividend-quantity, cash-check, and same-second. */
public record ReportRow(
    String date,
    String time,
    String type,
    String symbol,
    String currency,
    String quantity,
    String unitPrice,
    long cash,
    int sourceRow) {

  private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

  public static List<ReportRow> from(List<Activity> activities) {
    List<ReportRow> rows = new ArrayList<>();
    for (Activity activity : activities) {
      rows.add(from(activity));
    }
    return List.copyOf(rows);
  }

  private static ReportRow from(Activity activity) {
    return switch (activity) {
      case Buy buy ->
          row(
              "buy",
              buy.sourceRow(),
              buy.date(),
              buy.time(),
              buy.symbol(),
              buy.currency(),
              buy.quantity(),
              buy.unitPrice(),
              buy.cash());
      case Sell sell ->
          row(
              "sale",
              sell.sourceRow(),
              sell.date(),
              sell.time(),
              sell.symbol(),
              sell.currency(),
              sell.quantity(),
              sell.unitPrice(),
              sell.cash());
      case Deposit deposit ->
          row(
              "deposit",
              deposit.sourceRow(),
              deposit.date(),
              deposit.time(),
              deposit.symbol(),
              deposit.currency(),
              deposit.quantity(),
              deposit.unitPrice(),
              deposit.cash());
      case Dividend dividend ->
          row(
              "dividend",
              dividend.sourceRow(),
              dividend.date(),
              dividend.time(),
              dividend.symbol(),
              dividend.currency(),
              dividend.quantity(),
              dividend.unitPrice(),
              dividend.cash());
      case Interest interest ->
          row(
              "interest",
              interest.sourceRow(),
              interest.date(),
              interest.time(),
              interest.symbol(),
              interest.currency(),
              interest.quantity(),
              interest.unitPrice(),
              interest.cash());
      case Tax tax ->
          row(
              "tax",
              tax.sourceRow(),
              tax.date(),
              tax.time(),
              tax.symbol(),
              tax.currency(),
              tax.quantity(),
              tax.unitPrice(),
              tax.cash());
    };
  }

  private static ReportRow row(
      String type,
      int sourceRow,
      LocalDate date,
      LocalTime time,
      String symbol,
      String currency,
      String quantity,
      String unitPrice,
      long cash) {
    return new ReportRow(
        date.toString(),
        time == null ? null : CLOCK.format(time),
        type,
        symbol,
        currency,
        quantity,
        unitPrice,
        cash,
        sourceRow);
  }
}
