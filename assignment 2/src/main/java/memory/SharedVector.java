package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        if (vector == null) {
            throw new IllegalArgumentException("Illegal operation: null vector");
        }
        if (orientation == null) {
            throw new IllegalArgumentException("Illegal operation: null orientation");
        }

        // Deep copy (like code A)
        this.vector = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            this.vector[i] = vector[i];
        }
        this.orientation = orientation;
    }

    public double get(int index) {
        this.readLock();
        if (index < 0 || index >= this.length()) {
            this.readUnlock();
            throw new IllegalArgumentException("Illegal index");
        }
        double result = this.vector[index];
        this.readUnlock();
        return result;
    }

    public int length() {
        this.readLock();
        int len = this.vector.length;
        this.readUnlock();
        return len;
    }

    public VectorOrientation getOrientation() {
        this.readLock();
        VectorOrientation o = this.orientation;
        this.readUnlock();
        return o;
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
        this.writeLock();
        if (this.orientation == VectorOrientation.ROW_MAJOR) {
            this.orientation = VectorOrientation.COLUMN_MAJOR;
        } else {
            this.orientation = VectorOrientation.ROW_MAJOR;
        }
        this.writeUnlock();
    }

    public void add(SharedVector other) {
        if (other == null) {
            throw new IllegalArgumentException("Illegal operation: null vector");
        }

        this.writeLock();
        other.readLock();

        if (this.length() != other.length()) {
            this.writeUnlock();
            other.readUnlock();
            throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
        }
        if (this.getOrientation() != other.getOrientation()) {
            this.writeUnlock();
            other.readUnlock();
            throw new IllegalArgumentException("Illegal operation: orientations mismatch");
        }

        for (int i = 0; i < this.length(); i++) {
            this.vector[i] = this.vector[i] + other.vector[i];
        }

        other.readUnlock();
        this.writeUnlock();
    }

    public void negate() {
        this.writeLock();
        for (int i = 0; i < this.length(); i++) {
            this.vector[i] = this.vector[i] * (-1);
        }
        this.writeUnlock();
    }

    public double dot(SharedVector other) {
        if (other == null) {
            throw new IllegalArgumentException("Illegal operation: null vector");
        }

        this.readLock();
        other.readLock();

        if (this.getOrientation() == other.getOrientation()) {
            this.readUnlock();
            other.readUnlock();
            throw new IllegalArgumentException("Illegal operation: orientations mismatch");
        }
        if (this.length() != other.length()) {
            this.readUnlock();
            other.readUnlock();
            throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
        }

        double result = 0;
        for (int i = 0; i < this.length(); i++) {
            result += this.get(i) * other.get(i);
        }

        other.readUnlock();
        this.readUnlock();
        return result;
    }

    public void vecMatMul(SharedMatrix matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("Illegal operation: null matrix");
        }
        if (matrix.length() == 0) {
            throw new IllegalArgumentException("Illegal operation: empty matrix");
        }
        if (matrix.get(0) == null) {
            throw new IllegalArgumentException("Illegal operation: null column/row");
        }

        double[] temp;
        double sum;

        this.writeLock();

        // lock all vectors in the matrix (and guard against null entries)
        for (int i = 0; i < matrix.length(); i++) {
            SharedVector v = matrix.get(i);
            if (v == null) {
                // unlock what we already locked
                for (int k = 0; k < i; k++) {
                    matrix.get(k).readUnlock();
                }
                this.writeUnlock();
                throw new IllegalArgumentException("Illegal operation: null column/row");
            }
            v.readLock();
        }

        if (matrix.getOrientation() == VectorOrientation.COLUMN_MAJOR) {
            if (matrix.get(0).length() != this.length()) {
                for (int i = 0; i < matrix.length(); i++) {
                    matrix.get(i).readUnlock();
                }
                this.writeUnlock();
                throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
            }

            temp = new double[matrix.length()];
            for (int j = 0; j < temp.length; j++) {
                sum = 0;
                for (int i = 0; i < this.length(); i++) {
                    sum += this.get(i) * matrix.get(j).get(i);
                }
                temp[j] = sum;
            }

        } else {

            if (matrix.length() != this.length()) {
                for (int i = 0; i < matrix.length(); i++) {
                    matrix.get(i).readUnlock();
                }
                this.writeUnlock();
                throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
            }

            temp = new double[matrix.get(0).length()];
            for (int j = 0; j < temp.length; j++) {
                sum = 0;
                for (int i = 0; i < matrix.length(); i++) {
                    sum = sum + (matrix.get(i).get(j) * this.get(i));
                }
                temp[j] = sum;
            }
        }

        for (int i = 0; i < matrix.length(); i++) {
            matrix.get(i).readUnlock();
        }
        this.vector = temp;
        this.writeUnlock();
    }
}