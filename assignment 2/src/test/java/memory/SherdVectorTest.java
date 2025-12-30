package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SherdVectorTest {

    @Test
    void ctorKeepsLengthAndOrientation() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);

        assertEquals(3, v.length());
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    void getReturnsValues() {
        SharedVector v = new SharedVector(new double[]{10, -2, 5.5}, VectorOrientation.ROW_MAJOR);

        assertEquals(10.0, v.get(0), 1e-9);
        assertEquals(-2.0, v.get(1), 1e-9);
        assertEquals(5.5, v.get(2), 1e-9);
    }

    @Test
    void getOutOfRangeThrows() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);

        assertThrows(IndexOutOfBoundsException.class, () -> v.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> v.get(3));
    }

    @Test
    void transposeTogglesOrientation() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());

        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    void addAddsElementwise() {
        SharedVector a = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{10, 20, 30}, VectorOrientation.ROW_MAJOR);

        a.add(b);

        assertEquals(11.0, a.get(0), 1e-9);
        assertEquals(22.0, a.get(1), 1e-9);
        assertEquals(33.0, a.get(2), 1e-9);
    }

    @Test
    void addLengthMismatchThrows() {
        SharedVector a = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> a.add(b));
    }

    @Test
    void negateFlipsSigns() {
        SharedVector v = new SharedVector(new double[]{1, -2, 0, 5}, VectorOrientation.ROW_MAJOR);

        v.negate();

        assertEquals(-1.0, v.get(0), 1e-9);
        assertEquals(2.0, v.get(1), 1e-9);
        assertEquals(0.0, v.get(2), 1e-9);
        assertEquals(-5.0, v.get(3), 1e-9);
    }

    @Test
    void dotComputesInnerProduct() {
        SharedVector a = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.ROW_MAJOR);

        assertEquals(32.0, a.dot(b), 1e-9);
    }

    @Test
    void dotLengthMismatchThrows() {
        SharedVector a = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> a.dot(b));
    }

    @Test
    void lockMethodsDoNotThrow() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        assertDoesNotThrow(() -> v.readLock());
        assertDoesNotThrow(() -> v.readUnlock());

        assertDoesNotThrow(() -> v.writeLock());
        assertDoesNotThrow(() -> v.writeUnlock());
    }

    @Test
    void vecMatMulNullThrows() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(null));
    }

    @Test
    void vecMatMulEmptyMatrixThrows() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix();

        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    @Test
    void vecMatMulDimensionMismatchThrows() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);

        SharedMatrix m = new SharedMatrix(new double[][]{
                {1, 2},
                {3, 4}
        });

        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    @Test
    void vecMatMulBasic2x2CurrentBehavior() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        SharedMatrix m = new SharedMatrix(new double[][]{
                {3, 4},
                {5, 6}
        });

        v.vecMatMul(m);

        assertEquals(2, v.length());
        assertEquals(9.0, v.get(0), 1e-9);
        assertEquals(18.0, v.get(1), 1e-9);
    }

    @Test
    void vecMatMulResultLengthChanges() {
        SharedVector a = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        SharedMatrix m = new SharedMatrix(new double[][]{
                {1, 2, 3},
                {4, 5, 6}
        });

        assertThrows(IllegalArgumentException.class, () -> a.vecMatMul(m));

        SharedVector b = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        b.vecMatMul(m);

        assertEquals(2, b.length());
    }
}





