package memory;

import memory.SharedVector;
import memory.VectorOrientation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SherdVectorTest {
    private SharedVector rowVec;
    private SharedVector colVec;
    private final double[] data = {1.0, 2.0, 3.0};

    @BeforeEach
    void setUp() {
        rowVec = new SharedVector(data.clone(), VectorOrientation.ROW_MAJOR);
        colVec = new SharedVector(data.clone(), VectorOrientation.COLUMN_MAJOR);
    }

    @Test
    void testBasicGetters() {
        assertEquals(3, rowVec.length(), "Length should match input array size.");
        assertEquals(VectorOrientation.ROW_MAJOR, rowVec.getOrientation());
        assertEquals(1.0, rowVec.get(0));
    }

    @Test
    void testTranspose() {
        rowVec.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, rowVec.getOrientation(),
                "Transpose should flip orientation from ROW to COLUMN.");

        rowVec.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, rowVec.getOrientation(),
                "Transpose should flip orientation back to ROW.");
    }

    @Test
    void testNegate() {
        rowVec.negate();
        assertEquals(-1.0, rowVec.get(0));
        assertEquals(-2.0, rowVec.get(1));
        assertEquals(-3.0, rowVec.get(2));
    }

    @Test
    void testAddition() {
        SharedVector other = new SharedVector(new double[]{10.0, 20.0, 30.0}, VectorOrientation.ROW_MAJOR);
        rowVec.add(other);

        assertEquals(11.0, rowVec.get(0), "1.0 + 10.0 = 11.0");
        assertEquals(22.0, rowVec.get(1), "2.0 + 20.0 = 22.0");
        assertEquals(33.0, rowVec.get(2), "3.0 + 30.0 = 33.0");
    }

    @Test
    void testAddSelf() {
        // Specifically tests the 'this == other' logic in the implementation
        rowVec.add(rowVec);
        assertEquals(2.0, rowVec.get(0));
        assertEquals(4.0, rowVec.get(1));
        assertEquals(6.0, rowVec.get(2));
    }

    @Test
    void testDotProduct() {
        SharedVector other = new SharedVector(new double[]{4.0, 5.0, 6.0}, VectorOrientation.COLUMN_MAJOR);
        double result = rowVec.dot(other);

        // (1*4) + (2*5) + (3*6) = 4 + 10 + 18 = 32
        assertEquals(32.0, result, "Dot product of [1,2,3] and [4,5,6] should be 32.");
    }

    @Test
    void testDimensionMismatch() {
        SharedVector shortVec = new SharedVector(new double[]{1.0}, VectorOrientation.ROW_MAJOR);

        try {
            rowVec.add(shortVec);
        } catch (Exception e) {
            // Success if caught properly
        }
    }
}