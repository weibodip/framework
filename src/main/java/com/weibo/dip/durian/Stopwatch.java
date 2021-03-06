package com.weibo.dip.durian;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ticker;
import java.util.concurrent.TimeUnit;

@Beta
@GwtCompatible(emulated = true)
public final class Stopwatch {
  private final Ticker ticker;
  private boolean isRunning;
  private long elapsedNanos;
  private long startTick;

  /**
   * Creates (but does not start) a new stopwatch using {@link System#nanoTime} as its time source.
   */
  public Stopwatch() {
    this(Ticker.systemTicker());
  }

  /** Creates (but does not start) a new stopwatch, using the specified time source. */
  public Stopwatch(Ticker ticker) {
    this.ticker = checkNotNull(ticker, "ticker");
  }

  /**
   * Returns {@code true} if {@link #start()} has been called on this stopwatch, and {@link #stop()}
   * has not been called since the last call to {@code start()}.
   */
  public boolean isRunning() {
    return isRunning;
  }

  /**
   * Starts the stopwatch.
   *
   * @return this {@code Stopwatch} instance
   * @throws IllegalStateException if the stopwatch is already running.
   */
  public Stopwatch start() {
    checkState(
        !isRunning, "This stopwatch is already running; it cannot be started more than once.");
    isRunning = true;
    startTick = ticker.read();
    return this;
  }

  /**
   * Stops the stopwatch. Future reads will return the fixed duration that had elapsed up to this
   * point.
   *
   * @return this {@code Stopwatch} instance
   * @throws IllegalStateException if the stopwatch is already stopped.
   */
  public Stopwatch stop() {
    long tick = ticker.read();
    checkState(
        isRunning, "This stopwatch is already stopped; it cannot be stopped more than once.");
    isRunning = false;
    elapsedNanos += tick - startTick;
    return this;
  }

  /**
   * Sets the elapsed time for this stopwatch to zero, and places it in a stopped state.
   *
   * @return this {@code Stopwatch} instance
   */
  public Stopwatch reset() {
    elapsedNanos = 0;
    isRunning = false;
    return this;
  }

  private long elapsedNanos() {
    return isRunning ? ticker.read() - startTick + elapsedNanos : elapsedNanos;
  }

  /**
   * Returns the current elapsed time shown on this stopwatch, expressed in the desired time unit,
   * with any fraction rounded down.
   *
   * <p>Note that the overhead of measurement can be more than a microsecond, so it is generally not
   * useful to specify {@link TimeUnit#NANOSECONDS} precision here.
   *
   * @since 14.0 (since 10.0 as {@code elapsedTime()})
   */
  public long elapsed(TimeUnit desiredUnit) {
    return desiredUnit.convert(elapsedNanos(), NANOSECONDS);
  }

  /**
   * Returns the current elapsed time shown on this stopwatch, expressed in the desired time unit,
   * with any fraction rounded down.
   *
   * <p>Note that the overhead of measurement can be more than a microsecond, so it is generally not
   * useful to specify {@link TimeUnit#NANOSECONDS} precision here.
   *
   * @deprecated Use {@link com.google.common.base.Stopwatch#elapsed(TimeUnit)} instead. This method
   *     is scheduled to be removed in Guava release 16.0.
   */
  @Deprecated
  public long elapsedTime(TimeUnit desiredUnit) {
    return elapsed(desiredUnit);
  }

  /**
   * Returns the current elapsed time shown on this stopwatch, expressed in milliseconds, with any
   * fraction rounded down. This is identical to {@code elapsed(TimeUnit.MILLISECONDS)}.
   *
   * @deprecated Use {@code stopwatch.elapsed(MILLISECONDS)} instead. This method is scheduled to be
   *     removed in Guava release 16.0.
   */
  @Deprecated
  public long elapsedMillis() {
    return elapsed(MILLISECONDS);
  }

  /** Returns a string representation of the current elapsed time. */
  @GwtIncompatible("String.format()")
  @Override
  public String toString() {
    return toString(4);
  }

  /**
   * Returns a string representation of the current elapsed time, choosing an appropriate unit and
   * using the specified number of significant figures. For example, at the instant when {@code
   * elapsed(NANOSECONDS)} would return {1234567}, {@code toString(4)} returns {@code "1.235 ms"}.
   *
   * @deprecated Use {@link #toString()} instead. This method is scheduled to be removed in Guava
   *     release 15.0.
   */
  @Deprecated
  @GwtIncompatible("String.format()")
  public String toString(int significantDigits) {
    long nanos = elapsedNanos();

    TimeUnit unit = chooseUnit(nanos);
    double value = (double) nanos / NANOSECONDS.convert(1, unit);

    // Too bad this functionality is not exposed as a regular method call
    return String.format("%." + significantDigits + "g %s", value, abbreviate(unit));
  }

  private static TimeUnit chooseUnit(long nanos) {
    if (SECONDS.convert(nanos, NANOSECONDS) > 0) {
      return SECONDS;
    }
    if (MILLISECONDS.convert(nanos, NANOSECONDS) > 0) {
      return MILLISECONDS;
    }
    if (MICROSECONDS.convert(nanos, NANOSECONDS) > 0) {
      return MICROSECONDS;
    }
    return NANOSECONDS;
  }

  private static String abbreviate(TimeUnit unit) {
    switch (unit) {
      case NANOSECONDS:
        return "ns";
      case MICROSECONDS:
        return "\u03bcs"; // μs
      case MILLISECONDS:
        return "ms";
      case SECONDS:
        return "s";
      default:
        throw new AssertionError();
    }
  }
}
