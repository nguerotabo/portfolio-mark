package mark.analytics;

import java.math.BigDecimal;
import java.math.BigInteger;

public record Rational(BigInteger numerator, BigInteger denominator) {

    private static final BigInteger HUNDRED = BigInteger.valueOf(100);

    public static final Rational ZERO = new Rational(BigInteger.ZERO, BigInteger.ONE);

    public Rational {
        if (denominator.signum() == 0) {
            throw new ArithmeticException("denominator is zero");
        }
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger gcd = numerator.gcd(denominator);
        numerator = numerator.divide(gcd);
        denominator = denominator.divide(gcd);
    }

    public static Rational parse(String decimal) {
        BigDecimal bd = new BigDecimal(decimal.trim());
        if (bd.scale() >= 0) {
            return new Rational(bd.unscaledValue(), BigInteger.TEN.pow(bd.scale()));
        }
        return new Rational(bd.unscaledValue().multiply(BigInteger.TEN.pow(-bd.scale())), BigInteger.ONE);
    }

    public static Rational ofCents(long cents) {
        return new Rational(BigInteger.valueOf(cents), HUNDRED);
    }

    public Rational multiply(Rational other) {
        return new Rational(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
    }

    public Rational divide(Rational other) {
        return new Rational(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
    }

    public Rational add(Rational other) {
        return new Rational(
                numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }

    public Rational subtract(Rational other) {
        return add(new Rational(other.numerator.negate(), other.denominator));
    }

    public int compareTo(Rational other) {
        return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
    }

    public int signum() {
        return numerator.signum();
    }

    /** Dollars to whole cents, half to even. The only rounding in the replay. */
    public long toCents() {
        BigInteger[] qr = numerator.multiply(HUNDRED).divideAndRemainder(denominator);
        BigInteger cents = qr[0];
        BigInteger remainder = qr[1];
        int half = remainder.abs().shiftLeft(1).compareTo(denominator);
        if (half > 0 || (half == 0 && cents.testBit(0))) {
            cents = cents.add(BigInteger.valueOf(remainder.signum()));
        }
        return cents.longValueExact();
    }
}
