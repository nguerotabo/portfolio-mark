package mark.analytics;

import java.time.LocalDate;
import java.util.List;
import mark.ledger.Activity;
import mark.ledger.CsvReader;
import mark.market.Market;

/**
 * The four paths, the three steps, and the headline. Each step is the gap between two neighbouring
 * paths, so all on day one plus the three steps is your account.
 *
 * <p>Proves walkthrough, weekend-deposit, as-of-input, buys-only, and half-cent.
 */
public record Bridge(long allOnDayOne, long fundOnYourDates, long benchmark, long account) {

  public sealed interface Result permits Built, Refused {}

  public record Built(Bridge bridge, Picks.Replay replay) implements Result {}

  public record Refused(CsvReader.Error error) implements Result {}

  public long whenMoneyArrived() {
    return fundOnYourDates - allOnDayOne;
  }

  public long cashAndSales() {
    return benchmark - fundOnYourDates;
  }

  public long stockSelection() {
    return account - benchmark;
  }

  /** Your choices versus the fund on your dates. */
  public long headline() {
    return cashAndSales() + stockSelection();
  }

  /** When both replays refuse, the refusal on the earlier row wins. */
  public static Result calculate(
      List<Activity> activities, Market.Book market, String fund, LocalDate asOfDate) {
    Picks.Result picks = Picks.calculatePicks(activities, market, fund, asOfDate);
    ContributionPaths.Result paths =
        ContributionPaths.calculate(activities, market, fund, asOfDate);

    if (picks instanceof Picks.Refused refusedPicks) {
      if (paths instanceof ContributionPaths.Refused refusedPaths
          && refusedPaths.sourceRow() < refusedPicks.sourceRow()) {
        return new Refused(refusedPaths.error());
      }
      return new Refused(refusedPicks.error());
    }
    if (paths instanceof ContributionPaths.Refused refusedPaths) {
      return new Refused(refusedPaths.error());
    }

    Picks.Replay replay = (Picks.Replay) picks;
    ContributionPaths.Values values = (ContributionPaths.Values) paths;
    return new Built(
        new Bridge(
            values.allOnDayOne(), values.fundOnYourDates(), replay.benchmark(), replay.account()),
        replay);
  }
}
