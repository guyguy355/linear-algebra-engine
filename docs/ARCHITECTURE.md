# Architecture and contracts

## Evaluation

`InputParser` reads JSON through Jackson. Each leaf is a matrix; each internal node is an operation. `associativeNesting()` first normalizes every subtree. Unary operations require one child; addition and multiplication accept two or more children and fold left without changing order.

`LinearAlgebraEngine.run()` serializes calls on one instance. It finds a resolvable node, validates operand dimensions, loads owned working matrices, builds row tasks, waits for successful completion, then replaces the node with the result. A thrown task fails the batch rather than publishing a successful result. Earlier resolved child nodes are not rolled back.

`SharedMatrix.readRowMajor()` captures one vector-array snapshot, locks those vectors, copies their contents, and releases the same locks. The engine does not overlap structural loads with operations. Public low-level memory methods are not a transactional matrix API.

## Task lifecycle

1. A caller waits for an idle worker under the executor monitor.
2. The executor checks its shutdown state, removes one worker from the priority queue, increments the in-flight count, and offers one task.
3. The worker executes outside the executor monitor, catches task failures, updates duration/counters, and marks itself idle.
4. The completion callback re-enqueues the worker with its updated score, decrements in-flight work, wakes waiting callers, and completes that task's future.
5. `submitAll()` drains its own accepted futures and then throws on failure. Concurrent batches do not share a global completion barrier.

The queue key is stable while a worker is idle. Ties use worker IDs in the executor's comparator. Scores use accumulated nanoseconds multiplied by seeded per-worker weights; wall-clock timing still makes dispatch nondeterministic.

## Locking

Each vector owns a reentrant read/write lock and a monotonic lock-order ID. Addition acquires both write locks in ID order. This conservative choice avoids reciprocal-add deadlocks, at the cost of blocking readers of the right operand during the operation. Self-add is safe because locks are reentrant. Dot products hold read locks. All release paths use `finally`.

Multiplication copies the right matrix once per operation. Each task reads that stable snapshot and replaces one left row under its write lock. Arithmetic loops access local arrays directly instead of locking for every scalar access. Output assembly follows the batch barrier.

## Failure and shutdown

Ordinary task exceptions are delivered through futures, and workers can run subsequent tasks. `submit()` has no result handle; its failures are counted in the worker report. Fatal resource exhaustion is outside the reliability guarantee.

Shutdown first rejects new work, wakes blocked submitters, drains accepted work, asks workers to stop, and joins them. Workers poll their single-slot queues with a short timeout to observe stop requests without spin loops. There is no forced cancellation or deadline for an uncooperative task.

A caller interrupted while submitting stops accepting more work and drains its accepted tasks. A caller interrupted while awaiting results also drains the batch. Both preserve the interrupt flag and receive an exception. Shutdown completes cleanup before reporting interruption. Pool workers may not make nested blocking calls into their own executor.

## Performance experiment

The serial reference and engine use the same input and arithmetic order for multiplication. The reference avoids engine machinery; this exposes the real overhead of copying, validation, locks, handoff, and assembling output. Worker pools are created outside the timing window and reused. Three warmups precede measured rounds; order is shuffled; medians reduce sensitivity to occasional outliers.

This does not remove JIT, GC, thermal, frequency-scaling, or OS effects. A shared-host result is evidence about that run only. Next experiments could compare tiled multiplication or a standard executor under identical conditions, but those alternatives are not implemented here.
