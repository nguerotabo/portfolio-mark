package mark.analytics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import mark.ledger.Activity;
import mark.ledger.Buy;
import mark.ledger.CsvReader;
import mark.ledger.Deposit;
import mark.ledger.Interest;
import mark.ledger.Tax;
import mark.market.Market;

/**
 * All on day one and fund on your dates. Each contribution is its own fund lot, bought and held.
 * Neither path sells. Each lot is paid the fund's distributions inside its single rounding.
 * Interest and tax are cash on both.
 *
 * <p>Proves walkthrough, weekend-deposit, as-of-input, buys-only, half-cent,
 * dividend-and-distribution, and self-holding.
 */
public final class ContributionPaths {

  public record Contribution(int sourceRow, LocalDate date, long cents) {}

  public sealed interface Result permits Values, Refused {}

  public record Values(long allOnDayOne, long fundOnYourDates) implements Result {}

  /** {@code sourceRow} is the contribution the refusal was hit on. */
  public record Refused(CsvReader.Error error, int sourceRow) implements Result {}

  private ContributionPaths() {}

  /** The deposits when the file has any, otherwise each purchase on its own date. */
  public static List<Contribution> contributions(List<Activity> activities) {
    List<Contribution> deposits = new ArrayList<>();
    List<Contribution> purchases = new ArrayList<>();
    for (Activity activity : activities) {
      if (activity instanceof Deposit deposit) {
        deposits.add(new Contribution(deposit.sourceRow(), deposit.date(), deposit.cash()));
      } else if (activity instanceof Buy buy) {
        purchases.add(new Contribution(buy.sourceRow(), buy.date(), -buy.cash()));
      }
    }
    return List.copyOf(deposits.isEmpty() ? purchases : deposits);
  }

  public static Result calculate(
      List<Activity> activities, Market.Book market, String fund, LocalDate asOfDate) {
    long interestAndTax = 0;
    for (Activity activity : activities) {
      if (activity instanceof Interest interest) {
        interestAndTax += interest.cash();
      } else if (activity instanceof Tax tax) {
        interestAndTax += tax.cash();
      }
    }

    List<Contribution> contributions = contributions(activities);
    if (contributions.isEmpty()) {
      return new Values(interestAndTax, interestAndTax);
    }

    Contribution first = contributions.get(0);
    Market.Lookup dayOne = market.tradeClose(fund, first.date(), asOfDate);
    if (dayOne instanceof Market.Missing missing) {
      return new Refused(missing.error(), first.sourceRow());
    }
    Market.Lookup valuation = market.valuation(fund, asOfDate);
    if (valuation instanceof Market.Missing missing) {
      return new Refused(missing.error(), first.sourceRow());
    }
    Rational dayOneClose = close(dayOne);
    Rational asOfClose = close(valuation);
    List<Market.Distribution> owed = Distributions.owed(market, fund, asOfDate);
    Rational dayOnePerShare =
        asOfClose.add(Distributions.perShareHeldFrom(owed, first.date()));

    long allOnDayOne = interestAndTax;
    long fundOnYourDates = interestAndTax;
    for (Contribution contribution : contributions) {
      Market.Lookup trade = market.tradeClose(fund, contribution.date(), asOfDate);
      if (trade instanceof Market.Missing missing) {
        return new Refused(missing.error(), contribution.sourceRow());
      }
      Rational dollars = Rational.ofCents(contribution.cents());
      Rational yourDatesPerShare =
          asOfClose.add(Distributions.perShareHeldFrom(owed, contribution.date()));
      allOnDayOne += dollars.divide(dayOneClose).multiply(dayOnePerShare).toCents();
      fundOnYourDates += dollars.divide(close(trade)).multiply(yourDatesPerShare).toCents();
    }
    return new Values(allOnDayOne, fundOnYourDates);
  }

  private static Rational close(Market.Lookup lookup) {
    return Rational.parse(((Market.Found) lookup).close());
  }
}
