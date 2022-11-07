This provides a simple example of using reduce and collect,
both in sequential mode and parallel mode, in Java's Streams
API.

The operation has essentially no operations in the stream body,
and the computational load of the terminal operation is also
very simple, so it represents a severe corner case test for the
efficiency of parallelization.

There are two variants, one using a non-mutating reduce() method
and the other using a mutating collect() method. As expected,
the performance of the collect operation is significantly better
than that of the reduce, presumably due to avoiding several billion
object allocation/initialization operations

With a version 8 JVM, parallel mode is faster in both cases, and
dramatically faster with the collect operation. At a guess, it
seems fair to imagine that the GC load created by the reduce
operation is using hardware threads that are therefore unavilable
for the parallel operation.

However, the performance with newer JVMs (17 in my test, but I've
seen this consitently on many newer JVMs) is quite surprising. In
sequential mode, the throughput is substantially reduced (2~3x)
compared with the older version 8 JVM. But in parallel mode, both
the non-mutating reduce and mutating collect are very much slower
than the sequential mode. This reflects a behavior I recall (but
didn't document) when the Stream API was first released. The speed
in reduce drops almost 3x again, and in collect drops almost 4x.

It's entirely possible that the effect is simply because of some
optimization that would improve performance with realistic test
conditions (i.e. actual workload in the stream pipeline)! However,
it's somewhat disturbing in the absence of an authoritative
explanation.

A second concern relates to the ordered mode. This code runs by
default in an unordered mode, because the random number generator
creates an unordered source. If an ordered source is used (present
but commented out) it runs out of memory. That's expected. However,
adding .unordered() to the stream does *not* correct this. That
seems wrong.