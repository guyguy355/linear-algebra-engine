package spl.lae;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import parser.ComputationNode;
import parser.ComputationNodeType;

import static org.junit.jupiter.api.Assertions.*;

public class LinearAlgebraEngineTest {

    @Test
    @Timeout(5)
    void addMatricesSimpleCase() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(3);
        try {
            double[][] a = {{2.0, -1.0}, {0.5, 3.5}};
            double[][] b = {{4.0, 6.0}, {1.5, -0.5}};

            java.util.List<ComputationNode> children = new java.util.ArrayList<>();
            children.add(new ComputationNode(a));
            children.add(new ComputationNode(b));

            ComputationNode node =
                    new ComputationNode(ComputationNodeType.ADD, children);

            double[][] result = engine.run(node).getMatrix();

            assertArrayEquals(new double[]{6.0, 5.0}, result[0], 1e-9);
            assertArrayEquals(new double[]{2.0, 3.0}, result[1], 1e-9);
        } finally {
            shutdownEngine(engine);
        }
    }

    @Test
    @Timeout(5)
    void negateTurnsAllValuesNegative() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        try {
            double[][] data = {{-1.0, 4.0}, {2.0, -3.0}};

            java.util.List<ComputationNode> children = new java.util.ArrayList<>();
            children.add(new ComputationNode(data));

            ComputationNode node =
                    new ComputationNode(ComputationNodeType.NEGATE, children);

            double[][] result = engine.run(node).getMatrix();

            assertArrayEquals(new double[]{1.0, -4.0}, result[0], 1e-9);
            assertArrayEquals(new double[]{-2.0, 3.0}, result[1], 1e-9);
        } finally {
            shutdownEngine(engine);
        }
    }

    @Test
    @Timeout(5)
    void transposeOfSingleRowMatrix() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        try {
            double[][] original = {{5, 6, 7}};

            java.util.List<ComputationNode> children = new java.util.ArrayList<>();
            children.add(new ComputationNode(original));

            ComputationNode node =
                    new ComputationNode(ComputationNodeType.TRANSPOSE, children);

            double[][] result = engine.run(node).getMatrix();

            assertEquals(1, result.length);
            assertEquals(3, result[0].length);
            assertArrayEquals(new double[]{5.0, 6.0, 7.0}, result[0], 1e-9);
        } finally {
            shutdownEngine(engine);
        }
    }

    @Test
    @Timeout(5)
    void nestedMultiplyThenAdd() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(4);
        try {
            double[][] A = {{1, 1}, {2, 1}};
            double[][] B = {{3, 0}, {0, 2}};
            double[][] C = {{1, 1}, {1, 1}};

            java.util.List<ComputationNode> mulChildren = new java.util.ArrayList<>();
            mulChildren.add(new ComputationNode(A));
            mulChildren.add(new ComputationNode(B));

            ComputationNode mulNode =
                    new ComputationNode(ComputationNodeType.MULTIPLY, mulChildren);

            java.util.List<ComputationNode> addChildren = new java.util.ArrayList<>();
            addChildren.add(mulNode);
            addChildren.add(new ComputationNode(C));

            ComputationNode root =
                    new ComputationNode(ComputationNodeType.ADD, addChildren);

            double[][] result = engine.run(root).getMatrix();

            assertArrayEquals(new double[]{7.0, 5.0}, result[0], 1e-9);
            assertArrayEquals(new double[]{10.0, 7.0}, result[1], 1e-9);
        } finally {
            shutdownEngine(engine);
        }
    }

    @Test
    @Timeout(5)
    void loadAndComputeWithNullThrowsException() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> engine.loadAndCompute(null));
        } finally {
            shutdownEngine(engine);
        }
    }

    @Test
    @Timeout(5)
    void runWithNullRootThrowsException() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> engine.run(null));
        } finally {
            shutdownEngine(engine);
        }
    }

    @Test
    @Timeout(5)
    void addWithDifferentRowCountsThrows() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        try {
            double[][] a = {{1, 2}};
            double[][] b = {{3, 4}, {5, 6}};

            java.util.List<ComputationNode> children = new java.util.ArrayList<>();
            children.add(new ComputationNode(a));
            children.add(new ComputationNode(b));

            ComputationNode node =
                    new ComputationNode(ComputationNodeType.ADD, children);

            assertThrows(IllegalArgumentException.class,
                    () -> engine.run(node));
        } finally {
            shutdownEngine(engine);
        }
    }

    // סגירה מינימלית כדי לא להיתקע – בלי helpers חיצוניים
    private void shutdownEngine(LinearAlgebraEngine engine) {
        try {
            java.lang.reflect.Field f =
                    LinearAlgebraEngine.class.getDeclaredField("executor");
            f.setAccessible(true);
            scheduling.TiredExecutor ex =
                    (scheduling.TiredExecutor) f.get(engine);
            ex.shutdown();
        } catch (Exception ignored) {
        }
    }
}
