package mark.ledger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Map;

public sealed interface CsvReader permits CsvReader.Error, CsvReader.Success {

  record Error(String code, Map<String, Object> fields, String message) implements CsvReader {}

  record Success(List<Activity> activities) implements CsvReader {}

  /**
   * Proves deposit-quantity, dividend-quantity, cash-check, same-second, refuse-column,
   * refuse-currency, refuse-account, refuse-type, refuse-withdrawal, refuse-cash,
   * refuse-cash-below-zero, refuse-as-of.
   */    
  public static CsvReader read(Path activitiesCsv) {
    List<String> lines = readLines(activitiesCsv);
    if (lines.isEmpty()) {
      return missingColumn("effective_date");
    }
    Map<String, Integer> columns = header(split(lines.get(0)));
    for (String name : requiredColumns()) {
      if (!columns.containsKey(name)) {
        return missingColumn(name);
      }
    }
    List<ReadPiece> pieces = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) {
      if (lines.get(i).isBlank()) {
        continue;
      }
      ReadPiece piece = readRow(columns, split(lines.get(i)), i + 1);
      if (piece.error() != null) {
        return piece.error();
      }
      pieces.add(piece);
    }

    pieces.sort(
        Comparator.comparing((ReadPiece piece) -> piece.activity().date())
            .thenComparing(
                piece -> piece.activity().time(), Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparingInt(
                piece -> piece.activity().sourceRow()));
    CsvReader ledger = ledger(pieces);
    if (ledger != null) {
      return ledger;
    }
    List<Activity> activities = new ArrayList<>();
    for (ReadPiece piece : pieces) {
      activities.add(piece.activity());
    }
    return new Success(List.copyOf(activities));
  }

  private static ReadPiece readRow(Map<String, Integer> columns, List<String> cells, int sourceRow) {
    String type = cell(columns, cells, "activity_type");
    String subType = cell(columns, cells, "activity_sub_type");
    BigDecimal dollars = new BigDecimal(cell(columns, cells, "net_cash_amount"));
    long cash = cents(dollars);
    LocalDate date = LocalDate.parse(cell(columns, cells, "effective_date"));
    LocalTime time = time(cell(columns, cells, "effective_time"));
    String symbol = emptyAsNull(cell(columns, cells, "symbol"));
    String currency = cell(columns, cells, "currency");
    String accountId = cell(columns, cells, "account_id");
    String quantity = emptyAsNull(cell(columns, cells, "quantity"));
    String unitPrice = emptyAsNull(cell(columns, cells, "unit_price"));

    if (type.equals("Trade") && subType.equals("BUY")) {
      CsvReader gap =
          cashGap(dollars, quantity, unitPrice, cell(columns, cells, "commission"), true, sourceRow);
      if (gap != null) {
        return bad((Error) gap);
      }
      return ok(
          new Buy(sourceRow, date, time, symbol, currency, quantity, unitPrice, cash),
          accountId,
          currency,
          dollars,
          false);
    }
    if (type.equals("Trade") && subType.equals("SELL")) {
      CsvReader gap =
          cashGap(
              dollars, quantity, unitPrice, cell(columns, cells, "commission"), false, sourceRow);
      if (gap != null) {
        return bad((Error) gap);
      }
      return ok(
          new Sell(sourceRow, date, time, symbol, currency, quantity, unitPrice, cash),
          accountId,
          currency,
          dollars,
          false);
    }
    if (type.equals("MoneyMovement") && subType.equals("EFT")) {
      if (dollars.signum() < 0) {
        return bad(
            new Error(
                "withdrawal", fields("row", sourceRow), "Withdrawal at row " + sourceRow + "."));
      }
      if (dollars.signum() > 0) {
        return ok(
            new Deposit(sourceRow, date, time, symbol, currency, quantity, unitPrice, cash),
            accountId,
            currency,
            dollars,
            true);
      }
    }
    if (type.equals("Dividend") && subType.equals("-")) {
      return ok(
          new Dividend(sourceRow, date, time, symbol, currency, quantity, unitPrice, cash),
          accountId,
          currency,
          dollars,
          false);
    }
    if (type.equals("Interest") && subType.equals("-")) {
      return ok(
          new Interest(sourceRow, date, time, symbol, currency, quantity, unitPrice, cash),
          accountId,
          currency,
          dollars,
          false);
    }
    if (type.equals("Tax") && subType.equals("NRT")) {
      return ok(
          new Tax(sourceRow, date, time, symbol, currency, quantity, unitPrice, cash),
          accountId,
          currency,
          dollars,
          false);
    }
    return bad(
        new Error(
            "type",
            fields("type", type, "subType", subType, "row", sourceRow),
            "Unknown type " + type + " " + subType + " at row " + sourceRow + "."));
  }

  private static ReadPiece ok(
      Activity activity, String accountId, String currency, BigDecimal netCash, boolean deposit) {
    return new ReadPiece(null, activity, accountId, currency, netCash, deposit);
  }

