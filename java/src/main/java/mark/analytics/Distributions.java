package mark.analytics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import mark.market.Market;

/**
 * The comparison fund's distributions that a fund path is paid. A lot is paid for an ex-date when it
 * held the shares at the end of the day before, so a lot bought on the ex-date is not paid. Neither
 * side reinvests.
 *
 * <p>Proves dividend-and-distribution and self-holding.
 */
final class Distributions {

  private Distributions() {}

  /** The fund's distributions with an ex-date on or before {@code asOf}, by ex-date. */
  static List<Market.Distribution> owed(Market.Book market, String fund, LocalDate asOf) {
    List<Market.Distribution> owed = new ArrayList<>();
    for (Market.Distribution distribution : market.distributions()) {
      if (distribution.symbol().equals(fund) && !distribution.exDate().isAfter(asOf)) {
        owed.add(distribution);
      }
    }
    owed.sort(Comparator.comparing(Market.Distribution::exDate));
    return List.copyOf(owed);
  }

  /** The amount per share paid to a lot bought on {@code bought} and held to the as-of date. */
  static Rational perShareHeldFrom(List<Market.Distribution> owed, LocalDate bought) {
    Rational total = Rational.ZERO;
    for (Market.Distribution distribution : owed) {
      if (distribution.exDate().isAfter(bought)) {
        total = total.add(amount(distribution));
      }
    }
    return total;
  }

  static Rational amount(Market.Distribution distribution) {
    return Rational.parse(distribution.amountPerShare());
  }
}
