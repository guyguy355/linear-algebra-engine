package scheduling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TiredExecutorTest {

    @Test
    void ctorInvalidThreadsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TiredExecutor(0));
        assertThrows(IllegalArgumentException.class, () -> new TiredExecutor(-1));
    }

    @Test
    void submitNullThrows() throws InterruptedException {
        TiredExecutor ex = new TiredExecutor(1);
        try {
            assertThrows(IllegalArgumentException.class, () -> ex.submit(null));
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void submitRunsTask() throws InterruptedException {
        TiredExecutor ex = new TiredExecutor(1);

        final Object lock = new Object();
        final boolean[] done = {false};

        try {
            ex.submit(() -> {
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

            assertTrue(done[0]);
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void submitAllWaitsUntilFinished() throws InterruptedException {
        TiredExecutor ex = new TiredExecutor(2);

        final Object lock = new Object();
        final int[] count = {0};

        java.util.List<Runnable> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> {
                for (int j = 0; j < 100_000; j++) {
                    // simulate work
                }
                synchronized (lock) {
                    count[0]++;
                    lock.notifyAll();
                }
            });
        }

        try {
            ex.submitAll(tasks);

            synchronized (lock) {
                assertEquals(5, count[0]);
            }
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void shutdownWaitsForRunningTask() throws InterruptedException {
        TiredExecutor ex = new TiredExecutor(1);

        final Object lock = new Object();
        final boolean[] started = {false};
        final boolean[] finish = {false};

        ex.submit(() -> {
            synchronized (lock) {
                started[0] = true;
                lock.notifyAll();
                while (!finish[0]) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        });

        synchronized (lock) {
            if (!started[0]) {
                lock.wait(1000);
            }
        }

        assertTrue(started[0]);

        synchronized (lock) {
            finish[0] = true;
            lock.notifyAll();
        }

        ex.shutdown(); // should not hang
    }

    @Test
    void submitAllNullThrows() throws InterruptedException {
        TiredExecutor ex = new TiredExecutor(1);
        try {
            assertThrows(IllegalArgumentException.class, () -> ex.submitAll(null));
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void getWorkerReportContainsAllWorkers() throws InterruptedException {
        TiredExecutor ex = new TiredExecutor(3);
        try {
            String rep = ex.getWorkerReport();

            assertNotNull(rep);
            assertTrue(rep.contains("Worker 0"));
            assertTrue(rep.contains("Worker 1"));
            assertTrue(rep.contains("Worker 2"));
        } finally {
            ex.shutdown();
        }
    }
}
