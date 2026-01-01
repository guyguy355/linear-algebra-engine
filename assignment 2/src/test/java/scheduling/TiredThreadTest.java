package scheduling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TiredThreadTest {

    @Test
    void testBasicConstructor() {
        TiredThread t = new TiredThread(7, 1.25);

        assertEquals(7, t.getWorkerId(), "Worker id should be initialized correctly.");
        assertFalse(t.isBusy(), "New thread should not be busy.");
        assertEquals(0.0, t.getFatigue(), "Initial fatigue should be zero.");
    }

    @Test
    void testNewTaskWhenQueueFullThrows() {
        TiredThread t = new TiredThread(1, 1.0);
        // Thread is not started, so the task queue is never consumed

        t.newTask(() -> {});

        assertThrows(IllegalStateException.class,
                () -> t.newTask(() -> {}),
                "Adding a second task while queue is full should throw IllegalStateException.");
    }

    @Test
    void testRunExecutesTaskAndBecomesIdle() throws InterruptedException {
        TiredThread t = new TiredThread(2, 1.0);
        t.start();

        final Object lock = new Object();
        final boolean[] done = {false};

        t.newTask(() -> {
            synchronized (lock) {
                done[0] = true;
                lock.notifyAll();
            }
        });

        synchronized (lock) {
            if (!done[0]) {
                lock.wait(1000);
            }
        }

        assertTrue(done[0], "Task should be executed by the worker.");

        for (int i = 0; i < 200 && t.isBusy(); i++) {
            Thread.yield();
        }

        assertFalse(t.isBusy(), "Worker should not be busy after finishing task.");

        t.shutdown();
        t.join(1000);
        assertFalse(t.isAlive(), "Thread should terminate after shutdown.");
    }

    @Test
    void testTimeUsedIncreasesAfterTask() throws InterruptedException {
        TiredThread t = new TiredThread(3, 1.0);
        t.start();

        final Object lock = new Object();
        final boolean[] done = {false};

        long before = t.getTimeUsed();

        t.newTask(() -> {
            long sum = 0;
            for (int i = 0; i < 200_000; i++) {
                sum += i;
            }
            synchronized (lock) {
                done[0] = true;
                lock.notifyAll();
            }
        });

        synchronized (lock) {
            if (!done[0]) {
                lock.wait(2000);
            }
        }

        assertTrue(done[0], "Task should complete execution.");

        long after = t.getTimeUsed();
        assertTrue(after > before, "Time used should increase after executing a task.");

        t.shutdown();
        t.join(1000);
    }

    @Test
    void testCompareToStableCases() {
        TiredThread a = new TiredThread(10, 1.0);
        TiredThread b = new TiredThread(11, 1.0);

        assertEquals(0, a.compareTo(a), "compareTo with itself should return 0.");
        assertEquals(0, a.compareTo(b), "compareTo should be stable when no work was done.");
        assertEquals(0, b.compareTo(a), "compareTo should be symmetric in stable state.");
    }

    @Test
    void testShutdownWithoutTasksStopsThread() throws InterruptedException {
        TiredThread t = new TiredThread(5, 1.0);
        t.start();

        t.shutdown();
        t.join(1000);

        assertFalse(t.isAlive(), "Thread should stop after shutdown even if no tasks were submitted.");
    }
}

