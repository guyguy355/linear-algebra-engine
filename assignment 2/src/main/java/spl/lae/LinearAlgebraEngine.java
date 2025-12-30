package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        if (numThreads <= 0) {
            throw new IllegalArgumentException("numThreads must be positive");
        }
        this.executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        if (computationRoot == null) {
            throw new IllegalArgumentException("computationRoot cannot be null");
        }

// apply associative nesting if required by the spec
        computationRoot.associativeNesting();

// keep resolving until the root becomes a matrix node
        while (computationRoot.getNodeType() != ComputationNodeType.MATRIX) {
            ComputationNode nextNode = computationRoot.findResolvable();
            if (nextNode == null) {
                throw new IllegalStateException("No resolvable node found");
            }

            loadAndCompute(nextNode);

        }

        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }

        ComputationNodeType op = node.getNodeType();

// load matrices depending on operation
        if (op == ComputationNodeType.ADD || op == ComputationNodeType.MULTIPLY) {
            leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());
            rightMatrix.loadRowMajor(node.getChildren().get(1).getMatrix());

        } else if (op == ComputationNodeType.NEGATE) {
            leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());

        } else {
            // transpose: we want faster access by columns
            leftMatrix.loadColumnMajor(node.getChildren().get(0).getMatrix());
        }

// run tasks + basic validations
        if (op == ComputationNodeType.ADD) {
            if (leftMatrix.length() != rightMatrix.length()) {
                throw new IllegalArgumentException("The matrices have different length");
            }
            executor.submitAll(createAddTasks());
        }

        if (op == ComputationNodeType.MULTIPLY) {
            if (leftMatrix.get(0).length() != rightMatrix.length()) {
                throw new IllegalArgumentException(
                        "The left matrix number of columns is not equal to the right matrix number of rows"
                );
            }
            executor.submitAll(createMultiplyTasks());
        }

        if (op == ComputationNodeType.NEGATE) {
            executor.submitAll(createNegateTasks());
        }

        if (op == ComputationNodeType.TRANSPOSE) {
            executor.submitAll(createTransposeTasks());
        }
        double[][] result = leftMatrix.readRowMajor();
        node.resolve(result);

    }

    public List<Runnable> createAddTasks() {
        List<Runnable> tasks = new java.util.ArrayList<>();
        int rows = rightMatrix.length();

        for (int i = 0; i < rows; i++) {
            final int r = i;

            tasks.add(() -> {
                try {
                    leftMatrix.get(r).add(rightMatrix.get(r));
                } catch (Exception ex) {
                    throw new IllegalArgumentException(ex);
                }
            });
        }

        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        List<Runnable> tasks = new java.util.ArrayList<>();
        int rows = leftMatrix.length();

        for (int i = 0; i < rows; i++) {
            final int r = i;

            tasks.add(() -> {
                try {
                    leftMatrix.get(r).vecMatMul(rightMatrix);
                } catch (Exception ex) {
                    throw new IllegalArgumentException(ex);
                }
            });
        }

        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        List<Runnable> tasks = new java.util.ArrayList<>();
        int rows = leftMatrix.length();

        for (int i = 0; i < rows; i++) {
            final int r = i;

            tasks.add(() -> {
                try {
                    leftMatrix.get(r).negate();
                } catch (Exception ex) {
                    throw new IllegalArgumentException(ex);
                }
            });
        }

        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        List<Runnable> tasks = new java.util.ArrayList<>();
        int rows = leftMatrix.length();

        for (int i = 0; i < rows; i++) {
            final int r = i;

            tasks.add(() -> {
                try {
                    leftMatrix.get(r).transpose();
                } catch (Exception ex) {
                    throw new IllegalArgumentException(ex);
                }
            });
        }

        return tasks;
    }

    public String getWorkerReport() {
        return executor.getWorkerReport();
    }
}