  private static ReadPiece bad(Error error) {
    return new ReadPiece(error, null, null, null, null, false);
  }

  private static CsvReader ledger(List<ReadPiece> pieces) {
    boolean deposits = false;
    for (ReadPiece piece : pieces) {
      if (piece.deposit()) {
        deposits = true;
      }
    }
    String account = null;
    String currency = null;
    BigDecimal running = BigDecimal.ZERO;
    LocalDate day = null;
    for (ReadPiece piece : pieces) {
      LocalDate date = piece.activity().date();
      if (day != null && !date.equals(day)) {
        CsvReader below = belowZero(day, running, deposits);
        if (below != null) {
          return below;
        }
      }
      if (currency == null) {
        currency = piece.currency();
        account = piece.accountId();
      } else if (!piece.currency().equals(currency)) {
        return new Error(
            "currency",
            fields("currency", piece.currency(), "row", piece.activity().sourceRow()),
            "Second currency " + piece.currency() + " at row " + piece.activity().sourceRow() + ".");
      } else if (!piece.accountId().equals(account)) {
        return new Error(
            "account",
            fields("row", piece.activity().sourceRow()),
            "Second account at row " + piece.activity().sourceRow() + ".");
      }
      day = date;
      running = running.add(piece.netCash());
    }
    return belowZero(day, running, deposits);
  }

  private static CsvReader belowZero(LocalDate day, BigDecimal running, boolean deposits) {
    if (day == null || !deposits || running.signum() >= 0) {
      return null;
    }
    return new Error(
        "cash-below-zero",
        fields("date", day.toString(), "shortfall", number(running.abs())),
        "Cash is below zero on " + day + ".");
  }

  private static CsvReader cashGap(
      BigDecimal actual,
      String quantity,
      String unitPrice,
      String commissionText,
      boolean buy,
      int sourceRow) {
    BigDecimal shares = new BigDecimal(quantity);
    BigDecimal price = new BigDecimal(unitPrice);
    BigDecimal commission =
        commissionText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(commissionText);
    BigDecimal traded = shares.multiply(price);
    BigDecimal expected = buy ? traded.add(commission).negate() : traded.subtract(commission);
    BigDecimal gap = actual.subtract(expected).abs();
    BigDecimal tolerance =
        new BigDecimal("0.00005")
            .multiply(price)
            .add(new BigDecimal("0.00005").multiply(shares))
            .add(new BigDecimal("0.005"));
    if (gap.compareTo(tolerance) > 0) {
      return new Error(
          "cash",
          fields("row", sourceRow, "gap", number(gap)),
          "Cash at row " + sourceRow + " differs by " + gap + ".");
    }
    return null;
  }

  private static Object number(BigDecimal value) {
    BigDecimal stripped = value.stripTrailingZeros();
    if (stripped.scale() <= 0) {
      return stripped.longValueExact();
    }
    return stripped;
  }

  private static List<String> requiredColumns() {
    return List.of(
        "effective_date",
        "effective_time",
        "account_id",
        "activity_type",
        "activity_sub_type",
        "symbol",
        "currency",
        "quantity",
        "unit_price",
        "commission",
        "net_cash_amount");
  }

  private static Error missingColumn(String name) {
    return new Error("column", fields("column", name), "Missing column " + name + ".");
  }

  private static Map<String, Object> fields(String key, Object value) {
    return Map.of(key, value);
  }

  private static Map<String, Object> fields(
      String key1, Object value1, String key2, Object value2) {
    return Map.of(key1, value1, key2, value2);
  }

  private static Map<String, Object> fields(
      String key1, Object value1, String key2, Object value2, String key3, Object value3) {
    return Map.of(key1, value1, key2, value2, key3, value3);
  }

  private static Map<String, Integer> header(List<String> names) {
    Map<String, Integer> columns = new HashMap<>();
    for (int i = 0; i < names.size(); i++) {
      columns.putIfAbsent(names.get(i).trim(), i);
    }
    return columns;
  }

  private static String cell(Map<String, Integer> columns, List<String> cells, String name) {
    int index = columns.get(name);
    if (index >= cells.size()) {
      return "";
    }
    return cells.get(index).trim();
  }

  private static String emptyAsNull(String text) {
    return text.isEmpty() ? null : text;
  }

  private static LocalTime time(String text) {
    return text.isEmpty() ? null : LocalTime.parse(text);
  }

  private static long cents(BigDecimal dollars) {
    return dollars.movePointRight(2).longValueExact();
  }

  private static List<String> readLines(Path activitiesCsv) {
    try {
      return Files.readAllLines(activitiesCsv);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static List<String> split(String line) {
    List<String> cells = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else {
          quoted = !quoted;
        }
      } else if (c == ',' && !quoted) {
        cells.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    cells.add(current.toString());
    return cells;
  }
}

record ReadPiece(
    CsvReader.Error error,
    Activity activity,
    String accountId,
    String currency,
    BigDecimal netCash,
    boolean deposit) {}
