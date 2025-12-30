package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors
    private volatile int rows;
    private volatile int cols;
    private volatile VectorOrientation orientation;

    public SharedMatrix() {
        this.vectors = new SharedVector[0];
        this.rows = 0;
        this.cols = 0;
        this.orientation = VectorOrientation.ROW_MAJOR;
    }

    public SharedMatrix(double[][] matrix) {
        if (matrix == null){
            throw new IllegalArgumentException("matrix cannot be null");
        }
        int row = matrix.length;
        int cole;
        if (row == 0) {
            cole = 0;
        } else {
            cole = matrix[0].length;
        }
        // check
        for (int i = 0; i <= row -1; i++) {
            if (matrix[i] == null) {
                throw new IllegalArgumentException("row " + i + " is null");
            }
            if (matrix[i].length != cole) {
                throw new IllegalArgumentException("matrix is not rectangular");
            }
        }

        this.rows = row;
        this.cols = cole;
        this.orientation = VectorOrientation.ROW_MAJOR;
        this.vectors = new SharedVector[row];
        // new copy becose we don't want two object points to the same memory
        for (int i = 0; i <= row -1; i++) {

            double[] rowCopy = new double[cole];
            System.arraycopy(matrix[i], 0, rowCopy, 0, cole);

            this.vectors[i] = new SharedVector(rowCopy, VectorOrientation.ROW_MAJOR);
        }
    }


    public void loadRowMajor(double[][] matrix) {
        if (matrix == null){
            throw new IllegalArgumentException("matrix cannot be null");
        }
        int row = matrix.length;
        int cole;
        if (row == 0) {
            cole = 0;
        }
        else {
            cole = matrix[0].length;
        }
        // check
        for (int i = 0; i <= row -1; i++) {
            if (matrix[i] == null) {
                throw new IllegalArgumentException("row " + i + " is null");
            }
            if (matrix[i].length != cole) {
                throw new IllegalArgumentException("matrix is not rectangular");
            }
        }
        SharedVector[] newVectors = new SharedVector[row];
        for (int i = 0; i <= row -1; i++) {

            double[] rowCopy = new double[cole];
            System.arraycopy(matrix[i], 0, rowCopy, 0, cole);

            newVectors[i] = new SharedVector(rowCopy, VectorOrientation.ROW_MAJOR);
        }
        this.vectors = newVectors;
        this.rows = row;
        this.cols = cole;
        this.orientation = VectorOrientation.ROW_MAJOR;



    }

    public void loadColumnMajor(double[][] matrix) {
        if (matrix == null){
            throw new IllegalArgumentException("matrix cannot be null");
        }
        int row = matrix.length;
        int cole;
        if (row == 0) {
            cole = 0;
        }
        else {
            cole = matrix[0].length;
        }
        // check
        for (int i = 0; i <= row -1; i++) {
            if (matrix[i] == null) {
                throw new IllegalArgumentException("row " + i + " is null");
            }
            if (matrix[i].length != cole) {
                throw new IllegalArgumentException("matrix is not rectangular");
            }
        }
        SharedVector[] newVectors = new SharedVector[cole];
        for (int j = 0; j <= cole -1; j++) {
            double[] colCopy = new double[row];
            for (int i = 0; i <= row -1; i++) {
                colCopy[i] = matrix[i][j];
            }

            newVectors[j] = new SharedVector(colCopy, VectorOrientation.COLUMN_MAJOR);
        }
        this.vectors = newVectors;
        this.rows = row;
        this.cols = cole;
        this.orientation = VectorOrientation.COLUMN_MAJOR;

    }

    public double[][] readRowMajor() {
        double[][] result = new double[this.rows][this.cols];

        // if SharedVector is a row
        if (this.orientation == VectorOrientation.ROW_MAJOR) {
            for (int i = 0; i <= rows-1; i++) {
                SharedVector rowVec = vectors[i];
                for (int j = 0; j <= cols-1; j++) {
                    result[i][j] = rowVec.get(j);
                }
            }
            }
        // if SharedVector is a column
        else {
                for (int j = 0; j <= cols-1; j++) {
                    SharedVector colVec = vectors[j];
                    for (int i = 0; i <= rows-1; i++) {
                        result[i][j] = colVec.get(i);
                    }
                }
            }

        return result;


    }

    public SharedVector get(int index) {
        return vectors[index];
    }

    public int length() {
        return vectors.length;
    }

    public VectorOrientation getOrientation() {
        return orientation;
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        for (SharedVector v : vecs) {
            v.readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        //Release locks in reverse order to match the acquisition order
        for (int i = vecs.length - 1; i >= 0; i--) {
            vecs[i].readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        for (SharedVector v : vecs) {
            v.writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        //Release locks in reverse order to match the acquisition order
        for (int i = vecs.length - 1; i >= 0; i--) {
            vecs[i].writeUnlock();
        }
    }
}
