/*
 * Author: Simon Roberts
 * Illustration of non-mutating reduction for comparison
 * of parallel vs sequential modes
 * Notes:
 * - Start with sequential stream mode and a small value of COUNT
 *   then increment empirically to get an execution time that's long
 *   enough to amortize the JIT time (e.g. 30 seconds)
 * - switch to parallel mode
 * - works well with a decent speedup in a 1.8 JVM,
 *   fails dismally in newer JVMs
 *
 * My results (AMD Ryzen 7 3700X 8 core/16 hardware thread,
 *    32 GiB RAM, doing lots of other stuff :)
 * JDK 1.8 8u282-b08
 *    COUNT = 4_000_000_000
 *    sequential mode: time ~24 seconds, rate ~165 million
 *    parallel mode: time ~9 seconds, rate ~450 million
 *
 * JDK 17 17.0.1+12
 *    COUNT = 4_000_000_000
 *    sequential mode: time ~ 90 seconds, rate ~70 million
 *    parallel mode: time ~160 seconds, rate ~24 million
 */

package nonmutating;


import java.util.OptionalDouble;
import java.util.concurrent.ThreadLocalRandom;

final class Average {
  private final double sum;
  private final long count;

  public Average(double sum, long count) {
    this.sum = sum;
    this.count = count;
  }

  public Average include(double d) {
    return new Average(this.sum + d, this.count + 1);
  }

  public Average merge(Average other) {
    return new Average(this.sum + other.sum, this.count + other.count);
  }

  public OptionalDouble get() { // NaN is a horrible sentinel value
    if (count > 0) {
      return OptionalDouble.of(this.sum / this.count);
    } else {
      return OptionalDouble.empty();
    }
  }
}

public final class Averager {
  public static void main(String[] args) {
    final long COUNT = 4_000_000_000L;
    final long start = System.nanoTime();

    ThreadLocalRandom.current().doubles(COUNT, -1, +1)
//        .parallel()
        .boxed()
        .reduce(new Average(0, 0), Average::include, Average::merge)
        .get()
        .ifPresent(a -> System.out.println("Average is " + a));

    final long time = System.nanoTime() - start;
    System.out.printf("Averaged %,d items in %6.4f seconds\nRate is %,9.0f per second",
      COUNT, (time / 1_000_000_000.0), COUNT * 1_000_000_000.0 / time);
  }
}
