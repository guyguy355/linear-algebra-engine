package scheduling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TiredExecutorTest {

    @Test
    void testConstructorInvalidThreadsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new TiredExecutor(0),
                "Constructor should throw when numThreads is 0.");

        assertThrows(IllegalArgumentException.class,
                () -> new TiredExecutor(-1),
                "Constructor should throw when numThreads is negative.");
    }

    @Test
    void testSubmitNullThrows() throws InterruptedException {
        TiredExecutor ex = new TiredExecutor(1);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> ex.submit(null),
                    "Submitting a null task should throw IllegalArgumentException.");
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void testSubmitRunsTask() throws InterruptedException {
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

            assertTrue(done[0], "Submitted task should run and set done=true.");
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void testSubmitAllWaitsUntilFinished() throws InterruptedException {
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

            // submitAll should return only after all tasks are done
            synchronized (lock) {
                assertEquals(5, count[0], "submitAll should finish only after all 5 tasks are executed.");
            }
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void testShutdownWaitsForRunningTask() throws InterruptedException {
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

        assertTrue(started[0], "Task should start running before shutdown is called.");

        synchronized (lock) {
            finish[0] = true;
            lock.notifyAll();
        }

        // should not hang
        ex.shutdown();
    }

    @Test
    void testSubmitAllNullThrows() throws InterruptedException {
        TiredExecutor ex = new TiredExecutor(1);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> ex.submitAll(null),
                    "Submitting a null tasks collection should throw IllegalArgumentException.");
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void testGetWorkerReportContainsAllWorkers() throws InterruptedException {
        TiredExecutor ex = new TiredExecutor(3);
        try {
            String rep = ex.getWorkerReport();

            assertNotNull(rep, "Worker report should not be null.");
            assertTrue(rep.contains("Worker 0"), "Report should include Worker 0.");
            assertTrue(rep.contains("Worker 1"), "Report should include Worker 1.");
            assertTrue(rep.contains("Worker 2"), "Report should include Worker 2.");
        } finally {
            ex.shutdown();
        }
    }
}
