package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        if (matrix != null) {
            loadRowMajor(matrix);
        }
    }

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
        SharedVector[] newVectors = new SharedVector[matrix.length];
        for (int i = 0; i < newVectors.length; i++) {
            newVectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
        this.vectors = newVectors;

    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
        SharedVector[] newVectors = new SharedVector[matrix[0].length];

        for (int i = 0; i < newVectors.length; i++) {
            double[] temp = new double[matrix.length];
            for (int j = 0; j < temp.length; j++) {
                temp[j] = matrix[j][i];
            }
            newVectors[i] = new SharedVector(temp, VectorOrientation.COLUMN_MAJOR);
        }
        this.vectors = newVectors;
    }

    private static boolean isValidMatrix(double[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0] == null) {
            return false;
        }

        int expectedColumns = matrix[0].length;
        if (expectedColumns == 0) {
            return false;
        }

        for (int i = 1; i < matrix.length; i++) {
            if (matrix[i] == null || matrix[i].length != expectedColumns) {
                return false;
            }
        }

        return true;
    }

    public double[][] readRowMajor() {
        this.acquireAllVectorReadLocks(this.vectors);
        try {
            if (vectors.length == 0) return new double[0][0];

            VectorOrientation matrixOrientation = getOrientation();
            if (matrixOrientation == VectorOrientation.ROW_MAJOR) {
                int colLength = vectors[0].length();
                int rowLength = this.length();
                double[][] result = new double[rowLength][colLength];
                for (int i = 0; i < rowLength; i++) {
                    for (int j = 0; j < colLength; j++) {
                        result[i][j] = vectors[i].get(j);
                    }
                }
                return result;
            } else {
                int rowLength = vectors[0].length();
                int colLength = this.length();
                double[][] result = new double[rowLength][colLength];
                for (int j = 0; j < colLength; j++) {
                    for (int i = 0; i < rowLength; i++) {
                        result[i][j] = vectors[j].get(i);
                    }
                }
                return result;
            }
        } finally {
            this.releaseAllVectorReadLocks(this.vectors);
        }
    }

    public SharedVector get(int index) {
        if (0 > index || index >= vectors.length) {
            throw new RuntimeException("Access to index " + index + " in matrix is not valid.");
        }
        return this.vectors[index];
    }

    public int length() {
        return vectors.length;
    }

    public VectorOrientation getOrientation() {
        return vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        for (int i = 0; i < vecs.length; i++) {
            vecs[i].readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        for (int i = 0; i < vecs.length; i++) {
            vecs[i].readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        for (int i = 0; i < vecs.length; i++) {
            vecs[i].writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        for (int i = 0; i < vecs.length; i++) {
            vecs[i].writeUnlock();
        }
    }
}
