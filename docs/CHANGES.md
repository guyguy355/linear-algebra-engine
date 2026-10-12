# Portfolio edition changes

## Correctness and reliability

- Normalize n-ary operations recursively and copy caller operand lists before restructuring them.
- Reject wrong operator arity, incompatible dimensions, ragged/empty matrices, and nonnumeric JSON values.
- Propagate task failures to batch callers and keep workers reusable after ordinary task failures.
- Reject submissions after shutdown and nested blocking calls from pool workers.
- Publish worker availability only after updating the priority score and metrics.
- Track each batch independently; drain accepted tasks on interruptions/failure.
- Acquire reciprocal-add locks in stable order and release locks with `finally`.
- Capture a stable vector-array snapshot for whole-matrix reads.
- Share one right-hand snapshot across multiplication tasks; remove per-scalar locking from that loop.
- Add graceful `AutoCloseable` cleanup and useful CLI exit statuses.

## Presentation and demo

- Name the distributable MatrixLab and put the Maven project at its root.
- Add Matrix Race with seeded inputs, warmups, randomized measurement order, median timings, numerical verification, and HTML/CSV/JSON reports.
- Add a shaded executable JAR build, test runner configuration, and Java 17 baseline.
- Retain original authorship attribution; omit submission PDF, identification numbers, editor metadata, copied Git history, and old compiled output.
- Replace administrative README content with setup, architecture, limitations, and interview documentation.

## Test changes

New regression tests cover the reproduced issues and the new report feature. Prior test names `SherdMatrixTest` / `SherdVectorTest` were corrected. One prior test intentionally accepted ragged matrices; it now asserts the new rejection contract. A dimension-mismatch test that could pass without an exception now requires one. Worker tests wait for completion callbacks instead of assuming that a notification inside the task means worker metrics have already been updated.

The underlying original examples and their expected outputs remain part of the suite.
