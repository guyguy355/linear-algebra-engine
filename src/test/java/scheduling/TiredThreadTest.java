package scheduling;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(5)
class TiredThreadTest {
    @Test void basicConstructor() {
        TiredThread t = new TiredThread(7, 1.25);
        assertEquals(7, t.getWorkerId()); assertFalse(t.isBusy()); assertEquals(0, t.getFatigue());
    }
    @Test void fullHandoffRejects() {
        TiredThread t = new TiredThread(1, 1);
        t.newTask(() -> {});
        assertThrows(IllegalStateException.class, () -> t.newTask(() -> {}));
    }
    @Test void completionPublishesMetricsAndIdleState() throws Exception {
        TiredThread t = new TiredThread(2, 1); t.start();
        CompletableFuture<Void> done = new CompletableFuture<>();
        try {
            t.newTask(() -> {}, failure -> done.complete(null));
            done.get(2, TimeUnit.SECONDS);
            assertFalse(t.isBusy()); assertEquals(1, t.getCompletedTasks()); assertTrue(t.getTimeUsed() > 0);
        } finally { t.shutdown(); t.join(2000); }
        assertFalse(t.isAlive());
    }
    @Test void taskFailureIsReportedAndWorkerSurvives() throws Exception {
        TiredThread t = new TiredThread(3, 1); t.start();
        CompletableFuture<Throwable> failed = new CompletableFuture<>();
        CompletableFuture<Void> done = new CompletableFuture<>();
        try {
            t.newTask(() -> { throw new IllegalArgumentException("bad task"); }, failed::complete);
            assertInstanceOf(IllegalArgumentException.class, failed.get(2, TimeUnit.SECONDS));
            t.newTask(() -> {}, failure -> done.complete(null)); done.get(2, TimeUnit.SECONDS);
            assertEquals(2, t.getCompletedTasks()); assertEquals(1, t.getFailedTasks());
        } finally { t.shutdown(); t.join(2000); }
    }
    @Test void compareStableCases() {
        TiredThread a = new TiredThread(10, 1), b = new TiredThread(11, 1);
        assertEquals(0, a.compareTo(a)); assertEquals(0, a.compareTo(b));
    }
    @Test void shutdownStopsAndRejects() throws Exception {
        TiredThread t = new TiredThread(5, 1); t.start(); t.shutdown(); t.join(2000);
        assertFalse(t.isAlive()); assertThrows(IllegalStateException.class, () -> t.newTask(() -> {}));
    }
}
