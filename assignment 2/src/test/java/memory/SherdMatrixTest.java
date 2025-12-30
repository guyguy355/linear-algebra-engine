package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SherdMatrixTest {

    @Test
    void defaultCtorCreatesEmptyMatrix() {
        SharedMatrix m = new SharedMatrix();

        assertEquals(0, m.length());
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());

        double[][] a = m.readRowMajor();
        assertNotNull(a);
        assertEquals(0, a.length);
    }

    @Test
    void ctorFromArrayKeepsValues() {
        double[][] src = {
                {1, 2, 3},
                {4, 5, 6}
        };

        SharedMatrix m = new SharedMatrix(src);

        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
        assertEquals(2, m.length());

        double[][] out = m.readRowMajor();
        assertEquals(2, out.length);
        assertArrayEquals(new double[]{1, 2, 3}, out[0], 1e-9);
        assertArrayEquals(new double[]{4, 5, 6}, out[1], 1e-9);
    }

    @Test
    void ctorMakesDeepCopy() {
        double[][] src = {
                {1, 2},
                {3, 4}
        };

        SharedMatrix m = new SharedMatrix(src);

        src[0][0] = 100;
        src[1][1] = -5;

        double[][] out = m.readRowMajor();
        assertArrayEquals(new double[]{1, 2}, out[0], 1e-9);
        assertArrayEquals(new double[]{3, 4}, out[1], 1e-9);
    }

    @Test
    void ctorNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(null));
    }

    @Test
    void ctorWithNullRowThrows() {
        double[][] bad = new double[2][];
        bad[0] = new double[]{1, 2};
        bad[1] = null;

        assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(bad));
    }

    @Test
    void ctorNotRectangularThrows() {
        double[][] bad = {
                {1, 2, 3},
                {4, 5}
        };

        assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(bad));
    }

    @Test
    void loadRowMajorLoadsMatrix() {
        SharedMatrix m = new SharedMatrix();

        double[][] a = {
                {7, 8},
                {9, 10}
        };

        m.loadRowMajor(a);

        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
        assertEquals(2, m.length());

        double[][] out = m.readRowMajor();
        assertArrayEquals(new double[]{7, 8}, out[0], 1e-9);
        assertArrayEquals(new double[]{9, 10}, out[1], 1e-9);
    }

    @Test
    void loadRowMajorMakesDeepCopy() {
        SharedMatrix m = new SharedMatrix();

        double[][] a = {
                {1, 2},
                {3, 4}
        };

        m.loadRowMajor(a);
        a[0][1] = 99;

        double[][] out = m.readRowMajor();
        assertArrayEquals(new double[]{1, 2}, out[0], 1e-9);
        assertArrayEquals(new double[]{3, 4}, out[1], 1e-9);
    }

    @Test
    void loadRowMajorNullThrows() {
        SharedMatrix m = new SharedMatrix();
        assertThrows(IllegalArgumentException.class, () -> m.loadRowMajor(null));
    }

    @Test
    void loadRowMajorNotRectangularThrows() {
        SharedMatrix m = new SharedMatrix();

        double[][] bad = {
                {1, 2, 3},
                {4, 5}
        };

        assertThrows(IllegalArgumentException.class, () -> m.loadRowMajor(bad));
    }

    @Test
    void loadColumnMajorLoadsMatrix() {
        SharedMatrix m = new SharedMatrix();

        double[][] a = {
                {1, 2, 3},
                {4, 5, 6}
        };

        m.loadColumnMajor(a);

        assertEquals(VectorOrientation.COLUMN_MAJOR, m.getOrientation());
        assertEquals(3, m.length());

        double[][] out = m.readRowMajor();
        assertArrayEquals(new double[]{1, 2, 3}, out[0], 1e-9);
        assertArrayEquals(new double[]{4, 5, 6}, out[1], 1e-9);
    }

    @Test
    void loadColumnMajorMakesDeepCopy() {
        SharedMatrix m = new SharedMatrix();

        double[][] a = {
                {1, 2},
                {3, 4},
                {5, 6}
        };

        m.loadColumnMajor(a);
        a[2][0] = 999;

        double[][] out = m.readRowMajor();
        assertArrayEquals(new double[]{1, 2}, out[0], 1e-9);
        assertArrayEquals(new double[]{3, 4}, out[1], 1e-9);
        assertArrayEquals(new double[]{5, 6}, out[2], 1e-9);
    }

    @Test
    void loadColumnMajorNullThrows() {
        SharedMatrix m = new SharedMatrix();
        assertThrows(IllegalArgumentException.class, () -> m.loadColumnMajor(null));
    }

    @Test
    void readRowMajorSameForRowAndColumn() {
        double[][] a = {
                {2, 4, 6},
                {1, 3, 5}
        };

        SharedMatrix row = new SharedMatrix(a);

        SharedMatrix col = new SharedMatrix();
        col.loadColumnMajor(a);

        double[][] outRow = row.readRowMajor();
        double[][] outCol = col.readRowMajor();

        assertArrayEquals(outRow[0], outCol[0], 1e-9);
        assertArrayEquals(outRow[1], outCol[1], 1e-9);
    }

    @Test
    void getOutOfBoundsThrows() {
        SharedMatrix m = new SharedMatrix(new double[][]{
                {1, 2},
                {3, 4}
        });

        assertNotNull(m.get(0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(-1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(2));
    }

    @Test
    void lengthDependsOnOrientation() {
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
