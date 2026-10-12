package scheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Single-slot worker. Completion is published only after accounting is updated. */
public class TiredThread extends Thread implements Comparable<TiredThread> {
    private record Work(Runnable task, Consumer<Throwable> completion) {}
    private final int id;
    private final double fatigueFactor;
    private final ArrayBlockingQueue<Work> handoff = new ArrayBlockingQueue<>(1);
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong timeUsed = new AtomicLong(), timeIdle = new AtomicLong();
    private final AtomicLong completed = new AtomicLong(), failed = new AtomicLong();
    private volatile boolean accepting = true;

    public TiredThread(int id, double fatigueFactor) {
        if (!Double.isFinite(fatigueFactor) || fatigueFactor <= 0)
            throw new IllegalArgumentException("fatigueFactor must be finite and positive");
        this.id = id;
        this.fatigueFactor = fatigueFactor;
        setName("matrix-worker-" + id);
    }
    public int getWorkerId() { return id; }
    public double getFatigue() { return fatigueFactor * timeUsed.get(); }
    public boolean isBusy() { return busy.get(); }
    public long getTimeUsed() { return timeUsed.get(); }
    public long getTimeIdle() { return timeIdle.get(); }
    public long getCompletedTasks() { return completed.get(); }
    public long getFailedTasks() { return failed.get(); }

    public void newTask(Runnable task) { newTask(task, error -> {}); }
    synchronized void newTask(Runnable task, Consumer<Throwable> completion) {
        if (task == null) throw new IllegalArgumentException("task cannot be null");
        if (!accepting) throw new IllegalStateException("Worker is shut down");
        if (!handoff.offer(new Work(task, completion)))
            throw new IllegalStateException("Worker handoff is full");
    }
    /** Drains the accepted handoff before stopping; no busy-wait or task cancellation. */
    public synchronized void shutdown() { accepting = false; }

    @Override public void run() {
        while (accepting || !handoff.isEmpty()) {
            long idleStart = System.nanoTime();
            Work work;
            try { work = handoff.poll(100, TimeUnit.MILLISECONDS); }
            catch (InterruptedException e) { accepting = false; continue; }
            timeIdle.addAndGet(System.nanoTime() - idleStart);
            if (work == null) continue;
            busy.set(true);
            long start = System.nanoTime();
            Throwable failure = null;
            try { work.task.run(); }
            catch (Throwable e) { failure = e; failed.incrementAndGet(); }
            finally {
                timeUsed.addAndGet(System.nanoTime() - start);
                completed.incrementAndGet();
                busy.set(false);
            }
            work.completion.accept(failure);
        }
    }
    @Override public int compareTo(TiredThread other) {
        return Double.compare(getFatigue(), other.getFatigue());
    }
}
