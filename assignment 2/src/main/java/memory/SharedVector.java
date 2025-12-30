package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        this.vector= vector;
        this.orientation= orientation;
    }

    public double get(int index) {
        if (index < 0 || index >= vector.length) {
            throw new IndexOutOfBoundsException();
        }

        lock.readLock().lock();
        double result = vector[index];
        lock.readLock().unlock();
        return result;
    }

    public int length() {
        return vector.length;
    }

    public VectorOrientation getOrientation() {
        return orientation;
    }

    public void writeLock() {
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        lock.writeLock().unlock();
    }

    public void readLock() {
        lock.readLock().lock();
    }

    public void readUnlock() {
        lock.readLock().unlock();
    }

    public void transpose() {
        lock.writeLock().lock();


        if (orientation == VectorOrientation.ROW_MAJOR) {
            orientation = VectorOrientation.COLUMN_MAJOR;
        } else {
            orientation = VectorOrientation.ROW_MAJOR;
        }

        lock.writeLock().unlock();
    }

    public void add(SharedVector other) {
        if (vector.length != other.length()) {
            throw new IllegalArgumentException();
        }

        lock.writeLock().lock();

        for (int i = 0; i < vector.length; i++) {
            vector[i] += other.vector[i];
        }

        lock.writeLock().unlock();
    }

    public void negate() {
        lock.writeLock().lock(); //

        for (int i = 0; i < vector.length; i++) {
            vector[i] = -vector[i];
        }

        lock.writeLock().unlock(); //
    }

    public double dot(SharedVector other) {
        if (vector.length != other.length()) {
            throw new IllegalArgumentException();
        }

        lock.readLock().lock();

        double sum = 0;
        for (int i = 0; i < vector.length; i++) {
            sum += vector[i] * other.vector[i];
        }

        lock.readLock().unlock();

        return sum;
    }

    public void vecMatMul(SharedMatrix matrix) {
        if (matrix == null || matrix.length() == 0) {
            throw new IllegalArgumentException();
        }

        //
        if (this.vector.length != matrix.get(0).length()) {
            throw new IllegalArgumentException();
        }

        lock.writeLock().lock();

        int newSize = matrix.length();
        double[] newVectorData = new double[newSize];

        for (int i = 0; i < newSize; i++) {
            SharedVector column = matrix.get(i);

            double sum = 0;
            for (int j = 0; j < vector.length; j++) {
                sum += vector[j] * column.vector[i];
            }

            newVectorData[i] = sum;
        }

        this.vector = newVectorData;

        lock.writeLock().unlock();
    }
}
