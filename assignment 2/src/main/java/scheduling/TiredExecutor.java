package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        if (numThreads <= 0) {
            throw new IllegalArgumentException("numThreads must be positive");
        }

        workers = new TiredThread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            double fatigue = 0.5 + Math.random();

            workers[i] = new TiredThread(i, fatigue);
            idleMinHeap.add(workers[i]);
        }

        for (int i = 0; i < numThreads; i++) {
            workers[i].start();
        }
    }

    public void submit(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task cannot be null");
        }

        TiredThread worker;

        synchronized (idleMinHeap) {
            // wait until there is free worker
            while (idleMinHeap.isEmpty()) {
                try {
                    idleMinHeap.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // take worker with least fatigue
            worker = idleMinHeap.poll();
            inFlight.incrementAndGet();
        }
        final TiredThread assignedWorker = worker;

        try {
            // give task to worker
            assignedWorker.newTask(() -> {
                try {
                    task.run();
                } finally {
                    // task finished, return worker back
                    synchronized (idleMinHeap) {
                        inFlight.decrementAndGet();
                        idleMinHeap.add(assignedWorker);
                        idleMinHeap.notifyAll(); // wake waiting threads
                    }
                }
            });
        } catch (RuntimeException ex) {
            // something failed, fix counters and heap
            synchronized (idleMinHeap) {
                inFlight.decrementAndGet();
                idleMinHeap.add(assignedWorker);
                idleMinHeap.notifyAll();
            }
            throw ex;
        }
    }

    public void submitAll(Iterable<Runnable> tasks) {
        if (tasks == null) {
            throw new IllegalArgumentException("tasks cannot be null");
        }

        // push everything to the executor
        for (Runnable r : tasks) {
            submit(r);
        }

        // block until there are no running tasks
        synchronized (idleMinHeap) {
            while (inFlight.get() != 0) {
                try {
                    idleMinHeap.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        // wait until there are no running tasks
        synchronized (idleMinHeap) {
            while (inFlight.get() > 0) {
                idleMinHeap.wait();
            }
        }

        // ask all workers to stop working
        for (TiredThread t : workers) {
            t.shutdown();
        }

        // wait for all threads to terminate
        for (TiredThread t : workers) {
            t.join();
        }
    }

    public synchronized String getWorkerReport() {
        StringBuilder report = new StringBuilder();

        // go over all workers and build report string
        for (TiredThread worker : workers) {
            report.append("Worker ")
                    .append(worker.getWorkerId())
                    .append(" | busy=")
                    .append(worker.isBusy())
                    .append(" | timeUsed(ns)=")
                    .append(worker.getTimeUsed())
                    .append(" | timeIdle(ns)=")
                    .append(worker.getTimeIdle())
                    .append(" | fatigue=")
                    .append(worker.getFatigue())
                    .append('\n');
        }

        return report.toString();
    }
}
