package mark.market;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mark.ledger.CsvReader;

/**
 * Closes and distributions for one case folder. A fund trade uses that day's close or the next
 * within 7 days. Valuation uses the last close on the as-of date or in the 7 days before it.
 * Anything further is a missing close.
 *
 * <p>Proves refuse-missing-close, weekend-deposit, walkthrough, self-holding,
 * dividend-and-distribution, and half-cent.
 */
public final class Market {

  private static final BigDecimal TWENTY_PERCENT = new BigDecimal("0.20");

  public record Close(String symbol, LocalDate date, String close) {}

  public record Distribution(String symbol, LocalDate exDate, String amountPerShare) {}

  public record Warning(String code, Map<String, Object> fields, String message) {}

  public sealed interface Lookup permits Found, Missing {}

  public record Found(String symbol, LocalDate needed, LocalDate closeDate, String close)
      implements Lookup {}

  public record Missing(String symbol, LocalDate date) implements Lookup {
    public CsvReader.Error error() {
      return new CsvReader.Error(
          "missing-close",
          fields("symbol", symbol, "date", date.toString()),
          "No close for " + symbol + " within 7 days of " + date + ".");
    }
  }

  public record Book(List<Close> closes, List<Distribution> distributions, List<Warning> warnings) {
    /**
     * The close on {@code tradeDate}, or the earliest later close at most 7 days after it. A close
     * on the 7th day counts. When that close is after {@code asOf}, the valuation close is used.
     */
    public Lookup tradeClose(String symbol, LocalDate tradeDate, LocalDate asOf) {
      Close candidate = null;
      for (Close close : closes) {
        if (!close.symbol().equals(symbol)) {
          continue;
        }
        long daysAfter = ChronoUnit.DAYS.between(tradeDate, close.date());
        if (daysAfter < 0 || daysAfter > 7) {
          continue;
        }
        if (candidate == null || close.date().isBefore(candidate.date())) {
          candidate = close;
        }
      }
      if (candidate == null) {
        return new Missing(symbol, tradeDate);
      }
      if (candidate.date().isAfter(asOf)) {
        return valuation(symbol, asOf);
      }
      return new Found(symbol, tradeDate, candidate.date(), candidate.close());
    }

    /** The close on {@code asOf}, or the latest close in the 7 days before it. Never a later day. */
    public Lookup valuation(String symbol, LocalDate asOf) {
      Close chosen = null;
      for (Close close : closes) {
        if (!close.symbol().equals(symbol)) {
          continue;
        }
        long daysBefore = ChronoUnit.DAYS.between(close.date(), asOf);
        if (daysBefore < 0 || daysBefore > 7) {
          continue;
        }
        if (chosen == null || !close.date().isBefore(chosen.date())) {
          chosen = close;
        }
      }
      if (chosen == null) {
        return new Missing(symbol, asOf);
      }
      return new Found(symbol, asOf, chosen.date(), chosen.close());
    }
  }

  private record Priced(String symbol, LocalDate date, String close, int order) {}

  private Market() {}

  public static Book read(Path caseFolder) {
    Path prices = caseFolder.resolve("prices.csv");
    Path distributions = caseFolder.resolve("distributions.csv");
    List<Priced> priced = Files.exists(prices) ? readPrices(prices) : List.of();
    List<Distribution> paid =
        Files.exists(distributions) ? readDistributions(distributions) : List.of();
    List<Close> closes = new ArrayList<>();
    for (Priced row : priced) {
      closes.add(new Close(row.symbol(), row.date(), row.close()));
    }
    return new Book(List.copyOf(closes), List.copyOf(paid), List.copyOf(priceJumps(priced)));
  }

  private static List<Priced> readPrices(Path file) {
    List<String> lines = readLines(file);
    Map<String, Integer> columns = header(file, lines, List.of("symbol", "date", "close"));
    List<Priced> rows = new ArrayList<>();
    int order = 0;
    for (int i = 1; i < lines.size(); i++) {
      if (lines.get(i).isBlank()) {
        continue;
      }
      List<String> cells = split(lines.get(i));
      String close = cell(columns, cells, "close");
      new BigDecimal(close);
      rows.add(
          new Priced(
              cell(columns, cells, "symbol"),
              LocalDate.parse(cell(columns, cells, "date")),
              close,
              order++));
    }
    return rows;
  }

  private static List<Distribution> readDistributions(Path file) {
    List<String> lines = readLines(file);
    Map<String, Integer> columns =
        header(file, lines, List.of("symbol", "ex_date", "amount_per_share"));
    List<Distribution> rows = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) {
      if (lines.get(i).isBlank()) {
        continue;
      }
      List<String> cells = split(lines.get(i));
      String amount = cell(columns, cells, "amount_per_share");
      new BigDecimal(amount);
      rows.add(
          new Distribution(
              cell(columns, cells, "symbol"),
              LocalDate.parse(cell(columns, cells, "ex_date")),
              amount));
    }
    return rows;
  }

  private static List<Warning> priceJumps(List<Priced> priced) {
    Map<String, List<Priced>> bySymbol = new LinkedHashMap<>();
    for (Priced row : priced) {
      bySymbol.computeIfAbsent(row.symbol(), key -> new ArrayList<>()).add(row);
    }
    List<Warning> warnings = new ArrayList<>();
    for (List<Priced> rows : bySymbol.values()) {
      rows.sort(Comparator.comparing(Priced::date).thenComparingInt(Priced::order));
      for (int i = 1; i < rows.size(); i++) {
        Priced previous = rows.get(i - 1);
        Priced current = rows.get(i);
        long days = ChronoUnit.DAYS.between(previous.date(), current.date());
        if (days < 1 || days > 7) {
          continue;
        }
        BigDecimal before = new BigDecimal(previous.close());
        BigDecimal now = new BigDecimal(current.close());
        if (now.subtract(before).abs().compareTo(before.abs().multiply(TWENTY_PERCENT)) > 0) {
          warnings.add(
              new Warning(
                  "price-jump",
                  fields(
                      "symbol",
                      current.symbol(),
                      "date",
                      current.date().toString(),
                      "previousDate",
                      previous.date().toString()),
                  "Close for "
                      + current.symbol()
                      + " on "
                      + current.date()
                      + " is more than 20% from "
                      + previous.date()
                      + "."));
        }
      }
    }
    return warnings;
  }

  private static Map<String, Integer> header(Path file, List<String> lines, List<String> required) {
    if (lines.isEmpty()) {
      throw new IllegalArgumentException("Missing header in " + file.getFileName());
    }
    Map<String, Integer> columns = new LinkedHashMap<>();
    List<String> names = split(lines.get(0));
    for (int i = 0; i < names.size(); i++) {
      columns.putIfAbsent(names.get(i).trim(), i);
    }
    for (String name : required) {
      if (!columns.containsKey(name)) {
        throw new IllegalArgumentException("Missing column " + name + " in " + file.getFileName());
      }
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

  private static Map<String, Object> fields(String key, Object value, String key2, Object value2) {
    return Map.of(key, value, key2, value2);
  }

  private static Map<String, Object> fields(
      String key, Object value, String key2, Object value2, String key3, Object value3) {
    return Map.of(key, value, key2, value2, key3, value3);
  }

  private static List<String> readLines(Path file) {
    try {
      return Files.readAllLines(file);
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
