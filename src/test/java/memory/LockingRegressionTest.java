package memory;
import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@Timeout(5)
class LockingRegressionTest {
 @Test void reciprocalAddsDoNotDeadlock() throws Exception {
  var a=new SharedVector(new double[]{1},VectorOrientation.ROW_MAJOR);
  var b=new SharedVector(new double[]{2},VectorOrientation.ROW_MAJOR);
  var pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
  try {
   Future<?> x=pool.submit(()->{try{start.await();a.add(b);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
   Future<?> y=pool.submit(()->{try{start.await();b.add(a);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
   start.countDown();x.get(2,TimeUnit.SECONDS);y.get(2,TimeUnit.SECONDS);
   assertTrue((a.get(0)==3 && b.get(0)==5)||(a.get(0)==4 && b.get(0)==3));
  }finally{start.countDown();pool.shutdownNow();}
 }
 @Test void invalidMultiplyReleasesLock() {
  var a=new SharedVector(new double[]{1,2},VectorOrientation.ROW_MAJOR);
  assertThrows(IllegalArgumentException.class,()->a.multiplyByRows(new double[][]{{1}}));
  a.negate();assertEquals(-1,a.get(0));
 }
}
