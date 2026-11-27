package mark.analytics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import mark.ledger.Activity;
import mark.ledger.Buy;
import mark.ledger.CsvReader;
import mark.ledger.Deposit;
import mark.ledger.Dividend;
import mark.ledger.Interest;
import mark.ledger.Sell;
import mark.ledger.Tax;
import mark.market.Market;

/**
 * The replay of your account and the benchmark, with sales by fraction, and the pick impacts, in
 * whole cents.
 *
 * <p>Proves buys-only, half-cent, refuse-missing-close, walkthrough, and refuse-oversell.
 */
public final class Picks {

  public record Lot(long account, long fund) {}

  public record Pick(String symbol, long impact, long dividends, List<Lot> lots) {}

  public sealed interface Result permits Replay, Refused {}

  public record Replay(long account, long benchmark, List<Pick> picks) implements Result {
    public long stockSelection() {
      return account - benchmark;
    }

    /** The most positive impact, or null. A tie goes to the symbol that sorts first. */
    public String bestPick() {
      Pick best = null;
      for (Pick pick : picks) {
        if (pick.impact() > 0 && (best == null || pick.impact() > best.impact())) {
          best = pick;
        }
      }
      return best == null ? null : best.symbol();
    }

    /** The most negative impact, or null. A tie goes to the symbol that sorts first. */
    public String largestDrag() {
      Pick drag = null;
      for (Pick pick : picks) {
        if (pick.impact() < 0 && (drag == null || pick.impact() < drag.impact())) {
          drag = pick;
        }
      }
      return drag == null ? null : drag.symbol();
    }
  }

  /** {@code sourceRow} is the row the refusal was hit on, so the earliest one can win. */
  public record Refused(CsvReader.Error error, int sourceRow) implements Result {}

  /** One purchase, exact until it is rounded once on the as-of date. */
  private static final class OpenLot {
    private final Rational close;
    private final Rational fundClose;
    private Rational shares;
    private Rational proceeds = Rational.ZERO;
    private Rational fundShares;
    private Rational fundProceeds = Rational.ZERO;

    private OpenLot(Rational shares, Rational close, Rational fundShares, Rational fundClose) {
      this.shares = shares;
      this.close = close;
      this.fundShares = fundShares;
      this.fundClose = fundClose;
    }

    private Lot toLot() {
      return new Lot(
          shares.multiply(close).add(proceeds).toCents(),
          fundShares.multiply(fundClose).add(fundProceeds).toCents());
    }
  }

  private Picks() {}

  public static Result calculatePicks(
      List<Activity> activities, Market.Book market, String fund, LocalDate asOfDate) {
    Map<String, List<OpenLot>> lotsBySymbol = new TreeMap<>();
    long deposits = 0;
    long spent = 0;
    long interestAndTax = 0;
    boolean hasDeposits = false;

    for (Activity activity : activities) {
      switch (activity) {
        case Buy buy -> {
          Rational dollars = Rational.ofCents(-buy.cash());
          Rational shares = Rational.parse(buy.quantity());

          Market.Lookup stockClose = market.valuation(buy.symbol(), asOfDate);
          if (stockClose instanceof Market.Missing missing) {
            return new Refused(missing.error(), buy.sourceRow());
          }
          Market.Lookup fundTrade = market.tradeClose(fund, buy.date(), asOfDate);
          if (fundTrade instanceof Market.Missing missing) {
            return new Refused(missing.error(), buy.sourceRow());
          }
          Market.Lookup fundClose = market.valuation(fund, asOfDate);
          if (fundClose instanceof Market.Missing missing) {
            return new Refused(missing.error(), buy.sourceRow());
          }

          Rational fundShares = dollars.divide(close(fundTrade));
          lotsBySymbol
              .computeIfAbsent(buy.symbol(), symbol -> new ArrayList<>())
              .add(new OpenLot(shares, close(stockClose), fundShares, close(fundClose)));
          spent += -buy.cash();
        }
        case Sell sell -> {
          Refused refused = sell(sell, lotsBySymbol, market, fund, asOfDate);
          if (refused != null) {
            return refused;
          }
        }
        case Deposit deposit -> {
          hasDeposits = true;
          deposits += deposit.cash();
        }
        case Interest interest -> interestAndTax += interest.cash();
        case Tax tax -> interestAndTax += tax.cash();
        case Dividend dividend ->
            throw new UnsupportedOperationException(
                "dividends are not built yet, source row " + dividend.sourceRow());
      }
    }

    long contributions = hasDeposits ? deposits : spent;
    long cashOutsideLots = contributions - spent + interestAndTax;

    List<Pick> picks = new ArrayList<>();
    long accountLots = 0;
    long benchmarkLots = 0;
    for (Map.Entry<String, List<OpenLot>> entry : lotsBySymbol.entrySet()) {
      List<Lot> lots = new ArrayList<>();
      long accountSum = 0;
      long fundSum = 0;
      for (OpenLot open : entry.getValue()) {
        Lot lot = open.toLot();
        lots.add(lot);
        accountSum += lot.account();
        fundSum += lot.fund();
      }
      picks.add(new Pick(entry.getKey(), accountSum - fundSum, 0, List.copyOf(lots)));
      accountLots += accountSum;
      benchmarkLots += fundSum;
    }

    return new Replay(
        accountLots + cashOutsideLots, benchmarkLots + cashOutsideLots, List.copyOf(picks));
  }

  /**
   * The sale fraction: shares sold over shares held just before the sale. Every lot of the symbol,
   * and the fund lot that came from it, sells that fraction. Proceeds stay in the lot as cash. A
   * sale never consumes one chosen lot.
   */
  private static Refused sell(
      Sell sell,
      Map<String, List<OpenLot>> lotsBySymbol,
      Market.Book market,
      String fund,
      LocalDate asOfDate) {
    List<OpenLot> lots = lotsBySymbol.getOrDefault(sell.symbol(), List.of());
    Rational held = Rational.ZERO;
    for (OpenLot lot : lots) {
      held = held.add(lot.shares);
    }
    Rational sold = Rational.parse(sell.quantity());
    if (sold.compareTo(held) > 0) {
      return new Refused(
          new CsvReader.Error(
              "oversell",
              Map.of("symbol", sell.symbol(), "date", sell.date().toString()),
              "Sale of "
                  + sell.quantity()
                  + " "
                  + sell.symbol()
                  + " on "
                  + sell.date()
                  + " is more than the shares held."),
          sell.sourceRow());
    }
    Market.Lookup fundTrade = market.tradeClose(fund, sell.date(), asOfDate);
    if (fundTrade instanceof Market.Missing missing) {
      return new Refused(missing.error(), sell.sourceRow());
    }
    if (held.signum() == 0) {
      return null;
    }

    Rational fraction = sold.divide(held);
    Rational cash = Rational.ofCents(sell.cash());
    Rational fundClose = close(fundTrade);
    for (OpenLot lot : lots) {
      lot.proceeds = lot.proceeds.add(cash.multiply(lot.shares).divide(held));
      lot.shares = lot.shares.subtract(lot.shares.multiply(fraction));
      Rational fundSold = lot.fundShares.multiply(fraction);
      lot.fundProceeds = lot.fundProceeds.add(fundSold.multiply(fundClose));
      lot.fundShares = lot.fundShares.subtract(fundSold);
    }
    return null;
  }

  private static Rational close(Market.Lookup lookup) {
    return Rational.parse(((Market.Found) lookup).close());
  }
}
