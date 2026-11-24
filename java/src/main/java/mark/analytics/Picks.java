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
 * The buys-only replay: your account, the benchmark, and the pick impacts, in whole cents.
 *
 * <p>Proves buys-only, half-cent, and refuse-missing-close.
 */
public final class Picks {

  public record Lot(long account, long fund) {}

  public record Pick(String symbol, long impact, long dividends, List<Lot> lots) {}

  public sealed interface Result permits Replay, Refused {}

  public record Replay(long account, long benchmark, List<Pick> picks) implements Result {
    public long stockSelection() {
      return account - benchmark;
    }
  }

  public record Refused(CsvReader.Error error) implements Result {}

  private Picks() {}

  public static Result calculatePicks(
      List<Activity> activities, Market.Book market, String fund, LocalDate asOfDate) {
    Map<String, List<Lot>> lotsBySymbol = new TreeMap<>();
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
            return new Refused(missing.error());
          }
          Market.Lookup fundTrade = market.tradeClose(fund, buy.date(), asOfDate);
          if (fundTrade instanceof Market.Missing missing) {
            return new Refused(missing.error());
          }
          Market.Lookup fundClose = market.valuation(fund, asOfDate);
          if (fundClose instanceof Market.Missing missing) {
            return new Refused(missing.error());
          }

          Rational accountValue =
              shares.multiply(Rational.parse(((Market.Found) stockClose).close()));
          Rational fundShares =
              dollars.divide(Rational.parse(((Market.Found) fundTrade).close()));
          Rational fundValue =
              fundShares.multiply(Rational.parse(((Market.Found) fundClose).close()));

          lotsBySymbol
              .computeIfAbsent(buy.symbol(), symbol -> new ArrayList<>())
              .add(new Lot(accountValue.toCents(), fundValue.toCents()));
          spent += -buy.cash();
        }
        case Deposit deposit -> {
          hasDeposits = true;
          deposits += deposit.cash();
        }
        case Interest interest -> interestAndTax += interest.cash();
        case Tax tax -> interestAndTax += tax.cash();
        case Sell sell ->
            throw new UnsupportedOperationException(
                "sales are not built yet, source row " + sell.sourceRow());
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
    for (Map.Entry<String, List<Lot>> entry : lotsBySymbol.entrySet()) {
      long accountSum = 0;
      long fundSum = 0;
      for (Lot lot : entry.getValue()) {
        accountSum += lot.account();
        fundSum += lot.fund();
      }
      picks.add(new Pick(entry.getKey(), accountSum - fundSum, 0, List.copyOf(entry.getValue())));
      accountLots += accountSum;
      benchmarkLots += fundSum;
    }

    return new Replay(
        accountLots + cashOutsideLots, benchmarkLots + cashOutsideLots, List.copyOf(picks));
  }
}
