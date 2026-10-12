package spl.lae;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import parser.*;
import demo.MatrixRace;
import java.util.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class EngineRegressionTest {
    @TempDir Path temp;
    private ComputationNode scalar(double v) { return new ComputationNode(new double[][]{{v}}); }
    @Test void nestedNaryAdditionPreservesAllOperands() {
        try (var e = new LinearAlgebraEngine(2)) {
            var sum = new ComputationNode("+", List.of(scalar(1), scalar(2), scalar(3)));
            assertEquals(-6, e.run(new ComputationNode("-", List.of(sum))).getMatrix()[0][0]);
        }
    }
    @Test void immutableOperandsAreNotMutated() {
        var children = List.of(scalar(1), scalar(2), scalar(3));
        try (var e = new LinearAlgebraEngine(1)) {
            assertEquals(6, e.run(new ComputationNode("+", children)).getMatrix()[0][0]);
            assertEquals(3, children.size());
        }
    }
    @Test void nestedMultiplicationPreservesOrder() {
        try (var e = new LinearAlgebraEngine(2)) {
            var product = new ComputationNode("*", List.of(scalar(2), scalar(3), scalar(4)));
            assertEquals(25, e.run(new ComputationNode("+", List.of(scalar(1), product))).getMatrix()[0][0]);
        }
    }
    @Test void mismatchedRowsRejectedAndEngineReusable() {
        try (var e = new LinearAlgebraEngine(2)) {
            var root = new ComputationNode("+", List.of(new ComputationNode(new double[][]{{1},{2}}), scalar(3)));
            assertThrows(IllegalArgumentException.class, () -> e.run(root));
            assertEquals(ComputationNodeType.ADD, root.getNodeType());
            assertEquals(7, e.run(new ComputationNode("+", List.of(scalar(3),scalar(4)))).getMatrix()[0][0]);
        }
    }
    @Test void invalidArityRejected() {
        try (var e = new LinearAlgebraEngine(1)) {
            assertThrows(IllegalArgumentException.class, () -> e.run(new ComputationNode("+", List.of(scalar(1)))));
            assertThrows(IllegalArgumentException.class, () -> e.run(new ComputationNode("T", List.of(scalar(1),scalar(2)))));
        }
    }
    @Test void raggedAndEmptyMatricesRejected() {
        try (var e = new LinearAlgebraEngine(1)) {
            assertThrows(IllegalArgumentException.class, () -> e.run(new ComputationNode(new double[][]{{1},{2,3}})));
            assertThrows(IllegalArgumentException.class, () -> e.run(new ComputationNode(new double[0][0])));
        }
    }
    @Test void nonNumericJsonRejected() throws Exception {
        Path file = temp.resolve("bad.json"); Files.writeString(file, "[[1, \"oops\"]]");
        assertThrows(java.text.ParseException.class, () -> new InputParser().parse(file.toString()));
    }
    @Test void wrongJsonOperandTypeRejected() throws Exception {
        Path file = temp.resolve("bad.json"); Files.writeString(file, "{\"operator\":\"+\",\"operands\":3}");
        assertThrows(java.text.ParseException.class, () -> new InputParser().parse(file.toString()));
    }
    @Test void rectangularMultiplicationAndTranspose() {
        try (var e = new LinearAlgebraEngine(3)) {
            var product = new ComputationNode("*", List.of(new ComputationNode(new double[][]{{1,2,3},{4,5,6}}),new ComputationNode(new double[][]{{7,8},{9,10},{11,12}})));
            double[][] actual = e.run(new ComputationNode("T", List.of(product))).getMatrix();
            assertArrayEquals(new double[]{58,139}, actual[0]); assertArrayEquals(new double[]{64,154}, actual[1]);
        }
    }
    @Test void randomRectangularProductsMatchReference() {
        Random random = new Random(77);
        try (var e = new LinearAlgebraEngine(3)) {
            for (int t = 0; t < 12; t++) {
                int m=1+random.nextInt(6),n=1+random.nextInt(6),k=1+random.nextInt(6);
                double[][] a=new double[m][k],b=new double[k][n];
                for (double[] row:a) for(int j=0;j<row.length;j++) row[j]=random.nextInt(11)-5;
                for (double[] row:b) for(int j=0;j<row.length;j++) row[j]=random.nextInt(11)-5;
                double[][] actual=e.run(new ComputationNode("*",List.of(new ComputationNode(a),new ComputationNode(b)))).getMatrix();
                assertEquals(0, MatrixRace.maxError(MatrixRace.multiplySerial(a,b),actual));
            }
        }
    }
}
