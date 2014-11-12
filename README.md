Fast counter
======

## Benchmark results (Java 1.7.0_60)

```
Benchmark                         (counterType)   Mode  Samples          Score         Error  Units
u.d.s.CountersBenchmark.rw               ATOMIC  thrpt       20   36019198.469 ± 6167269.667  ops/s
u.d.s.CountersBenchmark.rw:get           ATOMIC  thrpt       20   24629842.297 ± 6180029.357  ops/s
u.d.s.CountersBenchmark.rw:inc           ATOMIC  thrpt       20   11389356.172 ±   94384.450  ops/s
u.d.s.CountersBenchmark.rw                 FAST  thrpt       20  376356081.531 ± 7231356.982  ops/s
u.d.s.CountersBenchmark.rw:get             FAST  thrpt       20    2072684.410 ±  222969.181  ops/s
u.d.s.CountersBenchmark.rw:inc             FAST  thrpt       20  374283397.122 ± 7317215.259  ops/s
```
Increment operation become blazingly fast (~34 times faster)

## Benchmark results (Java 1.8.0_05)
```
Benchmark                         (counterType)   Mode  Samples          Score         Error  Units
u.d.s.CountersBenchmark.rw               ATOMIC  thrpt       20   41575068.265 ± 6011493.964  ops/s
u.d.s.CountersBenchmark.rw:get           ATOMIC  thrpt       20   30451364.022 ± 5884330.330  ops/s
u.d.s.CountersBenchmark.rw:inc           ATOMIC  thrpt       20   11123704.243 ±  149478.137  ops/s
u.d.s.CountersBenchmark.rw                 FAST  thrpt       20  353811805.871 ± 9789148.252  ops/s
u.d.s.CountersBenchmark.rw:get             FAST  thrpt       20    2081168.895 ±  139712.029  ops/s
u.d.s.CountersBenchmark.rw:inc             FAST  thrpt       20  351730636.976 ± 9850925.452  ops/s
```
A bit slower but also fast (~31 times faster)

Test environment:
 Intel® Core™ i7-3770 CPU @ 3.40GHz × 8
 RAM 32GB
 Ubuntu 14.04
