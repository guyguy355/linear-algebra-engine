package scheduling;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(8)
class ExecutorRegressionTest {
    @Test void failurePropagatesAndNextBatchRuns() {
        try (var executor = new TiredExecutor(1)) {
            AtomicInteger count = new AtomicInteger();
            CompletionException error = assertThrows(CompletionException.class, () -> executor.submitAll(List.of(
                () -> {throw new IllegalArgumentException("expected");}, count::incrementAndGet)));
            assertInstanceOf(IllegalArgumentException.class, error.getCause()); assertEquals(1,count.get());
            executor.submitAll(List.of(count::incrementAndGet)); assertEquals(2,count.get());
            assertTrue(executor.getWorkerReport().contains("tasks=3"));
        }
    }
    @Test void shutdownRejectsAllNewWorkAndIsIdempotent() throws Exception {
        var executor = new TiredExecutor(1); executor.shutdown(); executor.shutdown();
        assertThrows(RejectedExecutionException.class, () -> executor.submit(() -> {}));
        assertThrows(RejectedExecutionException.class, () -> executor.submitAll(List.of()));
    }
    @Test void nullBatchMemberRejectedBeforeAnySubmission() {
        try(var executor = new TiredExecutor(1)) {
            AtomicInteger count=new AtomicInteger();
            assertThrows(IllegalArgumentException.class, () -> executor.submitAll(Arrays.asList(count::incrementAndGet,null)));
            assertEquals(0,count.get());
        }
    }
    @Test void interruptedCallerDrainsAcceptedWork() throws Exception {
        var executor = new TiredExecutor(1);
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(); AtomicReference<Throwable> failure=new AtomicReference<>();
        Thread caller=new Thread(() -> {
            try { executor.submitAll(List.of(() -> {started.countDown(); try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}}, () -> {})); }
            catch(Throwable e){ failure.set(e); interrupted.set(Thread.currentThread().isInterrupted()); }
        });
        try {
            caller.start(); assertTrue(started.await(2,TimeUnit.SECONDS)); caller.interrupt(); release.countDown(); caller.join(2000);
            assertFalse(caller.isAlive()); assertTrue(interrupted.get()); assertInstanceOf(RejectedExecutionException.class,failure.get());
            executor.submitAll(List.of(() -> {}));
        } finally {release.countDown(); caller.join(2000); executor.close();}
    }
    @Test void nestedBlockingSubmissionRejectedInsteadOfDeadlocking() {
        try(var executor = new TiredExecutor(1)) {
            CompletionException error=assertThrows(CompletionException.class, () -> executor.submitAll(List.of(() -> executor.submitAll(List.of(() -> {})))));
            assertInstanceOf(RejectedExecutionException.class,error.getCause());
        }
    }
    @Test void concurrentCallersCompleteTheirBatches() throws Exception {
        try(var executor=new TiredExecutor(3)) {
            var callers=Executors.newFixedThreadPool(2); AtomicInteger count=new AtomicInteger();
            try {
                Callable<Void> batch=()->{List<Runnable> tasks=new ArrayList<>();for(int i=0;i<50;i++)tasks.add(count::incrementAndGet);executor.submitAll(tasks);return null;};
                Future<Void> a=callers.submit(batch),b=callers.submit(batch);a.get(3,TimeUnit.SECONDS);b.get(3,TimeUnit.SECONDS);
                assertEquals(100,count.get());
            }finally{callers.shutdownNow();}
        }
    }
}
