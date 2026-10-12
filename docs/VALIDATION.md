# Validation record

Validated in Linux with Java 17.0.20 on 2026-10-11.

## Build and tests

`mvn clean verify` completed successfully. The preserved Surefire reports contain **59 tests, 0 failures, 0 errors, 0 skipped** across nine test classes. The executable shaded JAR was produced by this build.

Coverage includes matrix/vector operations, nested expression normalization, immutable operand lists, dimension validation, ordered locking, worker failure propagation, executor lifecycle and Matrix Race output correctness.

## Packaged CLI

The release JAR successfully solved `examples/example1.json`. An invalid addition request exited with status 1 rather than reporting success. The release JAR is the same binary produced by the verified build.

## Recorded demo

`race 128 4 5` with seed 42 and three warmups produced the files in `demo-preview/`. All engine outputs matched the serial reference (maximum error 0).

| Mode | Median ms |
| --- | ---: |
| Serial reference | 1.651 |
| Engine, 1 worker | 4.849 |
| Engine, 2 workers | 3.680 |
| Engine, 4 workers | 3.685 |

The engine was slower than the serial reference in this run. Scheduling, validation, copying and synchronization overhead are included in engine measurements. These numbers are one environment-specific educational experiment, not a general performance claim or JMH benchmark. See README for timing boundaries.

## Limits

Windows PowerShell instructions are provided but were not executed on Windows here. Concurrency tests cover selected failure and interleaving cases; passing tests do not prove absence of all races or deadlocks. No GitHub publication was performed.
