package demo;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
@Timeout(10)
class MatrixRaceTest {
 @TempDir Path out;
 @Test void reportWrittenAndVerified() throws Exception {
  MatrixRace.main(new String[]{"8","2","2",out.toString(),"42"});
  assertTrue(Files.readString(out.resolve("matrix-race.csv")).contains("engine,2,"));
  assertTrue(Files.readString(out.resolve("matrix-race.html")).contains("Matrix Race"));
  assertTrue(Files.readString(out.resolve("matrix-race.json")).contains("maxError"));
 }
 @Test void badResultFailsVerification() {
  assertThrows(IllegalStateException.class,()->MatrixRace.maxError(new double[][]{{1}},new double[][]{{2}}));
 }
 @Test void invalidArgumentsRejected() {
  assertThrows(IllegalArgumentException.class,()->MatrixRace.run(0,2,2,42));
 }
}
