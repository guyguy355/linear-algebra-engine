package scheduling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TiredThreadTest {

    @Test
    void ctorBasic() {
        TiredThread t = new TiredThread(7, 1.25);

        assertEquals(7, t.getWorkerId());
        assertFalse(t.isBusy());
        assertEquals(0.0, t.getFatigue(), 0.0);
    }

    @Test
    void newTaskWhenQueueFullThrows() {
        TiredThread t = new TiredThread(1, 1.0);
        // לא מפעילים start, אז אף אחד לא "אוכל" מהתור

        t.newTask(() -> { });
        assertThrows(IllegalStateException.class, () -> t.newTask(() -> { }));
    }

    @Test
    void runExecutesTaskAndStops() throws InterruptedException {
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

        assertTrue(done[0]);

        for (int i = 0; i < 200 && t.isBusy(); i++) {
            Thread.yield();
        }
        assertFalse(t.isBusy());

        t.shutdown();
        t.join(1000);
        assertFalse(t.isAlive());
    }

    @Test
    void timeUsedGoesUpAfterTask() throws InterruptedException {
        TiredThread t = new TiredThread(3, 1.0);
        t.start();

        final Object lock = new Object();
        final boolean[] done = {false};

        long before = t.getTimeUsed();

        t.newTask(() -> {
            long x = 0;
            for (int i = 0; i < 200_000; i++) {
                x += i;
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

        assertTrue(done[0]);

        long after = t.getTimeUsed();
        assertTrue(after >= before);
        assertTrue(after > before);

        t.shutdown();
        t.join(1000);
    }

    @Test
    void compareToStableCases() {
        TiredThread a = new TiredThread(10, 1.0);
        TiredThread b = new TiredThread(11, 1.0);

        assertEquals(0, a.compareTo(a));
        assertEquals(0, a.compareTo(b));
        assertEquals(0, b.compareTo(a));
    }

    @Test
    void shutdownWithoutTasksStops() throws InterruptedException {
        TiredThread t = new TiredThread(5, 1.0);
        t.start();

        t.shutdown();
        t.join(1000);

        assertFalse(t.isAlive());
    }
}
