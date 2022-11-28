package mutating;

/*
 * Author: Simon Roberts
 * Illustration of mutating collect operations for comparison
 * of parallel vs sequential modes
 * Notes:
 * - Start with sequential stream mode and a small value of COUNT
 *   then increment empirically to get an execution time that's long
 *   enough to amortize the JIT time (e.g. 30 seconds)
 * - switch to parallel mode
 * - works well with a decent speedup in a 1.8 JVM,
 *   fails dismally in newer JVMs
 *
 * My results (AMD Ryzen 7 3700X (8 core/16 hardware thread),
 *    32 GiB RAM, doing lots of other stuff :)
 * JDK 1.8 8u282-b08
 *    COUNT = 10_000_000_000
 *    sequential mode: time ~30 seconds, rate ~335 million
 *    parallel mode: time 2.5 seconds (*** likely not long enough for JIT amortization!)
 *       rate 4,300 million
 *
 * JDK 17 17.0.1+12
 *    COUNT = 10_000_000_000
 *    sequential mode: time 90 seconds, rate 110 million
 *    parallel mode: time 430 seconds, rate 23 million
 *
 * JDK 17 17.0.1+12
 **** USING ThreadLocalRandom.current().nextDouble(-1, +1)
 *    inside DoubleStream.generate / iterate
 *    rather than TLR..double()
 *
 *    COUNT = 10_000_000_000
 *    sequential mode: time 35 seconds, rate 290 million
 *    parallel mode: time 6.5 seconds, rate 1,500 million
 */

import java.util.OptionalDouble;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.DoubleStream;

final class Average {
  private double sum;
  private long count;

  public Average(double sum, long count) {
    this.sum = sum;
    this.count = count;
  }

  public Average() {
    this(0,0);
  }

  public void include(double d) {
    this.sum += d;
    this.count++;
  }

  public void merge(Average other) {
    this.sum += other.sum;
    this.count += other.count;
  }

  public OptionalDouble get() {
    if (count > 0) {
      return OptionalDouble.of(this.sum / this.count);
    } else {
      return OptionalDouble.empty();
    }
  }
}

public final class Averager {
  public static void main(String[] args) {
    final long COUNT = 10_000_000_000L;
    final long start = System.nanoTime();

    // ordered random source
//    DoubleStream.iterate(0.0, x -> ThreadLocalRandom.current().nextDouble(-1, +1))
//        .limit(COUNT)

// unordered random source
    DoubleStream.generate(() -> ThreadLocalRandom.current().nextDouble(-1, +1))
        .limit(COUNT)

// This is as slow, and as non-parallelizable as TLR..doubles!
//    new Random().doubles(COUNT, -1, +1)

//    ThreadLocalRandom.current().doubles(COUNT, -1, +1)
//        .unordered()
        .parallel()
        .collect(Average::new, Average::include, Average::merge)
        .get()
        .ifPresent(a -> System.out.println("Average is " + a));

    final long time = System.nanoTime() - start;
    System.out.printf("Averaged %,d items in %6.4f seconds\nRate is %,9.0f per second",
        COUNT, (time / 1_000_000_000.0), COUNT * 1_000_000_000.0 / time);
  }
}
