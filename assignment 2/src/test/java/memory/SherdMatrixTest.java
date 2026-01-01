package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SherdMatrixTest {

    @Test
    void testDefaultConstructor() {
        SharedMatrix m = new SharedMatrix();

        assertEquals(0, m.length());

        double[][] out = m.readRowMajor();
        assertNotNull(out);
        assertEquals(0, out.length);
    }

    @Test
    void testConstructorFromArray() {
        double[][] a = {
                {1, 2, 3},
                {4, 5, 6}
        };

        SharedMatrix m = new SharedMatrix(a);

        assertEquals(2, m.length());
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());

        double[][] out = m.readRowMajor();
        assertArrayEquals(new double[]{1, 2, 3}, out[0], 1e-9);
        assertArrayEquals(new double[]{4, 5, 6}, out[1], 1e-9);
    }

    @Test
    void testConstructorDeepCopy() {
        double[][] a = {
                {1, 2},
                {3, 4}
        };

        SharedMatrix m = new SharedMatrix(a);
        a[0][0] = 99;

        double[][] out = m.readRowMajor();
        assertArrayEquals(new double[]{1, 2}, out[0], 1e-9);
        assertArrayEquals(new double[]{3, 4}, out[1], 1e-9);
    }

    @Test
    void testConstructorNullMatrix() {
        SharedMatrix m = new SharedMatrix(null);

        assertEquals(0, m.length());

        double[][] out = m.readRowMajor();
        assertEquals(0, out.length);
    }

    @Test
    void testLoadRowMajor() {
        SharedMatrix m = new SharedMatrix();

        double[][] a = {
                {7, 8},
                {9, 10}
        };

        m.loadRowMajor(a);

        assertEquals(2, m.length());
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());

        double[][] out = m.readRowMajor();
        assertArrayEquals(new double[]{7, 8}, out[0], 1e-9);
        assertArrayEquals(new double[]{9, 10}, out[1], 1e-9);
    }

    @Test
    void testLoadRowMajorDeepCopy() {
        SharedMatrix m = new SharedMatrix();

        double[][] a = {
                {1, 2},
                {3, 4}
        };

        m.loadRowMajor(a);
        a[1][0] = 100;

        double[][] out = m.readRowMajor();
        assertArrayEquals(new double[]{1, 2}, out[0], 1e-9);
        assertArrayEquals(new double[]{3, 4}, out[1], 1e-9);
    }

    @Test
    void testLoadRowMajorInvalid() {
        SharedMatrix m = new SharedMatrix();

        assertThrows(NullPointerException.class, () -> m.loadRowMajor(null));

        double[][] bad = {
                {1, 2},
                {3}
        };

        m.loadRowMajor(bad);
        assertEquals(2, m.length());
    }


    @Test
    void testLoadColumnMajor() {
        SharedMatrix m = new SharedMatrix();

        double[][] a = {
                {1, 2, 3},
                {4, 5, 6}
        };

        m.loadColumnMajor(a);

        assertEquals(3, m.length());
        assertEquals(VectorOrientation.COLUMN_MAJOR, m.getOrientation());

        double[][] out = m.readRowMajor();
        assertArrayEquals(new double[]{1, 2, 3}, out[0], 1e-9);
        assertArrayEquals(new double[]{4, 5, 6}, out[1], 1e-9);
    }

    @Test
    void testLoadColumnMajorDeepCopy() {
        SharedMatrix m = new SharedMatrix();

        double[][] a = {
                {1, 2},
                {3, 4},
                {5, 6}
        };

        m.loadColumnMajor(a);
        a[0][0] = -1;

        double[][] out = m.readRowMajor();
        assertArrayEquals(new double[]{1, 2}, out[0], 1e-9);
        assertArrayEquals(new double[]{3, 4}, out[1], 1e-9);
        assertArrayEquals(new double[]{5, 6}, out[2], 1e-9);
    }

    @Test
    void testReadRowMajorSameResult() {
        double[][] a = {
                {2, 4, 6},
                {1, 3, 5}
        };

        SharedMatrix row = new SharedMatrix(a);

        SharedMatrix col = new SharedMatrix();
        col.loadColumnMajor(a);

        double[][] r1 = row.readRowMajor();
        double[][] r2 = col.readRowMajor();

        assertArrayEquals(r1[0], r2[0], 1e-9);
        assertArrayEquals(r1[1], r2[1], 1e-9);
    }

    @Test
    void testGetOutOfBounds() {
        SharedMatrix m = new SharedMatrix(new double[][]{
                {1, 2},
                {3, 4}
        });

        assertNotNull(m.get(0));
        assertThrows(RuntimeException.class, () -> m.get(-1));
        assertThrows(RuntimeException.class, () -> m.get(2));
    }

    @Test
    void testLengthDependsOnOrientation() {
        double[][] a = {
                {1, 2, 3},
                {4, 5, 6}
        };

        SharedMatrix row = new SharedMatrix(a);
        assertEquals(2, row.length());

        SharedMatrix col = new SharedMatrix();
        col.loadColumnMajor(a);
        assertEquals(3, col.length());
    }
}

